package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.health.data.local.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage): Long

    @Query("SELECT * FROM chat_message ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_message ORDER BY timestamp ASC")
    suspend fun getAllMessagesOnce(): List<ChatMessage>

    /**
     * 滑动窗口：仅取最近 N 条用于 API 调用。
     */
    @Query("SELECT * FROM chat_message ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessage>

    @Query("DELETE FROM chat_message")
    suspend fun deleteAll()
}
