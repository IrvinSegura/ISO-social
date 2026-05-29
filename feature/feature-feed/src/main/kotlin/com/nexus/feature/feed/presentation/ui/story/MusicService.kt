package com.nexus.feature.feed.presentation.ui.story

import android.util.Base64
import android.util.Log
import com.nexus.feature.feed.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object MusicService {

    private const val TAG = "MusicService"

    // ── iTunes Search API (fuente principal — sin auth, siempre funciona) ─────

    private suspend fun searchItunes(query: String): List<MusicEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://itunes.apple.com/search?term=$q&media=music&limit=25&country=MX"
                val json = JSONObject(URL(url).readText())
                val results = json.getJSONArray("results")

                (0 until results.length()).map { i ->
                    val track = results.getJSONObject(i)
                    val artUrl = track.optString("artworkUrl100").ifBlank { null }
                        ?.replace("100x100bb", "600x600bb")
                    MusicEntry(
                        title = track.optString("trackName").ifBlank { "Sin título" },
                        artist = track.optString("artistName").ifBlank { "Artista desconocido" },
                        previewUrl = track.optString("previewUrl").ifBlank { null },
                        albumArtUrl = artUrl,
                        trackDurationMs = track.optLong("trackTimeMillis", 0L),
                        spotifyUrl = track.optString("trackViewUrl").ifBlank { null },
                    )
                }
            }.onFailure { Log.e(TAG, "iTunes search failed", it) }
             .getOrDefault(emptyList())
        }

    // ── Spotify (opcional — enriquece con URI nativo de Spotify) ─────────────

    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L

    private suspend fun spotifyToken(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val existing = cachedToken
            if (existing != null && now < tokenExpiresAt - 60_000) return@runCatching existing

            val credentials = "${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}"
            val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

            val conn = URL("https://accounts.spotify.com/api/token")
                .openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Basic $encoded")
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.doOutput = true
                conn.outputStream.write("grant_type=client_credentials".toByteArray())
                conn.connect()
                if (conn.responseCode != 200) return@runCatching null
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val token = json.getString("access_token")
                cachedToken = token
                tokenExpiresAt = now + json.getLong("expires_in") * 1000
                token
            } finally {
                conn.disconnect()
            }
        }.onFailure { Log.w(TAG, "Spotify token failed (no pasa nada, usamos iTunes)", it) }
         .getOrNull()
    }

    private suspend fun enrichWithSpotify(
        entries: List<MusicEntry>,
        query: String,
    ): List<MusicEntry> = withContext(Dispatchers.IO) {
        val token = spotifyToken() ?: return@withContext entries
        runCatching {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://api.spotify.com/v1/search?q=$encoded&type=track&limit=25&market=MX"
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connect()
                if (conn.responseCode != 200) return@runCatching entries

                val items = JSONObject(conn.inputStream.bufferedReader().readText())
                    .getJSONObject("tracks").getJSONArray("items")

                // Mapa título.lowercase → spotifyUrl + spotifyUri
                val spotifyMap = buildMap {
                    for (i in 0 until items.length()) {
                        val t = items.getJSONObject(i)
                        val key = t.getString("name").lowercase().trim()
                        val sUrl = t.optJSONObject("external_urls")
                            ?.optString("spotify")?.ifBlank { null }
                        val sUri = t.optString("uri").ifBlank { null }
                        val sArt = t.getJSONObject("album").getJSONArray("images")
                            .let { imgs -> if (imgs.length() > 0) imgs.getJSONObject(0).getString("url") else null }
                        put(key, Triple(sUrl, sUri, sArt))
                    }
                }

                entries.map { entry ->
                    val extra = spotifyMap[entry.title.lowercase().trim()]
                    if (extra != null) {
                        entry.copy(
                            spotifyUrl = extra.first ?: entry.spotifyUrl,
                            spotifyUri = extra.second ?: entry.spotifyUri,
                            albumArtUrl = extra.third ?: entry.albumArtUrl,
                        )
                    } else entry
                }
            } finally {
                conn.disconnect()
            }
        }.onFailure { Log.w(TAG, "Spotify enrich failed", it) }
         .getOrDefault(entries)
    }

    // ── API pública ───────────────────────────────────────────────────────────

    suspend fun searchTracks(query: String): List<MusicEntry> = coroutineScope {
        // iTunes busca siempre (confiable, sin auth)
        val itunesDeferred = async { searchItunes(query) }
        // Spotify enriquece en paralelo (opcional)
        val entries = itunesDeferred.await()
        if (entries.isEmpty()) return@coroutineScope emptyList()
        enrichWithSpotify(entries, query)
    }
}
