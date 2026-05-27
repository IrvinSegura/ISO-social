package com.nexus.feature.feed.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexus.feature.feed.data.local.dao.PostDao
import com.nexus.feature.feed.data.local.dao.RemoteKeyDao
import com.nexus.feature.feed.data.local.entity.PostEntity
import com.nexus.feature.feed.data.local.entity.RemoteKeyEntity

@Database(
    entities = [PostEntity::class, RemoteKeyEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FeedDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun remoteKeyDao(): RemoteKeyDao
}
