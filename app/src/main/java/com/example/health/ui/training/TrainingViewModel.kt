package com.example.health.ui.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ExerciseLibrary
import com.example.health.data.local.entity.TrainingRecord
import com.example.health.data.preference.AppPreferences
import com.example.health.data.repository.AiRepository
import com.example.health.data.repository.ExerciseRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).trainingRecordDao()
    private val exerciseRepo = ExerciseRepository(application)
    private val prefs = AppPreferences(application)
    private val aiRepo = AiRepository(application)
    private val gson = Gson()

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

    // ── 训练计划 ──
    val planJson: StateFlow<String> = prefs.trainingPlanJson
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // ── 用户档案 ──
    val isOnboarded: StateFlow<Boolean> = prefs.userOnboarded
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun saveUserProfile(height: Int, weight: Double, goal: String, experience: String, equipment: String, days: Int) {
        viewModelScope.launch { prefs.setUserProfile(height, weight, goal, experience, equipment, days) }
    }

    fun generatePlan() {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val height: Int = prefs.userHeight.first()
                val weight: Double = prefs.userCurrentWeight.first()
                val goal: String = prefs.userGoal.first()
                val experience: String = prefs.userExperience.first()
                val equipment: String = prefs.userEquipment.first()
                val trainingDays: Int = prefs.userTrainingDays.first()

                val exercises = exerciseRepo.getAllExercises()
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value
                val history = dao.getAllRecords()
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value

                val exerciseNames = exercises.take(60).joinToString("、") { it.name }
                val recentTraining = history.take(15).joinToString("\n") {
                    "${it.date} ${it.bodyParts} ${it.exerciseName} ${it.sets}×${it.reps} ${it.weightKg}kg"
                }
                val prompt = buildString {
                    append("你是一位专业健身教练。请根据以下用户信息制定7天训练计划。\n\n")
                    append("用户档案：\n")
                    append("- 身高: ${height}cm\n")
                    append("- 体重: ${weight}kg\n")
                    append("- 目标: $goal\n")
                    append("- 训练经验: $experience\n")
                    append("- 可用器材: ${equipment.ifBlank { "哑铃、杠铃、自重" }}\n")
                    append("- 每周训练天数: $trainingDays\n\n")
                    if (recentTraining.isNotBlank()) {
                        append("最近训练记录：\n$recentTraining\n\n")
                    }
                    append("可选动作库：$exerciseNames\n\n")
                    append("要求：返回纯JSON数组，每天一个对象，7天（周一到周日）。\n")
                    append("格式：[{\"day\":\"周一\",\"date\":\"日期\",\"focus\":\"训练部位\",\"exercises\":")
                    append("[{\"name\":\"动作名\",\"sets\":3,\"reps\":\"8-12\",\"notes\":\"备注\"}]}]\n")
                    append("休息日 focus 写\"休息日\"，exercises 为空数组。\n")
                    append("合理分配部位，符合每周${trainingDays}天训练的安排。只返回JSON！")
                }

                val result = aiRepo.chatCompletion(prompt, null, emptyList())
                result.fold(
                    onSuccess = { json ->
                        val cleaned = json.replace("```json", "").replace("```", "").trim()
                        prefs.setTrainingPlanJson(cleaned)
                    },
                    onFailure = { e -> /* keep old plan */ }
                )
            } catch (_: Exception) {}
            _isGenerating.value = false
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

    fun updateRecord(id: Long, bodyParts: List<String>, exerciseName: String,
                     sets: Int, reps: Int, weightKg: Double, notes: String?) {
        viewModelScope.launch {
            dao.insert(TrainingRecord(id = id, date = todayDate,
                bodyParts = bodyParts.joinToString(","), exerciseName = exerciseName,
                sets = sets, reps = reps, weightKg = weightKg, notes = notes))
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
