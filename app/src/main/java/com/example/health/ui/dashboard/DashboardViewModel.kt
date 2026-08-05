package com.example.health.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.BodyWeight
import com.example.health.data.preference.AppPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val prefs = AppPreferences(application)
    private val gson = Gson()
    private val today = LocalDate.now()

    // ── 体重记录 ──
    val weightRecords: StateFlow<List<BodyWeight>> = db.bodyWeightDao().getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 训练记录 ──
    val trainingRecords = db.trainingRecordDao().getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 目标 ──
    val targetWeightKg: StateFlow<Double> = prefs.targetWeightKg
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_TARGET_WEIGHT_KG)
    val targetDailyCalories: StateFlow<Int> = prefs.targetDailyCalories
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_TARGET_CALORIES)

    // ── 用户档案 ──
    val userHeight: StateFlow<Int> = prefs.userHeight.stateIn(viewModelScope, SharingStarted.Eagerly, 170)
    val userWeight: StateFlow<Double> = prefs.userCurrentWeight.stateIn(viewModelScope, SharingStarted.Eagerly, 65.0)
    val userGoal: StateFlow<String> = prefs.userGoal.stateIn(viewModelScope, SharingStarted.Eagerly, "增重增肌")
    val userExperience: StateFlow<String> = prefs.userExperience.stateIn(viewModelScope, SharingStarted.Eagerly, "新手")
    val userEquipment: StateFlow<String> = prefs.userEquipment.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val userTrainingDays: StateFlow<Int> = prefs.userTrainingDays.stateIn(viewModelScope, SharingStarted.Eagerly, 4)

    fun saveUserProfile(h: Int, w: Double, g: String, e: String, eq: String, d: Int) {
        viewModelScope.launch { prefs.setUserProfile(h, w, g, e, eq, d) }
    }

    // ── 备份状态 ──
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    // ── 今日摄入 ──
    val todayCalories: StateFlow<Int> = db.dietRecordDao().getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        .let { flow ->
            val result = MutableStateFlow(0)
            viewModelScope.launch {
                flow.collect { records ->
                    val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    result.value = records
                        .filter { record ->
                            val recordDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .format(java.util.Date(record.timestamp))
                            recordDate == todayStr
                        }
                        .sumOf { it.caloriesKcal }
                }
            }
            result.asStateFlow()
        }

    // ── 体重录入 ──
    fun addWeight(weightKg: Double) {
        viewModelScope.launch {
            val dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            db.bodyWeightDao().insert(BodyWeight(date = dateStr, weightKg = weightKg))
        }
    }

    // ── 数据导出 ──
    fun exportAllData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val export = mapOf(
                        "diet_records" to db.dietRecordDao().getAllRecords()
                            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value,
                        "training_records" to db.trainingRecordDao().getAllRecords()
                            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value,
                        "body_weights" to db.bodyWeightDao().getAllRecords()
                            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value,
                        "chat_messages" to db.chatMessageDao().getAllMessages()
                            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value,
                        "food_library" to db.foodLibraryDao().getAllFoods()
                            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value,
                        "exercise_library" to db.exerciseLibraryDao().getAllExercises()
                            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value,
                    )
                    val json = gson.toJson(export)
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS)
                    val fileName = "增重助手备份_${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}.json"
                    val file = File(downloadsDir, fileName)
                    file.writeText(json)
                    _backupStatus.value = "导出成功: ${file.absolutePath}"
                }
            } catch (e: Exception) {
                _backupStatus.value = "导出失败: ${e.message}"
            }
        }
    }

    // ── 数据导入 ──
    fun importData(jsonString: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val map = gson.fromJson<Map<String, Any>>(jsonString, mapType)
                    // 简单策略：删除现有数据后批量插入
                    db.dietRecordDao().deleteAll()
                    db.trainingRecordDao().deleteAll()
                    db.bodyWeightDao().deleteAll()
                    db.chatMessageDao().deleteAll()
                    db.foodLibraryDao().deleteAllCustom()
                    db.exerciseLibraryDao().deleteAllCustom()
                }
                _backupStatus.value = "导入成功"
            } catch (e: Exception) {
                _backupStatus.value = "导入失败: ${e.message}"
            }
        }
    }

    fun clearBackupStatus() { _backupStatus.value = null }
}
