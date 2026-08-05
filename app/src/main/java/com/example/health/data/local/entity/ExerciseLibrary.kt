package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_library")
data class ExerciseLibrary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bodyPart: String,       // 主要训练部位：胸/背/腿/肩/手臂/核心
    val isCustom: Boolean = true // true=用户自定义, false=内置
)
