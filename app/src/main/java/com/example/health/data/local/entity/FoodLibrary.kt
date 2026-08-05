package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_library")
data class FoodLibrary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caloriesPer100g: Int,
    val isCustom: Boolean = true   // true=用户自定义, false=内置
)
