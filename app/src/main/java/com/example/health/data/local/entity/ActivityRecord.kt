package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 运动/活动记录（GPS 户外运动、心率训练、手动补录）。
 */
@Entity(tableName = "activity_record")
data class ActivityRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,               // "running" / "cycling" / "walking" / "heart_rate" / "manual"
    val startTime: Long,            // 开始时间戳（毫秒）
    val durationMinutes: Int,       // 时长（分钟）
    val avgHeartRate: Int = 0,      // 平均心率（无心率时为 0）
    val maxHeartRate: Int = 0,      // 最大心率
    val caloriesKcal: Int,          // 估算消耗（kcal）
    val distanceMeters: Double = 0.0, // GPS 距离（米）
    val avgPace: String? = null,    // 配速文本（如 "5'30\""）
    val routeJson: String? = null,  // 轨迹点 JSON（地图后置）
    val source: String,             // "gps" / "band_heart_rate" / "manual"
    val note: String? = null        // 备注
)
