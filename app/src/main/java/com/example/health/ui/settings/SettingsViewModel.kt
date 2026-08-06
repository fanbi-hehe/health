package com.example.health.ui.settings

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.preference.AppPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
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
    private val backupRepo = com.example.health.data.repository.BackupRepository(application)
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
                val fileName = backupRepo.exportAll()
                _backupStatus.value = "导出成功: $fileName"
            } catch (e: Exception) {
                _backupStatus.value = "导出失败: ${e.message}"
            }
        }
    }

    fun importData() {
        viewModelScope.launch {
            try {
                val jsonString = _importJson.value
                if (jsonString.isBlank()) {
                    _backupStatus.value = "导入失败: JSON 数据为空"
                    return@launch
                }
                val result = backupRepo.importFromJson(jsonString)
                _backupStatus.value = buildString {
                    append("导入成功！")
                    append("饮食${result.dietCount}条、")
                    append("训练${result.trainingCount}条、")
                    append("体重${result.weightCount}条、")
                    append("聊天${result.chatCount}条")
                    if (result.foodCount > 0) append("、食物${result.foodCount}个")
                    if (result.exerciseCount > 0) append("、动作${result.exerciseCount}个")
                }
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

    // ── 暴躁教练语录管理 ──
    private val _quotes = MutableStateFlow<List<String>>(emptyList())
    val quotes: StateFlow<List<String>> = _quotes.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.coachQuotes.collect { json ->
                _quotes.value = parseQuotes(json)
            }
        }
    }

    fun addCoachQuote(quote: String) {
        val updated = _quotes.value.toMutableList()
        updated.add(quote)
        saveQuotes(updated)
    }

    fun deleteCoachQuote(index: Int) {
        val updated = _quotes.value.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            saveQuotes(updated)
        }
    }

    private fun saveQuotes(quotes: List<String>) {
        viewModelScope.launch {
            prefs.setCoachQuotes(gson.toJson(quotes))
        }
    }

    private fun parseQuotes(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, listType)
        } catch (_: Exception) { emptyList() }
    }
}
