package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "advice_log")
data class AdviceLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,               // "yyyy-MM-dd"
    val requestSnapshot: String,    // 发送给AI的上下文快照(JSON)
    val aiResponse: String          // AI 返回的评估建议
)
