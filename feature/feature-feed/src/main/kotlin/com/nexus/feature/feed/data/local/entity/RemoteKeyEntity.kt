package com.nexus.feature.feed.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val postId: String,
    val prevKey: Long?,
    val nextKey: Long?,
)
