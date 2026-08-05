package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_template")
data class MealTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateName: String,
    val itemsJson: String    // 存储食物列表的 JSON 字符串
)
