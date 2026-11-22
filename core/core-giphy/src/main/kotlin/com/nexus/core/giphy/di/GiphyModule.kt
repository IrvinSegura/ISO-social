package com.nexus.core.giphy.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nexus.core.giphy.BuildConfig
import com.nexus.core.giphy.GiphyApi
import com.nexus.core.giphy.GiphyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GiphyModule {

    @Provides
    @Singleton
    fun provideGiphyApi(): GiphyApi {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        return Retrofit.Builder()
            .baseUrl(GiphyApi.BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GiphyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGiphyRepository(api: GiphyApi): GiphyRepository =
        GiphyRepository(api, BuildConfig.GIPHY_API_KEY)
}
