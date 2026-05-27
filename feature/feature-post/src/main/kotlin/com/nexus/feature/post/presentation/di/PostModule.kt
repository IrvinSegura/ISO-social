package com.nexus.feature.post.presentation.di

import com.nexus.feature.post.data.remote.PostRemoteDataSource
import com.nexus.feature.post.data.remote.PostRemoteDataSourceImpl
import com.nexus.feature.post.data.repository.PostRepositoryImpl
import com.nexus.feature.post.domain.repository.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PostModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository

    @Binds
    @Singleton
    abstract fun bindPostRemoteDataSource(impl: PostRemoteDataSourceImpl): PostRemoteDataSource
}
