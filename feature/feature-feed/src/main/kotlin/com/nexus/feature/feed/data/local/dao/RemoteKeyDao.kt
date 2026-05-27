package com.nexus.feature.feed.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nexus.feature.feed.data.local.entity.RemoteKeyEntity

@Dao
interface RemoteKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<RemoteKeyEntity>)

    @Query("SELECT * FROM feed_remote_keys WHERE postId = :postId")
    suspend fun remoteKeyForPost(postId: String): RemoteKeyEntity?

    @Query("DELETE FROM feed_remote_keys")
    suspend fun clearAll()
}
