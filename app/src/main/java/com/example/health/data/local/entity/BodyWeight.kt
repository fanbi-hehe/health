package com.example.health.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_weight")
data class BodyWeight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,           // "yyyy-MM-dd"
    val weightKg: Double
)