package com.example.health.data.repository

import android.content.Context
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ExerciseLibrary
import com.example.health.data.preference.AppPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * 动作库仓库 —— 管理内置动作初始化 + 自定义动作 CRUD + 搜索。
 */
class ExerciseRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).exerciseLibraryDao()
    private val prefs = AppPreferences(context)
    private val gson = Gson()

    fun getAllExercises(): Flow<List<ExerciseLibrary>> = dao.getAllExercises()

    fun searchExercises(query: String): Flow<List<ExerciseLibrary>> = dao.searchExercises(query)

    fun getExercisesByBodyPart(bodyPart: String): Flow<List<ExerciseLibrary>> =
        dao.getExercisesByBodyPart(bodyPart)

    fun getCustomExercises(): Flow<List<ExerciseLibrary>> = dao.getCustomExercises()

    suspend fun insertCustomExercise(name: String, bodyPart: String): Long {
        return dao.insert(ExerciseLibrary(name = name, bodyPart = bodyPart, isCustom = true))
    }

    suspend fun updateExercise(exercise: ExerciseLibrary) = dao.update(exercise)

    suspend fun deleteExercise(exercise: ExerciseLibrary) = dao.delete(exercise)

    /**
     * 首次启动时从 assets/builtin_exercises.json 导入内置动作到 Room。
     */
    suspend fun initializeBuiltinExercisesIfNeeded() {
        if (prefs.exercisesInitialized.first()) return

        try {
            val json = context.assets.open("builtin_exercises.json")
                .bufferedReader()
                .use { it.readText() }

            val listType = object : TypeToken<List<BuiltinExerciseDto>>() {}.type
            val exercises: List<BuiltinExerciseDto> = gson.fromJson(json, listType) ?: emptyList()

            val entities = exercises.map { dto ->
                ExerciseLibrary(
                    name = dto.name,
                    bodyPart = dto.bodyPart,
                    isCustom = false
                )
            }

            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
            }

            prefs.setExercisesInitialized(true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private data class BuiltinExerciseDto(
        val name: String,
        val bodyPart: String,
        val isCustom: Boolean = false
    )
}
