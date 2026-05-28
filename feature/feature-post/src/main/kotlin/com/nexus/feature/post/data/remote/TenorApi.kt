package com.nexus.feature.post.data.remote

import com.nexus.feature.post.data.model.TenorSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TenorApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("key") apiKey: String = API_KEY,
        @Query("client_key") clientKey: String = CLIENT_KEY,
        @Query("limit") limit: Int = 24,
        @Query("media_filter") mediaFilter: String = "gif,tinygif",
    ): TenorSearchResponse

    @GET("featured")
    suspend fun featured(
        @Query("key") apiKey: String = API_KEY,
        @Query("client_key") clientKey: String = CLIENT_KEY,
        @Query("limit") limit: Int = 24,
        @Query("media_filter") mediaFilter: String = "gif,tinygif",
    ): TenorSearchResponse

    companion object {
        const val BASE_URL = "https://tenor.googleapis.com/v2/"
        // Obtén tu API key gratis en https://tenor.com/developer/dashboard
        const val API_KEY = "YOUR_TENOR_API_KEY"
        private const val CLIENT_KEY = "nexus_social"
    }
}
