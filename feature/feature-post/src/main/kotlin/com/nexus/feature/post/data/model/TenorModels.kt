package com.nexus.feature.post.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GiphySearchResponse(
    val data: List<GiphyGif> = emptyList(),
)

@Serializable
data class GiphyGif(
    val id: String = "",
    val title: String = "",
    val images: GiphyImages = GiphyImages(),
) {
    val previewUrl: String get() = images.fixedHeightSmall.url.ifBlank { images.original.url }
    val fullUrl: String get() = images.original.url.ifBlank { images.fixedHeightSmall.url }
}

@Serializable
data class GiphyImages(
    @SerialName("fixed_height_small")
    val fixedHeightSmall: GiphyImageItem = GiphyImageItem(),
    val original: GiphyImageItem = GiphyImageItem(),
)

@Serializable
data class GiphyImageItem(
    val url: String = "",
    val width: String = "",
    val height: String = "",
)
