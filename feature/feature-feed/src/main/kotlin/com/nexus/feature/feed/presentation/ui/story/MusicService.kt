package com.nexus.feature.feed.presentation.ui.story

import android.util.Base64
import com.nexus.feature.feed.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal object MusicService {

    // HttpURLConnection waits forever by default; a blocked read would hold an IO thread
    // and leave the story silent, since a blocking read does not react to cancellation
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 8_000

    private const val ITUNES_SEARCH = "https://itunes.apple.com/search"
    private const val COUNTRY = "MX"

    /** GET (or POST with [body]) that returns the response text, or null on a non-200 answer. */
    private fun request(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
            if (body != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) null
            else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    // ── iTunes Search API (fuente principal — sin auth, siempre funciona) ─────

    private suspend fun searchItunes(query: String): List<MusicEntry> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "$ITUNES_SEARCH?term=${encode(query)}&media=music&limit=25&country=$COUNTRY"
                val results = JSONObject(request(url) ?: return@runCatching emptyList())
                    .getJSONArray("results")

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
            }.onFailure { Timber.e(it, "iTunes search failed") }
                .getOrDefault(emptyList())
        }

    /**
     * Preview URL for a story saved with an old (Deezer) or empty URL: looks the song up
     * again on iTunes by title and artist.
     */
    suspend fun resolvePreviewUrl(title: String, artist: String?): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val term = encode("$title ${artist.orEmpty()}".trim())
                val url = "$ITUNES_SEARCH?term=$term&media=music&limit=1&country=$COUNTRY"
                val results = JSONObject(request(url) ?: return@runCatching null)
                    .getJSONArray("results")
                if (results.length() > 0) {
                    results.getJSONObject(0).optString("previewUrl").ifBlank { null }
                } else null
            }.onFailure { Timber.w(it, "iTunes preview lookup failed") }
                .getOrNull()
        }

    // ── Spotify (opcional — enriquece con URI nativo de Spotify) ─────────────

    // Two searches can ask for a token at the same time; @Volatile keeps them consistent
    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiresAt: Long = 0L

    private suspend fun spotifyToken(): String? = withContext(Dispatchers.IO) {
        // Release builds ship without credentials: skip the request entirely
        if (BuildConfig.SPOTIFY_CLIENT_ID.isBlank()) return@withContext null
        runCatching {
            val now = System.currentTimeMillis()
            val existing = cachedToken
            if (existing != null && now < tokenExpiresAt - 60_000) return@runCatching existing

            val credentials = "${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}"
            val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            val response = request(
                url = "https://accounts.spotify.com/api/token",
                headers = mapOf(
                    "Authorization" to "Basic $encoded",
                    "Content-Type" to "application/x-www-form-urlencoded",
                ),
                body = "grant_type=client_credentials",
            ) ?: return@runCatching null
            val json = JSONObject(response)
            val token = json.getString("access_token")
            tokenExpiresAt = now + json.getLong("expires_in") * 1000
            cachedToken = token
            token
        }.onFailure { Timber.w(it, "Spotify token failed (no pasa nada, usamos iTunes)") }
            .getOrNull()
    }

    // Title alone is not unique ("Intro", "Home"): matching on title + artist avoids
    // giving one song the cover and link of another
    private fun matchKey(title: String, artist: String) =
        "${title.lowercase().trim()}|${artist.lowercase().trim()}"

    private suspend fun enrichWithSpotify(
        entries: List<MusicEntry>,
        query: String,
    ): List<MusicEntry> = withContext(Dispatchers.IO) {
        val token = spotifyToken() ?: return@withContext entries
        runCatching {
            val url = "https://api.spotify.com/v1/search?q=${encode(query)}&type=track&limit=25&market=$COUNTRY"
            val response = request(url, headers = mapOf("Authorization" to "Bearer $token"))
                ?: return@runCatching entries
            val items = JSONObject(response).getJSONObject("tracks").getJSONArray("items")

            val spotifyMap = buildMap {
                for (i in 0 until items.length()) {
                    val t = items.getJSONObject(i)
                    val artists = t.optJSONArray("artists")
                    val firstArtist = if (artists != null && artists.length() > 0) {
                        artists.getJSONObject(0).optString("name")
                    } else ""
                    val sUrl = t.optJSONObject("external_urls")
                        ?.optString("spotify")?.ifBlank { null }
                    val sUri = t.optString("uri").ifBlank { null }
                    val sArt = t.getJSONObject("album").getJSONArray("images")
                        .let { imgs -> if (imgs.length() > 0) imgs.getJSONObject(0).getString("url") else null }
                    putIfAbsent(matchKey(t.getString("name"), firstArtist), Triple(sUrl, sUri, sArt))
                }
            }

            entries.map { entry ->
                val extra = spotifyMap[matchKey(entry.title, entry.artist)]
                if (extra != null) {
                    entry.copy(
                        spotifyUrl = extra.first ?: entry.spotifyUrl,
                        spotifyUri = extra.second ?: entry.spotifyUri,
                        albumArtUrl = extra.third ?: entry.albumArtUrl,
                    )
                } else entry
            }
        }.onFailure { Timber.w(it, "Spotify enrich failed") }
            .getOrDefault(entries)
    }

    // ── API pública ───────────────────────────────────────────────────────────

    suspend fun searchTracks(query: String): List<MusicEntry> {
        // iTunes busca siempre (confiable, sin auth); Spotify solo enriquece
        val entries = searchItunes(query)
        if (entries.isEmpty()) return emptyList()
        return enrichWithSpotify(entries, query)
    }
}
