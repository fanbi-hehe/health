package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diet_record")
data class DietRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodName: String,
    val weightG: Int,
    val caloriesKcal: Int,
    val mealType: String,       // 早餐/午餐/晚餐/加餐
    val timestamp: Long,        // System.currentTimeMillis()
    val imagePath: String? = null  // 拍照图片路径，手动录入可为空
)