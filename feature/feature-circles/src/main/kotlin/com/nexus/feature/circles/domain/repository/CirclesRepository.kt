package com.nexus.feature.circles.domain.repository

import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.core.firebase.circle.UserBrief
import com.nexus.feature.circles.domain.model.CircleMember
import com.nexus.feature.circles.domain.model.CirclePost
import com.nexus.feature.circles.domain.model.InvitePreview
import kotlinx.coroutines.flow.Flow

interface CirclesRepository {
    val currentUid: String?

    /** The user's circles, kept up to date while collected. */
    fun observeMyCircles(): Flow<List<CircleSummary>>

    /**
     * One circle, kept up to date. Emits null when it was deleted and fails when the user
     * can no longer read it (they left or were removed).
     */
    fun observeCircle(circleId: String): Flow<CircleSummary?>

    suspend fun users(uids: Collection<String>): Map<String, UserBrief>

    /** Creates the circle with the user as owner and only member; returns its id. */
    suspend fun createCircle(name: String, emoji: String, colorIndex: Int): String

    /** The circle an invite code opens, or null when no circle uses that code. */
    suspend fun findInvite(code: String): InvitePreview?

    suspend fun join(invite: InvitePreview)

    suspend fun leave(circleId: String)

    /** Owner only. Also changes the invite code, so the removed person cannot rejoin with it. */
    suspend fun removeMember(circle: CircleSummary, uid: String)

    /** Owner only; returns the new code. The old one stops working. */
    suspend fun regenerateCode(circle: CircleSummary): String

    /** Owner only. */
    suspend fun deleteCircle(circle: CircleSummary)

    /** Newest posts of the circle. */
    suspend fun circlePosts(circleId: String): List<CirclePost>

    /** Members, owner first, then by name. */
    suspend fun members(circle: CircleSummary, refresh: Boolean = false): List<CircleMember>
}
