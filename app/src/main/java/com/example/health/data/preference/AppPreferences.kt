package com.example.health.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 顶层扩展属性，单例 DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppPreferences(private val context: Context) {

    companion object {
        // ── 模型服务配置 ──
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val TEXT_MODEL = stringPreferencesKey("text_model")
        val VISION_MODEL = stringPreferencesKey("vision_model")

        // ── 目标设定 ──
        val TARGET_WEIGHT_KG = doublePreferencesKey("target_weight_kg")
        val TARGET_DAILY_CALORIES = intPreferencesKey("target_daily_calories")

        // ── 内置食物初始化标记 ──
        val FOODS_INITIALIZED = booleanPreferencesKey("foods_initialized")

        // ── 通知与个性化 ──
        val COACH_NOTIFICATION_ENABLED = booleanPreferencesKey("coach_notification_enabled")
        val COACH_REMINDER_HOUR = intPreferencesKey("coach_reminder_hour")
        val COACH_REMINDER_MINUTE = intPreferencesKey("coach_reminder_minute")

        // ── 默认值 ──
        const val DEFAULT_API_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"
        const val DEFAULT_TEXT_MODEL = "glm-4-flash"
        const val DEFAULT_VISION_MODEL = "glm-4v-flash"
        const val DEFAULT_TARGET_WEIGHT_KG = 65.0
        const val DEFAULT_TARGET_CALORIES = 2800
        const val DEFAULT_REMINDER_HOUR = 21
        const val DEFAULT_REMINDER_MINUTE = 0
    }

    // ── 读取 ──

    val apiBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_BASE_URL] ?: DEFAULT_API_BASE_URL
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_KEY] ?: ""
    }

    val textModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TEXT_MODEL] ?: DEFAULT_TEXT_MODEL
    }

    val visionModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[VISION_MODEL] ?: DEFAULT_VISION_MODEL
    }

    val targetWeightKg: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[TARGET_WEIGHT_KG] ?: DEFAULT_TARGET_WEIGHT_KG
    }

    val targetDailyCalories: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TARGET_DAILY_CALORIES] ?: DEFAULT_TARGET_CALORIES
    }

    val coachNotificationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[COACH_NOTIFICATION_ENABLED] ?: true
    }

    val foodsInitialized: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FOODS_INITIALIZED] ?: false
    }

    val coachReminderHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COACH_REMINDER_HOUR] ?: DEFAULT_REMINDER_HOUR
    }

    val coachReminderMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COACH_REMINDER_MINUTE] ?: DEFAULT_REMINDER_MINUTE
    }

    // ── 写入（挂起函数） ──

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[API_BASE_URL] = url }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun setTextModel(model: String) {
        context.dataStore.edit { it[TEXT_MODEL] = model }
    }

    suspend fun setVisionModel(model: String) {
        context.dataStore.edit { it[VISION_MODEL] = model }
    }

    suspend fun setTargetWeightKg(weight: Double) {
        context.dataStore.edit { it[TARGET_WEIGHT_KG] = weight }
    }

    suspend fun setTargetDailyCalories(calories: Int) {
        context.dataStore.edit { it[TARGET_DAILY_CALORIES] = calories }
    }

    suspend fun setCoachNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[COACH_NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setCoachReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[COACH_REMINDER_HOUR] = hour
            it[COACH_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setFoodsInitialized(initialized: Boolean) {
        context.dataStore.edit { it[FOODS_INITIALIZED] = initialized }
    }
}
