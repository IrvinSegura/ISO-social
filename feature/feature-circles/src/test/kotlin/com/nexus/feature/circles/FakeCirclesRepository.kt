package com.nexus.feature.circles

import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.core.firebase.circle.UserBrief
import com.nexus.feature.circles.domain.model.CircleMember
import com.nexus.feature.circles.domain.model.CirclePost
import com.nexus.feature.circles.domain.model.InvitePreview
import com.nexus.feature.circles.domain.repository.CirclesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory circles: the flows emit whatever the test puts in [circles]. */
class FakeCirclesRepository(override val currentUid: String? = "me") : CirclesRepository {
    val circles = MutableStateFlow<Map<String, CircleSummary>>(emptyMap())
    val invites = mutableMapOf<String, InvitePreview>()
    val posts = mutableMapOf<String, List<CirclePost>>()
    var failWith: Exception? = null
    val calls = mutableListOf<String>()
    private var nextId = 1

    /** Fails the listener of this circle, as Firestore does once the user lost access. */
    var circleError: Exception? = null

    override fun observeMyCircles(): Flow<List<CircleSummary>> =
        circles.map { all -> all.values.filter { currentUid in it.memberIds }.sortedBy { it.createdAt } }

    override fun observeCircle(circleId: String): Flow<CircleSummary?> =
        circles.map { all ->
            circleError?.let { throw it }
            all[circleId]
        }

    override suspend fun users(uids: Collection<String>) = uids.associateWith { UserBrief(it, it.uppercase(), null) }

    override suspend fun createCircle(name: String, emoji: String, colorIndex: Int): String {
        failWith?.let { throw it }
        val id = "c${nextId++}"
        circles.value += id to circle(id, name, ownerId = currentUid!!, members = listOf(currentUid))
        calls += "create:$name"
        return id
    }

    override suspend fun findInvite(code: String): InvitePreview? {
        failWith?.let { throw it }
        return invites[code]
    }

    override suspend fun join(invite: InvitePreview) {
        failWith?.let { throw it }
        val circle = circles.value.getValue(invite.circleId)
        circles.value += circle.id to circle.copy(memberIds = circle.memberIds + currentUid!!)
        calls += "join:${invite.circleId}"
    }

    override suspend fun leave(circleId: String) {
        failWith?.let { throw it }
        calls += "leave:$circleId"
        circleError = IllegalStateException("PERMISSION_DENIED")
        val circle = circles.value.getValue(circleId)
        circles.value += circleId to circle.copy(memberIds = circle.memberIds - currentUid!!)
    }

    override suspend fun removeMember(circle: CircleSummary, uid: String) {
        failWith?.let { throw it }
        calls += "remove:$uid"
        circles.value += circle.id to circle.copy(memberIds = circle.memberIds - uid, inviteCode = "NEWCODE2")
    }

    override suspend fun regenerateCode(circle: CircleSummary): String {
        failWith?.let { throw it }
        circles.value += circle.id to circle.copy(inviteCode = "ZZZZ2222")
        return "ZZZZ2222"
    }

    override suspend fun deleteCircle(circle: CircleSummary) {
        failWith?.let { throw it }
        calls += "delete:${circle.id}"
        circles.value -= circle.id
    }

    override suspend fun circlePosts(circleId: String): List<CirclePost> {
        failWith?.let { throw it }
        calls += "posts:$circleId"
        return posts[circleId].orEmpty()
    }

    override suspend fun members(circle: CircleSummary, refresh: Boolean): List<CircleMember> =
        circle.memberIds.map { CircleMember(it, it.uppercase(), null, isOwner = it == circle.ownerId, isMe = it == currentUid) }

    companion object {
        fun circle(id: String, name: String, ownerId: String, members: List<String>, code: String = "ABCD2345") =
            CircleSummary(id, name, "🏠", 0, ownerId, members, code, createdAt = id.hashCode().toLong())

        fun post(id: String) = CirclePost(
            id = id, authorId = "me", authorName = "Yo", authorPhotoUrl = null, content = "Hola $id",
            imageUrl = null, imageCount = 0, hasAudio = false, mood = null, ecoCount = 0, reactionCount = 0, createdAt = 0,
        )
    }
}
