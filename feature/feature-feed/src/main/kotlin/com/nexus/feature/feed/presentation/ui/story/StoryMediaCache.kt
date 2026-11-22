package com.nexus.feature.feed.presentation.ui.story

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

/**
 * Disk cache for story videos: watching a story again, or going back to the previous one,
 * plays from disk instead of downloading the whole video again.
 */
@OptIn(UnstableApi::class)
internal object StoryMediaCache {

    private const val MAX_BYTES = 100L * 1024 * 1024

    // SimpleCache locks its folder: there must be exactly one instance per process
    @Volatile private var cache: SimpleCache? = null

    private fun cache(context: Context): SimpleCache =
        cache ?: synchronized(this) {
            cache ?: SimpleCache(
                File(context.cacheDir, "story_media"),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context),
            ).also { cache = it }
        }

    /** ExoPlayer whose network reads go through the cache. */
    fun newPlayer(context: Context): ExoPlayer {
        val appContext = context.applicationContext
        val dataSource = CacheDataSource.Factory()
            .setCache(cache(appContext))
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(appContext))
            // A failing cache (full disk...) must not stop playback: read from the network
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .build()
    }
}
