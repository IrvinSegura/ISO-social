package com.nexus.feature.post.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TenorSearchResponse(
    val results: List<TenorResult> = emptyList(),
    val next: String = "",
)

@Serializable
data class TenorResult(
    val id: String = "",
    val title: String = "",
    @SerialName("media_formats")
    val mediaFormats: Map<String, TenorMediaFormat> = emptyMap(),
) {
    val previewUrl: String get() = mediaFormats["tinygif"]?.url
        ?: mediaFormats["gif"]?.url
        ?: ""

    val fullUrl: String get() = mediaFormats["gif"]?.url
        ?: mediaFormats["tinygif"]?.url
        ?: ""
}

@Serializable
data class TenorMediaFormat(
    val url: String = "",
    val dims: List<Int> = emptyList(),
    val size: Long = 0L,
)
