package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.health.data.local.entity.MealTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: MealTemplate): Long

    @Delete
    suspend fun delete(template: MealTemplate)

    @Query("SELECT * FROM meal_template ORDER BY templateName ASC")
    fun getAllTemplates(): Flow<List<MealTemplate>>

    @Query("DELETE FROM meal_template")
    suspend fun deleteAll()
}
