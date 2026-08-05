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
     * 首次启动时从 assets/exercises.json 导入内置动作到 Room。
     */
    suspend fun initializeBuiltinExercisesIfNeeded() {
        if (prefs.exercisesInitialized.first()) return

        try {
            val json = context.assets.open("exercises.json")
                .bufferedReader()
                .use { it.readText() }

            val listType = object : TypeToken<List<BuiltinExerciseDto>>() {}.type
            val exercises: List<BuiltinExerciseDto> = gson.fromJson(json, listType) ?: emptyList()

            val entities = exercises.map { dto ->
                ExerciseLibrary(
                    name = dto.name,
                    bodyPart = dto.body_part ?: "",
                    equipment = dto.equipment ?: "",
                    muscleGroup = dto.muscle_group ?: "",
                    target = dto.target ?: "",
                    secondaryMuscles = gson.toJson(dto.secondary_muscles ?: emptyList<String>()),
                    instructions = dto.instructions?.zh ?: "",
                    instructionSteps = gson.toJson(dto.instruction_steps?.zh ?: emptyList<String>()),
                    image = dto.image ?: "",
                    gifUrl = dto.gif_url ?: "",
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

    // exercises.json 对应的 DTO
    private data class BuiltinExerciseDto(
        val id: String? = null,
        val name: String = "",
        val category: String? = null,
        val body_part: String? = null,
        val equipment: String? = null,
        val instructions: InstructionDto? = null,
        val instruction_steps: InstructionStepsDto? = null,
        val muscle_group: String? = null,
        val secondary_muscles: List<String>? = null,
        val target: String? = null,
        val media_id: String? = null,
        val image: String? = null,
        val gif_url: String? = null,
        val attribution: String? = null,
        val created_at: String? = null
    )

    private data class InstructionDto(val zh: String? = null)
    private data class InstructionStepsDto(val zh: List<String>? = null)
}
