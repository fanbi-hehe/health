package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM diet_record WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = :date ORDER BY timestamp DESC")
    suspend fun getRecordsByDate(date: String): List<DietRecord>

    @Query("SELECT SUM(caloriesKcal) FROM diet_record WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = :date")
    suspend fun getTotalCaloriesByDate(date: String): Int?

    @Query("SELECT * FROM diet_record WHERE date(timestamp / 1000, 'unixepoch', 'localtime') BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getRecordsBetweenDates(startDate: String, endDate: String): List<DietRecord>

    @Query("SELECT * FROM diet_record WHERE id = :id")
    suspend fun getRecordById(id: Long): DietRecord?

    @Query("SELECT * FROM diet_record ORDER BY timestamp DESC")
    suspend fun getAllRecordsOnce(): List<DietRecord>

    @Update
    suspend fun update(record: DietRecord)

    /**
     * 查询两个日期之间的总热量（SUM），用于多日统计。
     * @param startDate 较早日期 "yyyy-MM-dd"
     * @param endDate   较晚日期 "yyyy-MM-dd"
     */
    @Query("SELECT SUM(caloriesKcal) FROM diet_record WHERE date(timestamp / 1000, 'unixepoch', 'localtime') BETWEEN :startDate AND :endDate")
    suspend fun getTotalCaloriesBetweenDates(startDate: String, endDate: String): Int?

    /**
     * 获取最近有饮食记录的 N 个日期（去重、降序），供意图路由区分"有数据的天"。
     */
    @Query("SELECT DISTINCT date(timestamp / 1000, 'unixepoch', 'localtime') AS recordDate FROM diet_record ORDER BY recordDate DESC LIMIT :limit")
    suspend fun getRecentDatesWithRecords(limit: Int): List<String>

    @Query("DELETE FROM diet_record")
    suspend fun deleteAll()
}
