package com.example.health.ui.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ExerciseLibrary
import com.example.health.data.local.entity.TrainingRecord
import com.example.health.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).trainingRecordDao()
    private val exerciseRepo = ExerciseRepository(application)

    private val todayDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    // ── 今日训练记录 ──
    val todayRecords: StateFlow<List<TrainingRecord>> = dao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 动作库 ──
    val allExercises: StateFlow<List<ExerciseLibrary>> = exerciseRepo.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 按部位筛选的历史动作名 ──
    private val _historyExercises = MutableStateFlow<List<String>>(emptyList())
    val historyExercises: StateFlow<List<String>> = _historyExercises.asStateFlow()

    /**
     * 选择部位后查询该部位的历史动作。
     */
    fun loadHistoryExercises(bodyPart: String) {
        viewModelScope.launch {
            _historyExercises.value = dao.getDistinctExercisesByBodyPart(bodyPart)
        }
    }

    fun clearHistoryExercises() {
        _historyExercises.value = emptyList()
    }

    /**
     * 保存一条训练记录。
     */
    fun saveRecord(
        bodyParts: List<String>,
        exerciseName: String,
        sets: Int,
        reps: Int,
        weightKg: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            dao.insert(
                TrainingRecord(
                    date = todayDate,
                    bodyParts = bodyParts.joinToString(","),
                    exerciseName = exerciseName,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    notes = notes
                )
            )
        }
    }

    /**
     * 删除训练记录。
     */
    fun deleteRecord(record: TrainingRecord) {
        viewModelScope.launch {
            dao.delete(record)
        }
    }

    /**
     * 按动作名查找 ExerciseLibrary 详情（供详情页使用）。
     */
    suspend fun getExerciseByName(name: String): ExerciseLibrary? {
        return exerciseRepo.getAllExercises()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
            .value
            .firstOrNull { it.name == name }
    }
}
