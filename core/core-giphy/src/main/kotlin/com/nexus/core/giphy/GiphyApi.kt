package com.nexus.core.giphy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** The parts of the Giphy API the pickers use: GIFs, and animated emoji (stickers). */
interface GiphyApi {

    @GET("v1/gifs/trending")
    suspend fun trendingGifs(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("rating") rating: String = RATING,
    ): GiphyResponse

    @GET("v1/gifs/search")
    suspend fun searchGifs(
        @Query("q") query: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("rating") rating: String = RATING,
        @Query("lang") lang: String = LANG,
    ): GiphyResponse

    /** Giphy's own animated emoji set. */
    @GET("v2/emoji")
    suspend fun emoji(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = EMOJI_PAGE_SIZE,
    ): GiphyResponse

    /** Stickers have a transparent background, so a search for emoji uses them. */
    @GET("v1/stickers/search")
    suspend fun searchStickers(
        @Query("q") query: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = EMOJI_PAGE_SIZE,
        @Query("rating") rating: String = RATING,
        @Query("lang") lang: String = LANG,
    ): GiphyResponse

    companion object {
        const val BASE_URL = "https://api.giphy.com/"
        const val PAGE_SIZE = 30
        const val EMOJI_PAGE_SIZE = 48
        private const val RATING = "g"
        private const val LANG = "es"
    }
}

@Serializable
data class GiphyResponse(
    val data: List<GiphyGifDto> = emptyList(),
)

@Serializable
data class GiphyGifDto(
    val id: String = "",
    val title: String = "",
    val images: GiphyImages = GiphyImages(),
)

/** Renditions Giphy sends for each GIF; not every one comes for every item. */
@Serializable
data class GiphyImages(
    @SerialName("fixed_width_small") val fixedWidthSmall: GiphyRendition = GiphyRendition(),
    @SerialName("fixed_height_small") val fixedHeightSmall: GiphyRendition = GiphyRendition(),
    @SerialName("fixed_height") val fixedHeight: GiphyRendition = GiphyRendition(),
    val downsized: GiphyRendition = GiphyRendition(),
    val original: GiphyRendition = GiphyRendition(),
)

@Serializable
data class GiphyRendition(
    val url: String = "",
)
