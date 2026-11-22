package com.nexus.feature.messages.domain.model

/** A conversation as stored in conversations/{a_b}, seen from the current user. */
data class Conversation(
    val id: String,
    val otherUid: String,
    val lastText: String,
    val lastSenderId: String,
    val updatedAt: Long,
    /** Messages the current user has not read. */
    val unread: Int,
    /** When the other person last opened it, to show "Visto". */
    val otherLastRead: Long,
)

/** A row of the conversation list: the conversation plus who it is with. */
data class ConversationItem(
    val conversation: Conversation,
    val otherName: String,
    val otherPhotoUrl: String?,
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val createdAt: Long,
    /** Written on this device and not yet confirmed by the server. */
    val isPending: Boolean,
    /** A GIF or animated emoji from Giphy; [text] is empty then. */
    val gifUrl: String? = null,
)

/** Someone the user may write to: they share at least one circle. */
data class Contact(
    val uid: String,
    val name: String,
    val photoUrl: String?,
    /** A circle both are in; a new conversation records it, as the rules require. */
    val sharedCircleId: String,
    val circleNames: List<String>,
)

object Conversations {
    const val MAX_TEXT = 2000
    const val MAX_PREVIEW = 200

    /** What the conversation list shows for a message that is only a GIF. */
    const val GIF_PREVIEW = "GIF"

    /** One conversation per pair: the two uids sorted, joined with "_". */
    fun idFor(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"
}
