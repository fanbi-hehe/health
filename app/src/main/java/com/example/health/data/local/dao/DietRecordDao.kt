package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.health.data.local.entity.DietRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DietRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DietRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DietRecord>)

    @Delete
    suspend fun delete(record: DietRecord)

    @Query("SELECT * FROM diet_record ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<DietRecord>>

    @Query("SELECT * FROM diet_record WHERE mealType = :mealType ORDER BY timestamp DESC")
    fun getRecordsByMealType(mealType: String): Flow<List<DietRecord>>

    @Query("SELECT * FROM diet_record WHERE date(timestamp / 1000, 'unixepoch') = :date ORDER BY timestamp DESC")
    suspend fun getRecordsByDate(date: String): List<DietRecord>

    @Query("SELECT SUM(caloriesKcal) FROM diet_record WHERE date(timestamp / 1000, 'unixepoch') = :date")
    suspend fun getTotalCaloriesByDate(date: String): Int?

    @Query("SELECT * FROM diet_record WHERE date(timestamp / 1000, 'unixepoch') BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getRecordsBetweenDates(startDate: String, endDate: String): List<DietRecord>

    @Query("DELETE FROM diet_record")
    suspend fun deleteAll()
}
