package com.nexus.core.giphy

/** What the picker shows: GIFs, or animated emoji with a transparent background. */
enum class GiphyKind { GIF, EMOJI }

/**
 * One result. [previewUrl] is a small rendition for the grid; [url] is the one that gets
 * stored and shown in posts, ecos and messages (under 2 MB, unlike the original).
 */
data class GiphyItem(
    val id: String,
    val title: String,
    val previewUrl: String,
    val url: String,
)

class GiphyRepository(
    private val api: GiphyApi,
    private val apiKey: String,
) {
    /** False when the build has no GIPHY_API_KEY: the pickers say so instead of failing. */
    val isAvailable: Boolean get() = apiKey.isNotBlank()

    suspend fun search(kind: GiphyKind, query: String): List<GiphyItem> {
        check(isAvailable) { "GIPHY_API_KEY is not set" }
        val q = query.trim()
        val response = when (kind) {
            GiphyKind.GIF -> if (q.isEmpty()) api.trendingGifs(apiKey) else api.searchGifs(q, apiKey)
            GiphyKind.EMOJI -> if (q.isEmpty()) api.emoji(apiKey) else api.searchStickers(q, apiKey)
        }
        return response.data.mapNotNull { it.toItem() }.distinctBy { it.id }
    }

    companion object {
        /** Only Giphy's own hosts: the Firestore rules accept nothing else as a GIF. */
        private val GIPHY_URL = Regex("^https://([a-z0-9-]+\\.)*giphy\\.com/.+")

        fun isGiphyUrl(url: String?): Boolean = url != null && url.length <= 2048 && GIPHY_URL.matches(url)
    }
}

internal fun GiphyGifDto.toItem(): GiphyItem? {
    val full = listOf(images.downsized, images.fixedHeight, images.original)
        .map { it.url }.firstOrNull { it.isNotBlank() } ?: return null
    val preview = listOf(images.fixedWidthSmall, images.fixedHeightSmall, images.fixedHeight)
        .map { it.url }.firstOrNull { it.isNotBlank() } ?: full
    if (id.isBlank() || !GiphyRepository.isGiphyUrl(full)) return null
    return GiphyItem(id = id, title = title, previewUrl = preview, url = full)
}
