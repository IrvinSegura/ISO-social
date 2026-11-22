package com.nexus.feature.profile.domain.repository

import com.nexus.feature.profile.domain.model.MyPosts
import com.nexus.feature.profile.domain.model.ProfilePost
import com.nexus.feature.profile.domain.model.ProfileUser
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /** Profile of the signed-in user, or null when nobody is signed in. */
    suspend fun currentUser(): ProfileUser?

    suspend fun myPosts(): MyPosts

    /** The posts with these ids, in the same order; ids of deleted posts are left out. */
    suspend fun postsByIds(ids: List<String>): List<ProfilePost>

    /** Saved post ids, newest first, updated live. */
    fun observeSavedIds(): Flow<List<String>>

    suspend fun setSaved(postId: String, saved: Boolean)
}
