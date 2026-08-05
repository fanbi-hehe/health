package com.example.health.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.preference.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)

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
}
