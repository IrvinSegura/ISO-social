package com.nexus.feature.messages.domain.repository

import com.nexus.core.firebase.circle.UserBrief
import com.nexus.feature.messages.domain.model.ChatMessage
import com.nexus.feature.messages.domain.model.Contact
import com.nexus.feature.messages.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface MessagesRepository {
    val currentUid: String?

    /** The user's conversations, most recent first, kept up to date. */
    fun observeConversations(): Flow<List<Conversation>>

    /** People who share a circle with the user, by name. */
    fun observeContacts(): Flow<List<Contact>>

    /** The conversation with [otherUid]; null while it does not exist yet. */
    fun observeConversation(otherUid: String): Flow<Conversation?>

    /** The latest messages of an existing conversation, oldest first. */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    suspend fun users(uids: Collection<String>): Map<String, UserBrief>

    /**
     * Sends [text], or a Giphy [gifUrl], to [otherUid]. The first message creates the
     * conversation, which needs a circle both share ([sharedCircleId]); later ones only
     * update its preview and counters.
     */
    suspend fun send(otherUid: String, text: String, sharedCircleId: String?, gifUrl: String? = null)

    /** Clears the user's unread count and records when they read it. */
    suspend fun markRead(conversationId: String)
}
