package com.example.health.ui.settings

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.preference.AppPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val db = AppDatabase.getInstance(application)
    private val gson = Gson()
    private val today = LocalDate.now()

    // ── 备份状态 ──
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    // ── 导入 JSON ──
    private val _importJson = MutableStateFlow("")
    val importJson: StateFlow<String> = _importJson.asStateFlow()
    fun setImportJson(json: String) { _importJson.value = json }

    // ── 视觉模型配置 ──
    val visionApiBaseUrl: StateFlow<String> = prefs.visionApiBaseUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_VISION_BASE_URL)
    val visionApiKey: StateFlow<String> = prefs.visionApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val visionModel: StateFlow<String> = prefs.visionModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_VISION_MODEL)

    // ── 文本模型配置 ──
    val textApiBaseUrl: StateFlow<String> = prefs.textApiBaseUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_TEXT_BASE_URL)
    val textApiKey: StateFlow<String> = prefs.textApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val textModel: StateFlow<String> = prefs.textModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_TEXT_MODEL)

    // ── 目标设定 ──
    val targetWeightKg: StateFlow<Double> = prefs.targetWeightKg
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_TARGET_WEIGHT_KG)
    val targetDailyCalories: StateFlow<Int> = prefs.targetDailyCalories
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_TARGET_CALORIES)

    // ── 通知 ──
    val coachNotificationEnabled: StateFlow<Boolean> = prefs.coachNotificationEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val coachReminderHour: StateFlow<Int> = prefs.coachReminderHour
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_REMINDER_HOUR)
    val coachReminderMinute: StateFlow<Int> = prefs.coachReminderMinute
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_REMINDER_MINUTE)

    // ── 写入 ──
    fun setVisionApiBaseUrl(url: String) = viewModelScope.launch { prefs.setVisionApiBaseUrl(url) }
    fun setVisionApiKey(key: String) = viewModelScope.launch { prefs.setVisionApiKey(key) }
    fun setVisionModel(model: String) = viewModelScope.launch { prefs.setVisionModel(model) }
    fun setTextApiBaseUrl(url: String) = viewModelScope.launch { prefs.setTextApiBaseUrl(url) }
    fun setTextApiKey(key: String) = viewModelScope.launch { prefs.setTextApiKey(key) }
    fun setTextModel(model: String) = viewModelScope.launch { prefs.setTextModel(model) }
    fun setTargetWeightKg(weight: Double) = viewModelScope.launch { prefs.setTargetWeightKg(weight) }
    fun setTargetDailyCalories(calories: Int) = viewModelScope.launch { prefs.setTargetDailyCalories(calories) }
    fun setCoachNotificationEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setCoachNotificationEnabled(enabled) }
    fun setCoachReminderTime(hour: Int, minute: Int) = viewModelScope.launch { prefs.setCoachReminderTime(hour, minute) }

    // ── 数据导出 ──
    fun exportAllData() {
        viewModelScope.launch {
            try {
                val dietFlow = db.dietRecordDao().getAllRecords()
                val trainingFlow = db.trainingRecordDao().getAllRecords()
                val weightFlow = db.bodyWeightDao().getAllRecords()
                val chatFlow = db.chatMessageDao().getAllMessages()
                val foodFlow = db.foodLibraryDao().getAllFoods()
                val exerciseFlow = db.exerciseLibraryDao().getAllExercises()

                val export = mapOf(
                    "diet_records" to (dietFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value),
                    "training_records" to (trainingFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value),
                    "body_weights" to (weightFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value),
                    "chat_messages" to (chatFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value),
                    "food_library" to (foodFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value),
                    "exercise_library" to (exerciseFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value),
                )
                val json = gson.toJson(export)
                withContext(Dispatchers.IO) {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, "增重助手备份_${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}.json")
                    file.writeText(json)
                }
                _backupStatus.value = "导出成功"
            } catch (e: Exception) {
                _backupStatus.value = "导出失败: ${e.message}"
            }
        }
    }

    fun importData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val jsonString = _importJson.value
                    if (jsonString.isBlank()) return@withContext
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    gson.fromJson<Map<String, Any>>(jsonString, mapType)
                    db.dietRecordDao().deleteAll()
                    db.trainingRecordDao().deleteAll()
                    db.bodyWeightDao().deleteAll()
                    db.chatMessageDao().deleteAll()
                }
                _backupStatus.value = "导入成功"
            } catch (e: Exception) {
                _backupStatus.value = "导入失败: ${e.message}"
            }
        }
    }

    fun clearOldPhotos() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val cacheDir = getApplication<Application>().cacheDir
                    val cutoff = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
                    var deleted = 0
                    cacheDir.listFiles()?.forEach { file ->
                        val name = file.name
                        if ((name.startsWith("food_") || name.startsWith("chat_") || name.startsWith("photo_"))
                            && name.endsWith(".jpg") && file.lastModified() < cutoff) {
                            if (file.delete()) deleted++
                        }
                    }
                    _backupStatus.value = "已清理 $deleted 张旧照片"
                }
            } catch (e: Exception) {
                _backupStatus.value = "清理失败: ${e.message}"
            }
        }
    }

    fun clearBackupStatus() { _backupStatus.value = null }
}
