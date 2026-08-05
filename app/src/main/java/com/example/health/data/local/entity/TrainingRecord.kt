package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_record")
data class TrainingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,           // "yyyy-MM-dd"
    val bodyParts: String,      // 逗号分隔多选部位，如 "胸,肩"
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val notes: String? = null   // 可选备注（如 RPE 自评）
)