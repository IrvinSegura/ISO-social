package com.nexus.feature.post.presentation.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nexus.feature.post.data.remote.GiphyApi
import com.nexus.feature.post.data.remote.PostRemoteDataSource
import com.nexus.feature.post.data.remote.PostRemoteDataSourceImpl
import com.nexus.feature.post.data.repository.PostRepositoryImpl
import com.nexus.feature.post.domain.repository.PostRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Named
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

    companion object {

        @Provides
        @Singleton
        @Named("giphy")
        fun provideGiphyRetrofit(): Retrofit {
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
            return Retrofit.Builder()
                .baseUrl(GiphyApi.BASE_URL)
                .addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType()),
                )
                .build()
        }

        @Provides
        @Singleton
        fun provideGiphyApi(@Named("giphy") retrofit: Retrofit): GiphyApi =
            retrofit.create(GiphyApi::class.java)
    }
}
