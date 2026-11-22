package com.nexus.core.firebase.post

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Why a post is reported; the name is what gets stored (see firestore.rules). */
enum class ReportReason(val label: String) {
    SPAM("Spam o publicidad"),
    HARASSMENT("Acoso o lenguaje de odio"),
    INAPPROPRIATE("Contenido inapropiado"),
    MISINFORMATION("Información falsa"),
    OTHER("Otro motivo"),
}

enum class ReportResult { SENT, ALREADY_REPORTED }

/**
 * Actions on a post shared by the feed, the post detail and the profile: reacting, saving
 * it for later and reporting it. They live here so no feature module depends on another.
 */
@Singleton
class PostActions @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {

    private fun savedCollection(uid: String) =
        firestore.collection(USERS).document(uid).collection(SAVED)

    /**
     * Ids of the posts the current user saved, newest first, kept up to date while collected.
     * The listener also reports local writes right away, so a toggle shows up instantly.
     */
    fun observeSavedIdsOrdered(): Flow<List<String>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return callbackFlow {
            val listener = savedCollection(uid)
                .orderBy(SAVED_AT, Query.Direction.DESCENDING)
                .limit(MAX_SAVED)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) trySend(snapshot.documents.map { it.id })
                }
            awaitClose { listener.remove() }
        }
    }

    /** Same as [observeSavedIdsOrdered], for screens that only ask "is this one saved?". */
    fun observeSavedIds(): Flow<Set<String>> = observeSavedIdsOrdered().map { it.toSet() }

    suspend fun setSaved(postId: String, saved: Boolean) {
        val uid = auth.currentUser?.uid ?: error("Usuario no autenticado")
        val ref = savedCollection(uid).document(postId)
        if (saved) ref.set(mapOf(SAVED_AT to System.currentTimeMillis())).await()
        else ref.delete().await()
    }

    /**
     * One report per user and post: the document id joins both, and the rules only allow
     * creating it, so a second report is detected instead of piling up duplicates.
     */
    suspend fun report(postId: String, reason: ReportReason, details: String?): ReportResult {
        val uid = auth.currentUser?.uid ?: error("Usuario no autenticado")
        val ref = firestore.collection(REPORTS).document("${postId}_$uid")
        if (ref.get().await().exists()) return ReportResult.ALREADY_REPORTED
        try {
            ref.set(
                mapOf(
                    "postId" to postId,
                    "reporterId" to uid,
                    "reason" to reason.name,
                    "details" to details?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_DETAILS),
                    "createdAt" to System.currentTimeMillis(),
                ),
            ).await()
        } catch (e: FirebaseFirestoreException) {
            // Sent from another device between the check and the write
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return ReportResult.ALREADY_REPORTED
            throw e
        }
        return ReportResult.SENT
    }

    /** The current user's reaction to [postId], or null when they have not reacted. */
    suspend fun myReaction(postId: String): String? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection(USERS).document(uid).collection(MY_REACTIONS).document(postId)
            .get().await()
            .getString(EMOJI)
    }

    /**
     * Reacts to [postId] with [emoji], or takes the reaction back when it is the same one.
     * Returns the user's reaction after the change, as the server decided it.
     */
    suspend fun toggleReaction(postId: String, emoji: String): String? {
        val uid = auth.currentUser?.uid ?: error("Usuario no autenticado")
        val postRef = firestore.collection(POSTS).document(postId)
        val userReactionRef = postRef.collection(USER_REACTIONS).document(uid)
        val mirrorRef = firestore.collection(USERS).document(uid).collection(MY_REACTIONS).document(postId)

        return firestore.runTransaction { tx ->
            // Only the user's own reaction is read. Reading the post too would make every
            // concurrent reaction or eco on it abort and retry this transaction; the
            // increments below are atomic without it.
            val currentEmoji = tx.get(userReactionRef).takeIf { it.exists() }?.getString(EMOJI)
            val nextEmoji = if (currentEmoji == emoji) null else emoji

            // Both counters change in a single update, which is what the security rules check
            val counterUpdates = buildMap<String, Any> {
                currentEmoji?.let { put("$REACTIONS.$it", FieldValue.increment(-1L)) }
                nextEmoji?.let { put("$REACTIONS.$it", FieldValue.increment(1L)) }
            }
            tx.update(postRef, counterUpdates)

            val reaction = nextEmoji?.let { mapOf(EMOJI to it) }
            if (reaction == null) {
                tx.delete(userReactionRef)
                tx.delete(mirrorRef)
            } else {
                tx.set(userReactionRef, reaction)
                tx.set(mirrorRef, reaction)
            }
            nextEmoji
        }.await()
    }

    private companion object {
        const val POSTS = "posts"
        const val USER_REACTIONS = "userReactions"
        const val REACTIONS = "reactions"
        const val EMOJI = "emoji"

        // Private copy of the user's reactions, keyed by post id
        const val MY_REACTIONS = "reactions"
        const val USERS = "users"
        const val SAVED = "saved"
        const val REPORTS = "reports"
        const val SAVED_AT = "savedAt"
        const val MAX_SAVED = 200L
        const val MAX_DETAILS = 500
    }
}
