package com.nexus.feature.circles.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.nexus.core.common.dispatchers.IoDispatcher
import com.nexus.core.firebase.circle.CircleDirectory
import com.nexus.core.firebase.circle.CircleDirectory.Companion.toCircle
import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.core.firebase.circle.UserBrief
import com.nexus.feature.circles.domain.model.CircleMember
import com.nexus.feature.circles.domain.model.CirclePost
import com.nexus.feature.circles.domain.model.InviteCode
import com.nexus.feature.circles.domain.model.InvitePreview
import com.nexus.feature.circles.domain.repository.CirclesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject

/**
 * Circles in Firestore. Every change that touches more than one document goes in one batch,
 * because the security rules check them together: a circle and its invite code, a new member
 * and the proof of the code they used, a new code and its lookup document.
 */
class CirclesRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val directory: CircleDirectory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CirclesRepository {

    private val circles get() = firestore.collection(CircleDirectory.CIRCLES)
    private val invites get() = firestore.collection(INVITES)
    private val random = SecureRandom()

    override val currentUid: String? get() = directory.currentUid

    override fun observeMyCircles(): Flow<List<CircleSummary>> = directory.observeMyCircles()

    override fun observeCircle(circleId: String): Flow<CircleSummary?> = callbackFlow {
        val listener = circles.document(circleId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot.toCircle())
        }
        awaitClose { listener.remove() }
    }

    override suspend fun users(uids: Collection<String>): Map<String, UserBrief> = directory.users(uids)

    override suspend fun createCircle(name: String, emoji: String, colorIndex: Int): String =
        withContext(ioDispatcher) {
            val uid = requireUid()
            val ref = circles.document()
            val code = freeCode()
            val batch = firestore.batch()
            batch.set(
                ref,
                mapOf(
                    "name" to name,
                    "emoji" to emoji,
                    "colorIndex" to colorIndex,
                    "ownerId" to uid,
                    "memberIds" to listOf(uid),
                    "inviteCode" to code,
                    "createdAt" to System.currentTimeMillis(),
                ),
            )
            batch.set(invites.document(code), inviteData(ref.id, name, emoji, colorIndex))
            batch.commit().await()
            ref.id
        }

    override suspend fun findInvite(code: String): InvitePreview? = withContext(ioDispatcher) {
        // From the server: a cached copy could point at a code the owner already changed
        val doc = invites.document(code).get(Source.SERVER).await()
        if (!doc.exists()) return@withContext null
        InvitePreview(
            code = code,
            circleId = doc.getString("circleId") ?: return@withContext null,
            name = doc.getString("name").orEmpty(),
            emoji = doc.getString("emoji")?.takeIf { it.isNotBlank() } ?: "⭕",
            colorIndex = doc.getLong("colorIndex")?.toInt()?.coerceIn(0, 7) ?: 0,
        )
    }

    override suspend fun join(invite: InvitePreview) = withContext(ioDispatcher) {
        val uid = requireUid()
        val circle = circles.document(invite.circleId)
        val batch = firestore.batch()
        // The rules accept the new member only together with this proof of the current code
        batch.set(
            circle.collection(MEMBERS).document(uid),
            mapOf("code" to invite.code, "joinedAt" to System.currentTimeMillis()),
        )
        batch.update(circle, CircleDirectory.MEMBER_IDS, FieldValue.arrayUnion(uid))
        batch.commit().await()
        Unit
    }

    override suspend fun leave(circleId: String) = withContext(ioDispatcher) {
        val uid = requireUid()
        val circle = circles.document(circleId)
        val batch = firestore.batch()
        batch.update(circle, CircleDirectory.MEMBER_IDS, FieldValue.arrayRemove(uid))
        batch.delete(circle.collection(MEMBERS).document(uid))
        batch.commit().await()
        Unit
    }

    override suspend fun removeMember(circle: CircleSummary, uid: String) = withContext(ioDispatcher) {
        val ref = circles.document(circle.id)
        val code = freeCode()
        val batch = firestore.batch()
        batch.update(
            ref,
            mapOf(
                CircleDirectory.MEMBER_IDS to FieldValue.arrayRemove(uid),
                "inviteCode" to code,
            ),
        )
        batch.set(invites.document(code), inviteData(circle))
        batch.delete(invites.document(circle.inviteCode))
        batch.delete(ref.collection(MEMBERS).document(uid))
        batch.commit().await()
        Unit
    }

    override suspend fun regenerateCode(circle: CircleSummary): String = withContext(ioDispatcher) {
        val code = freeCode()
        val batch = firestore.batch()
        batch.update(circles.document(circle.id), "inviteCode", code)
        batch.set(invites.document(code), inviteData(circle))
        batch.delete(invites.document(circle.inviteCode))
        batch.commit().await()
        code
    }

    override suspend fun deleteCircle(circle: CircleSummary) = withContext(ioDispatcher) {
        val ref = circles.document(circle.id)
        val batch = firestore.batch()
        batch.delete(invites.document(circle.inviteCode))
        // Only current members have a proof document; those who left already deleted theirs
        circle.memberIds.forEach { batch.delete(ref.collection(MEMBERS).document(it)) }
        batch.delete(ref)
        batch.commit().await()
        Unit
    }

    override suspend fun circlePosts(circleId: String): List<CirclePost> = withContext(ioDispatcher) {
        firestore.collection(POSTS)
            .whereEqualTo("circleId", circleId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(POSTS_LIMIT)
            .get().await()
            .documents
            .mapNotNull { it.toCirclePost() }
    }

    override suspend fun members(circle: CircleSummary, refresh: Boolean): List<CircleMember> =
        withContext(ioDispatcher) {
            val me = currentUid
            val users = directory.users(circle.memberIds, refresh)
            circle.memberIds.map { uid ->
                val user = users[uid]
                CircleMember(
                    uid = uid,
                    name = user?.name ?: CircleDirectory.UNKNOWN_NAME,
                    photoUrl = user?.photoUrl,
                    isOwner = uid == circle.ownerId,
                    isMe = uid == me,
                )
            }.sortedWith(compareByDescending<CircleMember> { it.isOwner }.thenBy { it.name.lowercase() })
        }

    private fun requireUid(): String = currentUid ?: throw IllegalStateException("Not signed in")

    /** A code no circle uses yet. The rules would reject a taken one anyway. */
    private suspend fun freeCode(): String {
        repeat(CODE_ATTEMPTS) {
            val code = buildString {
                repeat(InviteCode.LENGTH) { append(InviteCode.ALPHABET[random.nextInt(InviteCode.ALPHABET.length)]) }
            }
            if (!invites.document(code).get(Source.SERVER).await().exists()) return code
        }
        throw IllegalStateException("No free invite code after $CODE_ATTEMPTS attempts")
    }

    private fun inviteData(circle: CircleSummary) =
        inviteData(circle.id, circle.name, circle.emoji, circle.colorIndex)

    private fun inviteData(circleId: String, name: String, emoji: String, colorIndex: Int) = mapOf(
        "circleId" to circleId,
        "name" to name,
        "emoji" to emoji,
        "colorIndex" to colorIndex,
    )

    private fun DocumentSnapshot.toCirclePost(): CirclePost? {
        if (!exists()) return null
        val images = (get("imageUrls") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val reactions = (get("reactions") as? Map<*, *>)?.values
            ?.sumOf { (it as? Number)?.toInt()?.coerceAtLeast(0) ?: 0 } ?: 0
        return CirclePost(
            id = id,
            authorId = getString("authorId").orEmpty(),
            authorName = getString("authorName")?.takeIf { it.isNotBlank() } ?: CircleDirectory.UNKNOWN_NAME,
            authorPhotoUrl = getString("authorPhotoUrl"),
            content = getString("content").orEmpty(),
            // A GIF stands in for the photo when the post has none
            imageUrl = images.firstOrNull() ?: getString("gifUrl"),
            imageCount = images.size,
            hasAudio = getString("audioUrl") != null,
            mood = getString("mood"),
            ecoCount = getLong("ecoCount")?.toInt()?.coerceAtLeast(0) ?: 0,
            reactionCount = reactions,
            createdAt = getLong("createdAt") ?: 0L,
        )
    }

    private companion object {
        const val INVITES = "circleInvites"
        const val MEMBERS = "members"
        const val POSTS = "posts"
        const val POSTS_LIMIT = 50L
        const val CODE_ATTEMPTS = 5
    }
}
