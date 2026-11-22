package com.nexus.feature.messages.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.nexus.core.common.dispatchers.IoDispatcher
import com.nexus.core.firebase.circle.CircleDirectory
import com.nexus.core.firebase.circle.UserBrief
import com.nexus.core.giphy.GiphyRepository
import com.nexus.feature.messages.domain.model.ChatMessage
import com.nexus.feature.messages.domain.model.Contact
import com.nexus.feature.messages.domain.model.Conversation
import com.nexus.feature.messages.domain.model.Conversations
import com.nexus.feature.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Thrown when a first message has no circle in common to start the conversation from. */
class NoSharedCircleException : IllegalStateException("No shared circle")

/**
 * Direct messages in Firestore. A message and the conversation's preview and counters are
 * written in one batch: the rules accept a message only if its conversation exists after it.
 */
class MessagesRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val directory: CircleDirectory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MessagesRepository {

    private val conversations get() = firestore.collection(CONVERSATIONS)

    override val currentUid: String? get() = directory.currentUid

    override fun observeConversations(): Flow<List<Conversation>> {
        val me = currentUid ?: return flowOf(emptyList())
        return callbackFlow {
            val listener = conversations
                .whereArrayContains(PARTICIPANTS, me)
                .orderBy(UPDATED_AT, Query.Direction.DESCENDING)
                .limit(CONVERSATIONS_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) trySend(snapshot.documents.mapNotNull { it.toConversation(me) })
                }
            awaitClose { listener.remove() }
        }
    }

    override fun observeContacts(): Flow<List<Contact>> {
        val me = currentUid ?: return flowOf(emptyList())
        return directory.observeMyCircles().map { circles ->
            val circlesByUid = circles
                .flatMap { circle -> circle.memberIds.filter { it != me }.map { it to circle } }
                .groupBy({ it.first }, { it.second })
            val users = directory.users(circlesByUid.keys)
            circlesByUid.map { (uid, shared) ->
                val user = users[uid]
                Contact(
                    uid = uid,
                    name = user?.name ?: CircleDirectory.UNKNOWN_NAME,
                    photoUrl = user?.photoUrl,
                    sharedCircleId = shared.first().id,
                    circleNames = shared.map { it.name },
                )
            }.sortedBy { it.name.lowercase() }
        }
    }

    override fun observeConversation(otherUid: String): Flow<Conversation?> {
        val me = currentUid ?: return flowOf(null)
        return callbackFlow {
            val listener = conversations.document(Conversations.idFor(me, otherUid))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) trySend(snapshot.toConversation(me))
                }
            awaitClose { listener.remove() }
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        // Metadata changes too, so a message stops showing as pending once the server has it
        val listener = conversations.document(conversationId).collection(MESSAGES)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(MESSAGES_LIMIT)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(
                        snapshot.documents.mapNotNull { doc ->
                            ChatMessage(
                                id = doc.id,
                                senderId = doc.getString("senderId") ?: return@mapNotNull null,
                                text = doc.getString("text").orEmpty(),
                                createdAt = doc.getLong(CREATED_AT) ?: 0L,
                                isPending = doc.metadata.hasPendingWrites(),
                                gifUrl = doc.getString("gifUrl"),
                            )
                        }.reversed(),
                    )
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun users(uids: Collection<String>): Map<String, UserBrief> = directory.users(uids)

    override suspend fun send(otherUid: String, text: String, sharedCircleId: String?, gifUrl: String?) = withContext(ioDispatcher) {
        val me = currentUid ?: throw IllegalStateException("Not signed in")
        val body = text.trim().take(Conversations.MAX_TEXT)
        require(gifUrl == null || GiphyRepository.isGiphyUrl(gifUrl)) { "Not a Giphy URL" }
        require(body.isNotEmpty() || gifUrl != null) { "Empty message" }
        val ref = conversations.document(Conversations.idFor(me, otherUid))
        val now = System.currentTimeMillis()
        val previewText = body.ifEmpty { Conversations.GIF_PREVIEW }.take(Conversations.MAX_PREVIEW)
        val preview = mapOf("text" to previewText, "senderId" to me, CREATED_AT to now)
        val exists = ref.get().await().exists()

        val batch = firestore.batch()
        if (exists) {
            batch.update(
                ref,
                FieldPath.of(LAST_MESSAGE), preview,
                FieldPath.of(UPDATED_AT), now,
                FieldPath.of(UNREAD, otherUid), FieldValue.increment(1),
                FieldPath.of(LAST_READ, me), now,
            )
        } else {
            val circleId = sharedCircleId ?: throw NoSharedCircleException()
            batch.set(
                ref,
                mapOf(
                    PARTICIPANTS to listOf(me, otherUid).sorted(),
                    "sharedCircleId" to circleId,
                    LAST_MESSAGE to preview,
                    UPDATED_AT to now,
                    UNREAD to mapOf(otherUid to 1, me to 0),
                    LAST_READ to mapOf(me to now, otherUid to 0L),
                ),
            )
        }
        batch.set(
            ref.collection(MESSAGES).document(),
            buildMap {
                put("senderId", me)
                put("text", body)
                put(CREATED_AT, now)
                if (gifUrl != null) put("gifUrl", gifUrl)
            },
        )
        batch.commit().await()
        Unit
    }

    override suspend fun markRead(conversationId: String) = withContext(ioDispatcher) {
        val me = currentUid ?: return@withContext
        conversations.document(conversationId).update(
            FieldPath.of(UNREAD, me), 0,
            FieldPath.of(LAST_READ, me), System.currentTimeMillis(),
        ).await()
        Unit
    }

    private fun DocumentSnapshot.toConversation(me: String): Conversation? {
        if (!exists()) return null
        val participants = (get(PARTICIPANTS) as? List<*>)?.filterIsInstance<String>().orEmpty()
        val other = participants.firstOrNull { it != me } ?: return null
        val last = get(LAST_MESSAGE) as? Map<*, *>
        val unread = get(UNREAD) as? Map<*, *>
        val lastRead = get(LAST_READ) as? Map<*, *>
        return Conversation(
            id = id,
            otherUid = other,
            lastText = last?.get("text") as? String ?: "",
            lastSenderId = last?.get("senderId") as? String ?: "",
            updatedAt = getLong(UPDATED_AT) ?: 0L,
            unread = (unread?.get(me) as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
            otherLastRead = (lastRead?.get(other) as? Number)?.toLong() ?: 0L,
        )
    }

    private companion object {
        const val CONVERSATIONS = "conversations"
        const val MESSAGES = "messages"
        const val PARTICIPANTS = "participants"
        const val LAST_MESSAGE = "lastMessage"
        const val UPDATED_AT = "updatedAt"
        const val UNREAD = "unread"
        const val LAST_READ = "lastRead"
        const val CREATED_AT = "createdAt"
        const val CONVERSATIONS_LIMIT = 100L
        const val MESSAGES_LIMIT = 200L
    }
}
