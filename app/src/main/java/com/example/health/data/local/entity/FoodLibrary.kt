package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_library")
data class FoodLibrary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Double = 0.0,   // 每100g 蛋白质（克），0 = 未填写
    val carbsPer100g: Double = 0.0,      // 每100g 碳水化合物（克）
    val fatPer100g: Double = 0.0,        // 每100g 脂肪（克）
    val isCustom: Boolean = true   // true=用户自定义, false=内置
)
