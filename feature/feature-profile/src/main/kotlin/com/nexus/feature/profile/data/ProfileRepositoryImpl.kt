package com.nexus.feature.profile.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import com.nexus.core.common.dispatchers.IoDispatcher
import com.nexus.core.firebase.post.PostActions
import com.nexus.feature.profile.domain.model.MyPosts
import com.nexus.feature.profile.domain.model.ProfilePost
import com.nexus.feature.profile.domain.model.ProfileUser
import com.nexus.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val postActions: PostActions,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProfileRepository {

    private val posts get() = firestore.collection(POSTS)

    override suspend fun currentUser(): ProfileUser? = withContext(ioDispatcher) {
        val user = auth.currentUser ?: return@withContext null
        // The users document holds the name the app shows everywhere; the Auth profile is
        // only a fallback while it has not been written yet
        val doc = runCatching { firestore.collection(USERS).document(user.uid).get().await() }.getOrNull()
        ProfileUser(
            uid = user.uid,
            name = doc?.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore('@')
                ?: "Usuario de Nexus",
            photoUrl = doc?.getString("photoUrl") ?: user.photoUrl?.toString(),
            email = user.email,
            memberSince = user.metadata?.creationTimestamp,
        )
    }

    override suspend fun myPosts(): MyPosts = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext MyPosts(emptyList(), hasMore = false)
        // Equality only, sorted here: authorId + createdAt would need a composite index that
        // a fresh project may not have deployed yet, and a profile holds few posts
        val docs = posts.whereEqualTo("authorId", uid)
            .limit(MY_POSTS_LIMIT)
            .get().await()
            .documents
        MyPosts(
            posts = docs.mapNotNull { it.toProfilePost() }.sortedByDescending { it.createdAt },
            hasMore = docs.size >= MY_POSTS_LIMIT,
        )
    }

    override suspend fun postsByIds(ids: List<String>): List<ProfilePost> = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext emptyList()
        // One get per post instead of a whereIn query: the rules accept a query only if it
        // cannot return circle posts, and a saved post may be from a circle the user is in.
        // A post that is gone or no longer readable (the user left its circle) is missing;
        // any other failure (offline) propagates, so the saved list is not trimmed by mistake
        val found = coroutineScope {
            ids.distinct().map { id -> async { readPost(id) } }.awaitAll()
        }.filterNotNull().associateBy { it.id }
        ids.mapNotNull { found[it] }
    }

    private suspend fun readPost(id: String): ProfilePost? = try {
        posts.document(id).get().await().toProfilePost()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) null else throw e
    }

    override fun observeSavedIds(): Flow<List<String>> = postActions.observeSavedIdsOrdered()

    override suspend fun setSaved(postId: String, saved: Boolean) = postActions.setSaved(postId, saved)

    private fun DocumentSnapshot.toProfilePost(): ProfilePost? {
        if (!exists()) return null
        val images = (get("imageUrls") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val reactions = (get("reactions") as? Map<*, *>)?.values
            ?.sumOf { (it as? Number)?.toInt()?.coerceAtLeast(0) ?: 0 } ?: 0
        return ProfilePost(
            id = id,
            authorId = getString("authorId").orEmpty(),
            authorName = getString("authorName")?.takeIf { it.isNotBlank() } ?: "Anónimo",
            authorPhotoUrl = getString("authorPhotoUrl"),
            content = getString("content").orEmpty(),
            // A GIF stands in for the photo when the post has none
            imageUrl = images.firstOrNull() ?: getString("gifUrl"),
            imageCount = images.size,
            hasAudio = getString("audioUrl") != null,
            fileName = getString("fileName"),
            mood = getString("mood"),
            ecoCount = getLong("ecoCount")?.toInt()?.coerceAtLeast(0) ?: 0,
            reactionCount = reactions,
            createdAt = getLong("createdAt") ?: 0L,
        )
    }

    private companion object {
        const val POSTS = "posts"
        const val USERS = "users"
        const val MY_POSTS_LIMIT = 100L
    }
}
