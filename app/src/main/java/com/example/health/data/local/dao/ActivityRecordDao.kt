package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.health.data.local.entity.ActivityRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ActivityRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ActivityRecord>)

    @Delete
    suspend fun delete(record: ActivityRecord)

    @Query("SELECT * FROM activity_record ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_record ORDER BY startTime DESC")
    suspend fun getAllRecordsOnce(): List<ActivityRecord>

    @Query("SELECT * FROM activity_record WHERE date(startTime / 1000, 'unixepoch', 'localtime') = :date ORDER BY startTime DESC")
    suspend fun getRecordsByDate(date: String): List<ActivityRecord>

    @Query("SELECT * FROM activity_record WHERE date(startTime / 1000, 'unixepoch', 'localtime') BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    suspend fun getRecordsBetweenDates(startDate: String, endDate: String): List<ActivityRecord>

    @Query("SELECT COALESCE(SUM(caloriesKcal), 0) FROM activity_record WHERE date(startTime / 1000, 'unixepoch', 'localtime') = :date")
    suspend fun getTotalCaloriesByDate(date: String): Int

    @Query("SELECT COALESCE(SUM(caloriesKcal), 0) FROM activity_record WHERE date(startTime / 1000, 'unixepoch', 'localtime') BETWEEN :startDate AND :endDate")
    suspend fun getTotalCaloriesBetweenDates(startDate: String, endDate: String): Int

    @Query("DELETE FROM activity_record")
    suspend fun deleteAll()
}
