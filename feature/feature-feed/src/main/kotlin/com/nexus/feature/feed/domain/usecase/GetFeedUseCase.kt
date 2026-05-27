package com.nexus.feature.feed.domain.usecase

import androidx.paging.PagingData
import com.nexus.feature.feed.domain.model.Post
import com.nexus.feature.feed.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFeedUseCase @Inject constructor(
    private val repository: FeedRepository,
) {
    operator fun invoke(): Flow<PagingData<Post>> = repository.getFeed()
}
