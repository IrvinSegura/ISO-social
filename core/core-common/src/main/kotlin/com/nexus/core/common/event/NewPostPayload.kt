package com.nexus.core.common.event

data class NewPostPayload(
    val tempId: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val content: String,
    val mood: String?,
    val locationName: String?,
    val createdAt: Long,
    val isPending: Boolean,
    val confirmedId: String? = null,
)
