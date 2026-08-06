package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日系统步数与步数消耗估算。
 */
@Entity(tableName = "daily_step_count")
data class DailyStepCount(
    @PrimaryKey
    val date: String,               // "yyyy-MM-dd"
    val steps: Int,
    val caloriesKcal: Int
)
