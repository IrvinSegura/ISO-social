package com.nexus.feature.feed.presentation.di

import android.content.Context
import androidx.room.Room
import com.nexus.feature.feed.data.local.db.FeedDatabase
import com.nexus.feature.feed.data.remote.FeedRemoteDataSource
import com.nexus.feature.feed.data.remote.FeedRemoteDataSourceImpl
import com.nexus.feature.feed.data.repository.FeedRepositoryImpl
import com.nexus.feature.feed.domain.repository.FeedRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedModule {

    @Binds
    @Singleton
    abstract fun bindFeedRepository(impl: FeedRepositoryImpl): FeedRepository

    @Binds
    @Singleton
    abstract fun bindFeedRemoteDataSource(impl: FeedRemoteDataSourceImpl): FeedRemoteDataSource

    companion object {

        @Provides
        @Singleton
        fun provideFeedDatabase(@ApplicationContext context: Context): FeedDatabase =
            Room.databaseBuilder(context, FeedDatabase::class.java, "nexus_feed_db").build()
    }
}
