package com.nexus.feature.circles.domain.model

/** A post published in a circle, as its list row shows it. */
data class CirclePost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val content: String,
    val imageUrl: String?,
    val imageCount: Int,
    val hasAudio: Boolean,
    val mood: String?,
    val ecoCount: Int,
    val reactionCount: Int,
    val createdAt: Long,
)

/** Someone in a circle. */
data class CircleMember(
    val uid: String,
    val name: String,
    val photoUrl: String?,
    val isOwner: Boolean,
    val isMe: Boolean,
)

/** What an invite code shows before joining: the circle's name and look, never its members. */
data class InvitePreview(
    val code: String,
    val circleId: String,
    val name: String,
    val emoji: String,
    val colorIndex: Int,
)

object InviteCode {
    // No I, O, 0 or 1: a code read aloud or copied by hand cannot be mistyped
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LENGTH = 8

    /** Uppercase, without spaces or dashes, as typed by the person joining. */
    fun normalize(input: String): String = input.uppercase().filter { it.isLetterOrDigit() }

    fun isValid(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }

    /** "ABCD2345" → "ABCD-2345", easier to read and share. */
    fun format(code: String): String =
        if (code.length == LENGTH) code.substring(0, 4) + "-" + code.substring(4) else code
}
