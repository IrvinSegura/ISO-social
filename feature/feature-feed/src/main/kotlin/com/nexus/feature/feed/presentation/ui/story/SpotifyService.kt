package com.nexus.feature.feed.presentation.ui.story

import android.util.Base64
import com.nexus.feature.feed.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object SpotifyService {

    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L

    private suspend fun accessToken(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedToken?.takeIf { now < tokenExpiresAt - 60_000 }?.let { return@withContext it }

        val credentials = "${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val conn = URL("https://accounts.spotify.com/api/token").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Basic $encoded")
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.doOutput = true
        conn.outputStream.write("grant_type=client_credentials".toByteArray())

        val json = JSONObject(conn.inputStream.bufferedReader().readText())
        val token = json.getString("access_token")
        val expiresIn = json.getLong("expires_in")

        cachedToken = token
        tokenExpiresAt = now + expiresIn * 1000
        token
    }

    suspend fun searchTracks(query: String): List<MusicEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val token = accessToken()
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://api.spotify.com/v1/search?q=$encoded&type=track&limit=20&market=MX"

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")

            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val items = json.getJSONObject("tracks").getJSONArray("items")

            (0 until items.length()).mapNotNull { i ->
                val track = items.getJSONObject(i)
                val artists = track.getJSONArray("artists")
                val artistName = artists.getJSONObject(0).getString("name")
                val album = track.getJSONObject("album")
                val images = album.getJSONArray("images")
                val artUrl = if (images.length() > 0) images.getJSONObject(0).getString("url") else null
                val previewUrl = track.optString("preview_url").ifBlank { null }

                MusicEntry(
                    title = track.getString("name"),
                    artist = artistName,
                    previewUrl = previewUrl,
                    albumArtUrl = artUrl,
                )
            }
        }.getOrDefault(emptyList())
    }
}
