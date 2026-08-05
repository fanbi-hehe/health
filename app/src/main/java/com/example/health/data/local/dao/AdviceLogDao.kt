package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.health.data.local.entity.AdviceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AdviceLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AdviceLog): Long

    @Query("SELECT * FROM advice_log ORDER BY date DESC")
    fun getAllLogs(): Flow<List<AdviceLog>>

    @Query("SELECT * FROM advice_log WHERE date = :date LIMIT 1")
    suspend fun getLogByDate(date: String): AdviceLog?

    @Query("DELETE FROM advice_log")
    suspend fun deleteAll()
}
