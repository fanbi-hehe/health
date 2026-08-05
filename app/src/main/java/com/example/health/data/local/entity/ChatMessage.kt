package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_message")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String,              // "user" / "assistant"
    val content: String,           // 文本内容（可为空，如图片消息）
    val imagePath: String? = null, // 用户上传的图片路径（本地缓存）
    val timestamp: Long            // System.currentTimeMillis()
)
