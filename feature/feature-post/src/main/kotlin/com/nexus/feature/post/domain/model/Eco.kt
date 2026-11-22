package com.nexus.feature.post.domain.model

enum class EcoType { TEXT, EMOJI, GIF, IMAGE, AUDIO }

data class Eco(
    val id: String,
    val authorId: String,
    val authorName: String,
    val type: EcoType = EcoType.TEXT,
    val message: String = "",
    val emoji: String? = null,
    val mediaUrl: String? = null,
    val createdAt: Long,
    /** The top-level eco this one answers, or null for a top-level eco. */
    val parentId: String? = null,
    /** Who this reply answers, shown as @name; null when it answers the thread itself. */
    val replyToName: String? = null,
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
)

/** Who a new eco answers: the thread it goes into and the person it names. */
data class EcoReplyTarget(
    val parentId: String,
    val name: String,
)

/** A top-level eco and its replies, oldest first. */
data class EcoThread(
    val root: Eco,
    val replies: List<Eco>,
)

/**
 * Groups [ecos] (oldest first) into one-level threads. A reply whose top-level eco was not
 * loaded (it fell outside the newest ones) is shown as its own thread so it is not lost.
 */
fun buildThreads(ecos: List<Eco>): List<EcoThread> {
    val rootIds = ecos.asSequence().filter { it.parentId == null }.map { it.id }.toSet()
    val repliesByParent = ecos
        .filter { it.parentId != null && it.parentId in rootIds }
        .groupBy { it.parentId }
    return ecos
        .filter { it.parentId == null || it.parentId !in rootIds }
        .map { root -> EcoThread(root, repliesByParent[root.id].orEmpty()) }
}

/** The target for answering [eco]: its own thread, or the thread it belongs to. */
fun Eco.replyTarget(): EcoReplyTarget = EcoReplyTarget(parentId = parentId ?: id, name = authorName)
