package com.nexus.feature.post.data.remote

import com.nexus.feature.post.data.model.GiphySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("api_key") apiKey: String = API_KEY,
        @Query("limit") limit: Int = 25,
        @Query("rating") rating: String = "g",
        @Query("lang") lang: String = "es",
    ): GiphySearchResponse

    @GET("trending")
    suspend fun trending(
        @Query("api_key") apiKey: String = API_KEY,
        @Query("limit") limit: Int = 25,
        @Query("rating") rating: String = "g",
    ): GiphySearchResponse

    companion object {
        const val BASE_URL = "https://api.giphy.com/v1/gifs/"
        // Obtén tu API key gratis en https://developers.giphy.com/
        const val API_KEY = "YOUR_GIPHY_API_KEY"
    }
}
