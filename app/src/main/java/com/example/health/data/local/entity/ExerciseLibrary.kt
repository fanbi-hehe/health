package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_library")
data class ExerciseLibrary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bodyPart: String,          // 主要训练部位
    val equipment: String = "",    // 器械
    val muscleGroup: String = "",  // 肌群
    val target: String = "",       // 目标肌
    val secondaryMuscles: String = "",   // JSON 数组：辅助肌群
    val instructions: String = "",       // 完整动作说明
    val instructionSteps: String = "",   // JSON 数组：分步说明
    val image: String = "",              // assets 内图片路径，如 "images/2133-xxx.jpg"
    val gifUrl: String = "",             // assets 内 GIF 路径，如 "videos/2133-xxx.gif"
    val isCustom: Boolean = true         // true=用户自定义, false=内置
)
