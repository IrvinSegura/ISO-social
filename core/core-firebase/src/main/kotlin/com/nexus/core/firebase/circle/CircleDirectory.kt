package com.nexus.core.firebase.circle

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** A circle the current user belongs to, as stored in circles/{id}. */
data class CircleSummary(
    val id: String,
    val name: String,
    val emoji: String,
    val colorIndex: Int,
    val ownerId: String,
    val memberIds: List<String>,
    val inviteCode: String,
    val createdAt: Long,
)

/** Public name and photo of a user (users/{uid}). */
data class UserBrief(
    val uid: String,
    val name: String,
    val photoUrl: String?,
)

/**
 * Circles of the current user and the public profiles of people in them. Shared by the
 * circles tab, the circle picker when publishing and the "new message" list, so none of
 * those features depends on another.
 */
@Singleton
class CircleDirectory @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    // Names change rarely; this avoids reading the same profile for every screen
    private val userCache = ConcurrentHashMap<String, UserBrief>()

    val currentUid: String? get() = auth.currentUser?.uid

    /** The user's circles, oldest first, kept up to date while collected. */
    fun observeMyCircles(): Flow<List<CircleSummary>> {
        val uid = currentUid ?: return flowOf(emptyList())
        return callbackFlow {
            val listener = firestore.collection(CIRCLES)
                .whereArrayContains(MEMBER_IDS, uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        trySend(snapshot.documents.mapNotNull { it.toCircle() }.sortedBy { it.createdAt })
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    /** Profiles for the given uids; unknown or unreadable ones get a placeholder name. */
    suspend fun users(uids: Collection<String>, refresh: Boolean = false): Map<String, UserBrief> = coroutineScope {
        val missing = uids.distinct().filter { refresh || !userCache.containsKey(it) }
        missing.map { uid ->
            async {
                val doc = runCatching { firestore.collection(USERS).document(uid).get().await() }.getOrNull()
                val brief = UserBrief(
                    uid = uid,
                    name = doc?.getString("displayName")?.takeIf { it.isNotBlank() } ?: UNKNOWN_NAME,
                    photoUrl = doc?.getString("photoUrl"),
                )
                // A failed read is not cached, so the next screen tries again
                if (doc != null) userCache[uid] = brief
                brief
            }
        }.awaitAll()
        uids.associateWith { userCache[it] ?: UserBrief(it, UNKNOWN_NAME, null) }
    }

    companion object {
        const val CIRCLES = "circles"
        const val USERS = "users"
        const val MEMBER_IDS = "memberIds"
        const val MAX_MEMBERS = 30
        const val UNKNOWN_NAME = "Usuario de Nexus"

        fun DocumentSnapshot.toCircle(): CircleSummary? {
            if (!exists()) return null
            return CircleSummary(
                id = id,
                name = getString("name").orEmpty(),
                emoji = getString("emoji")?.takeIf { it.isNotBlank() } ?: "⭕",
                colorIndex = getLong("colorIndex")?.toInt()?.coerceIn(0, 7) ?: 0,
                ownerId = getString("ownerId").orEmpty(),
                memberIds = (get(MEMBER_IDS) as? List<*>)?.filterIsInstance<String>().orEmpty(),
                inviteCode = getString("inviteCode").orEmpty(),
                createdAt = getLong("createdAt") ?: 0L,
            )
        }
    }
}
