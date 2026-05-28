package com.nexus.feature.post.domain.usecase

import android.net.Uri
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.repository.PostRepository
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val repository: PostRepository,
) {
    suspend operator fun invoke(
        content: String,
        circleId: String?,
        circleName: String?,
        imageUris: List<Uri>,
        audioPath: String?,
        fileUri: Uri?,
        fileName: String?,
        mood: String?,
        locationName: String?,
    ): NexusResult<String> = repository.createPost(
        content = content.trim(),
        circleId = circleId,
        circleName = circleName,
        imageUris = imageUris,
        audioPath = audioPath,
        fileUri = fileUri,
        fileName = fileName,
        mood = mood,
        locationName = locationName,
    )
}
