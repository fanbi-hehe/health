package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.health.data.local.entity.TrainingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TrainingRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<TrainingRecord>)

    @Delete
    suspend fun delete(record: TrainingRecord)

    @Query("SELECT * FROM training_record ORDER BY date DESC, id DESC")
    fun getAllRecords(): Flow<List<TrainingRecord>>

    @Query("SELECT * FROM training_record WHERE date = :date ORDER BY id DESC")
    suspend fun getRecordsByDate(date: String): List<TrainingRecord>

    /**
     * 根据动作名模糊搜索历史记录，返回最近的一组（用于历史动作推荐）。
     */
    @Query("SELECT DISTINCT exerciseName FROM training_record WHERE bodyParts LIKE '%' || :bodyPart || '%' ORDER BY id DESC")
    suspend fun getDistinctExercisesByBodyPart(bodyPart: String): List<String>

    /**
     * 查询指定动作名称的历史记录（按日期降序，用于对话Agent精准查询）。
     */
    @Query("SELECT * FROM training_record WHERE exerciseName LIKE '%' || :exerciseName || '%' ORDER BY date DESC")
    suspend fun getRecordsByExerciseName(exerciseName: String): List<TrainingRecord>

    /**
     * 获取最近 N 条指定动作的训练记录。
     */
    @Query("SELECT * FROM training_record WHERE exerciseName LIKE '%' || :exerciseName || '%' ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentRecordsByExercise(exerciseName: String, limit: Int): List<TrainingRecord>

    @Query("DELETE FROM training_record")
    suspend fun deleteAll()
}
