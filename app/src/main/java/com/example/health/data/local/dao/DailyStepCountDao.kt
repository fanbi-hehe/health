package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.health.data.local.entity.DailyStepCount
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepCountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(count: DailyStepCount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(counts: List<DailyStepCount>)

    @Query("SELECT * FROM daily_step_count ORDER BY date DESC")
    suspend fun getAllOnce(): List<DailyStepCount>

    @Query("SELECT * FROM daily_step_count WHERE date = :date")
    suspend fun getByDate(date: String): DailyStepCount?

    @Query("SELECT * FROM daily_step_count ORDER BY date DESC")
    fun getAll(): Flow<List<DailyStepCount>>

    @Query("SELECT * FROM daily_step_count WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getBetweenDates(startDate: String, endDate: String): List<DailyStepCount>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_step_count WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalStepsBetweenDates(startDate: String, endDate: String): Int
}
