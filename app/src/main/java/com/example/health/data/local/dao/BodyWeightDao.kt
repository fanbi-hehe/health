package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.health.data.local.entity.BodyWeight
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BodyWeight): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BodyWeight>)

    @Query("SELECT * FROM body_weight ORDER BY date DESC")
    fun getAllRecords(): Flow<List<BodyWeight>>

    @Query("SELECT * FROM body_weight ORDER BY date DESC")
    suspend fun getAllRecordsOnce(): List<BodyWeight>

    @Query("SELECT * FROM body_weight WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getRecordsBetweenDates(startDate: String, endDate: String): Flow<List<BodyWeight>>

    @Query("SELECT * FROM body_weight WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): BodyWeight?

    @Query("SELECT AVG(weightKg) FROM body_weight WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageWeightBetween(startDate: String, endDate: String): Double?

    @Query("SELECT weightKg FROM body_weight ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeight(): Double?

    @Query("DELETE FROM body_weight")
    suspend fun deleteAll()
}
