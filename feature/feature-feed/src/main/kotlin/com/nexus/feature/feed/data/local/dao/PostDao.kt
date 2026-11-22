package com.nexus.feature.feed.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nexus.feature.feed.data.local.entity.PostEntity
import com.nexus.feature.feed.data.local.entity.PostReactionRow

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY createdAt DESC, id DESC")
    fun pagingSource(): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getById(postId: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("SELECT id, myReaction FROM posts WHERE id IN (:postIds) AND myReaction IS NOT NULL")
    suspend fun getMyReactions(postIds: List<String>): List<PostReactionRow>

    @Query("UPDATE posts SET reactions = :reactions, myReaction = :myReaction WHERE id = :postId")
    suspend fun updateReactions(postId: String, reactions: Map<String, Int>, myReaction: String?)

    @Query("UPDATE posts SET ecoCount = MAX(0, ecoCount + :delta) WHERE id = :postId")
    suspend fun addToEcoCount(postId: String, delta: Int)

    @Query("UPDATE posts SET ecoCount = :count WHERE id = :postId")
    suspend fun setEcoCount(postId: String, count: Int)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deleteById(postId: String)

    @Query("DELETE FROM posts")
    suspend fun clearAll()
}
