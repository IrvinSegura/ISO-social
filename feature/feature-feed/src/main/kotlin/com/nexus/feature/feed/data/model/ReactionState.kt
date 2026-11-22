package com.nexus.feature.feed.data.model

/** Reaction counters of a post plus the emoji the current user reacted with, if any. */
data class ReactionState(
    val reactions: Map<String, Int>,
    val myReaction: String?,
) {
    /** Tapping the user's current emoji removes it; any other emoji replaces it. */
    fun toggle(emoji: String): ReactionState = switchTo(if (myReaction == emoji) null else emoji)

    /** Moves the user's reaction to [target] (null = no reaction), adjusting both counters. */
    fun switchTo(target: String?): ReactionState {
        if (target == myReaction) return this
        val counts = reactions.toMutableMap()
        myReaction?.let { counts.addCount(it, -1) }
        target?.let { counts.addCount(it, 1) }
        return ReactionState(counts, target)
    }

    private fun MutableMap<String, Int>.addCount(emoji: String, delta: Int) {
        val count = (this[emoji] ?: 0) + delta
        if (count > 0) this[emoji] = count else remove(emoji)
    }
}
