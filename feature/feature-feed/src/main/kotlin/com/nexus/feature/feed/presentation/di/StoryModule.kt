package com.nexus.feature.feed.presentation.di

import com.nexus.feature.feed.data.remote.StoryRemoteDataSource
import com.nexus.feature.feed.data.remote.StoryRemoteDataSourceImpl
import com.nexus.feature.feed.data.repository.StoryRepositoryImpl
import com.nexus.feature.feed.domain.repository.StoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StoryModule {

    @Binds
    @Singleton
    abstract fun bindStoryRemoteDataSource(impl: StoryRemoteDataSourceImpl): StoryRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindStoryRepository(impl: StoryRepositoryImpl): StoryRepository
}
