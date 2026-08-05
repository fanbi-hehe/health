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

    // ── 模型服务配置 ──
    val apiBaseUrl: StateFlow<String> = prefs.apiBaseUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_API_BASE_URL)

    val apiKey: StateFlow<String> = prefs.apiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val textModel: StateFlow<String> = prefs.textModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_TEXT_MODEL)

    val visionModel: StateFlow<String> = prefs.visionModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences.DEFAULT_VISION_MODEL)

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

    // ── 写入方法 ──
    fun setApiBaseUrl(url: String) = viewModelScope.launch { prefs.setApiBaseUrl(url) }
    fun setApiKey(key: String) = viewModelScope.launch { prefs.setApiKey(key) }
    fun setTextModel(model: String) = viewModelScope.launch { prefs.setTextModel(model) }
    fun setVisionModel(model: String) = viewModelScope.launch { prefs.setVisionModel(model) }
    fun setTargetWeightKg(weight: Double) = viewModelScope.launch { prefs.setTargetWeightKg(weight) }
    fun setTargetDailyCalories(calories: Int) = viewModelScope.launch { prefs.setTargetDailyCalories(calories) }
    fun setCoachNotificationEnabled(enabled: Boolean) = viewModelScope.launch {
        prefs.setCoachNotificationEnabled(enabled)
    }
    fun setCoachReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        prefs.setCoachReminderTime(hour, minute)
    }
}
