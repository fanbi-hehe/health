package com.example.health.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.health.data.local.entity.ExerciseLibrary
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLibraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseLibrary): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseLibrary>)

    @Update
    suspend fun update(exercise: ExerciseLibrary)

    @Delete
    suspend fun delete(exercise: ExerciseLibrary)

    @Query("SELECT * FROM exercise_library ORDER BY isCustom DESC, name ASC")
    fun getAllExercises(): Flow<List<ExerciseLibrary>>

    @Query("SELECT * FROM exercise_library ORDER BY isCustom DESC, name ASC")
    suspend fun getAllExercisesOnce(): List<ExerciseLibrary>

    @Query("SELECT * FROM exercise_library WHERE name LIKE '%' || :query || '%' ORDER BY isCustom DESC, name ASC")
    fun searchExercises(query: String): Flow<List<ExerciseLibrary>>

    @Query("SELECT * FROM exercise_library WHERE bodyPart = :bodyPart ORDER BY isCustom DESC, name ASC")
    fun getExercisesByBodyPart(bodyPart: String): Flow<List<ExerciseLibrary>>

    @Query("SELECT * FROM exercise_library WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomExercises(): Flow<List<ExerciseLibrary>>

    @Query("DELETE FROM exercise_library WHERE isCustom = 1")
    suspend fun deleteAllCustom()
}
