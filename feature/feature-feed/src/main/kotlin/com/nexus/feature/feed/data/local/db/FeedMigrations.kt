package com.nexus.feature.feed.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema changes of the feed cache. Every version bump adds one migration here, so an
 * update keeps the cached feed and the user's pending reactions instead of wiping them.
 * Exported schemas live in feature/feature-feed/schemas (from version 5 on).
 */
internal object FeedMigrations {

    // v5: index for the feed query (ORDER BY createdAt DESC, id DESC)
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_posts_createdAt_id` ON `posts` (`createdAt`, `id`)",
            )
        }
    }

    // v6: a post can carry a GIF from Giphy
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `posts` ADD COLUMN `gifUrl` TEXT")
        }
    }

    val ALL = arrayOf(MIGRATION_4_5, MIGRATION_5_6)

    // Versions from before migrations existed: their schema was never exported, so they
    // are rebuilt from scratch (it is only a cache of Firestore)
    val VERSIONS_WITHOUT_MIGRATION = intArrayOf(1, 2, 3)
}
