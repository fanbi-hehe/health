package com.example.health.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppPreferences(private val context: Context) {

    companion object {
        // ── 视觉模型（食物识别） ──
        val VISION_API_BASE_URL = stringPreferencesKey("vision_api_base_url")
        val VISION_API_KEY = stringPreferencesKey("vision_api_key")
        val VISION_MODEL = stringPreferencesKey("vision_model")

        // ── 文本模型（AI 对话） ──
        val TEXT_API_BASE_URL = stringPreferencesKey("text_api_base_url")
        val TEXT_API_KEY = stringPreferencesKey("text_api_key")
        val TEXT_MODEL = stringPreferencesKey("text_model")
        val TAVILY_API_KEY = stringPreferencesKey("tavily_api_key")

        // ── 目标设定 ──
        val TARGET_WEIGHT_KG = doublePreferencesKey("target_weight_kg")
        val TARGET_DAILY_CALORIES = intPreferencesKey("target_daily_calories")

        // ── 用户档案 ──
        val USER_HEIGHT = intPreferencesKey("user_height_cm")
        val USER_CURRENT_WEIGHT = doublePreferencesKey("user_current_weight")
        val USER_AGE = intPreferencesKey("user_age")
        val USER_GENDER = stringPreferencesKey("user_gender")
        val USER_GOAL = stringPreferencesKey("user_goal")
        val USER_EXPERIENCE = stringPreferencesKey("user_experience")
        val USER_EQUIPMENT = stringPreferencesKey("user_equipment")
        val USER_TRAINING_DAYS = intPreferencesKey("user_training_days")
        val USER_ONBOARDED = booleanPreferencesKey("user_onboarded")
        val TRAINING_PLAN_JSON = stringPreferencesKey("training_plan_json")

        // ── AI 对话滚动摘要 / 每日重置 ──
        val CHAT_SUMMARY = stringPreferencesKey("chat_summary")
        val LAST_CHAT_DATE = stringPreferencesKey("last_chat_date")

        // ── 步数基线（轻量档：传感器只给累计值，App 按天记账） ──
        val STEP_BASE_TOTAL = longPreferencesKey("step_base_total")
        val STEP_BASE_DATE = stringPreferencesKey("step_base_date")

        // ── 暴躁语录 ──
        val COACH_QUOTES = stringPreferencesKey("coach_quotes")

        // ── 初始化标记 ──
        val FOODS_INITIALIZED = booleanPreferencesKey("foods_initialized")
        val EXERCISES_INITIALIZED = booleanPreferencesKey("exercises_initialized")

        // ── 通知 ──
        val COACH_NOTIFICATION_ENABLED = booleanPreferencesKey("coach_notification_enabled")
        val COACH_REMINDER_HOUR = intPreferencesKey("coach_reminder_hour")
        val COACH_REMINDER_MINUTE = intPreferencesKey("coach_reminder_minute")

        // ── 默认值 ──
        const val DEFAULT_VISION_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"
        const val DEFAULT_VISION_MODEL = "glm-4v-flash"
        const val DEFAULT_TEXT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"
        const val DEFAULT_TEXT_MODEL = "glm-4-flash"
        const val DEFAULT_TARGET_WEIGHT_KG = 65.0
        const val DEFAULT_TARGET_CALORIES = 2800
        const val DEFAULT_REMINDER_HOUR = 21
        const val DEFAULT_REMINDER_MINUTE = 0
    }

    // ────────── 视觉模型读取 ──────────
    val visionApiBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[VISION_API_BASE_URL] ?: DEFAULT_VISION_BASE_URL
    }
    val visionApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[VISION_API_KEY] ?: ""
    }
    val visionModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[VISION_MODEL] ?: DEFAULT_VISION_MODEL
    }

    // ────────── 文本模型读取 ──────────
    val textApiBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TEXT_API_BASE_URL] ?: DEFAULT_TEXT_BASE_URL
    }
    val textApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TEXT_API_KEY] ?: ""
    }
    val textModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TEXT_MODEL] ?: DEFAULT_TEXT_MODEL
    }
    val tavilyApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TAVILY_API_KEY] ?: ""
    }

    // ────────── 目标 ──────────
    val targetWeightKg: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[TARGET_WEIGHT_KG] ?: DEFAULT_TARGET_WEIGHT_KG
    }
    val targetDailyCalories: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TARGET_DAILY_CALORIES] ?: DEFAULT_TARGET_CALORIES
    }

    // ────────── 通知 ──────────
    val coachNotificationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[COACH_NOTIFICATION_ENABLED] ?: true
    }
    // ────────── 用户档案 ──────────
    val userHeight: Flow<Int> = context.dataStore.data.map { prefs -> prefs[USER_HEIGHT] ?: 170 }
    val userCurrentWeight: Flow<Double> = context.dataStore.data.map { prefs -> prefs[USER_CURRENT_WEIGHT] ?: 65.0 }
    val userAge: Flow<Int> = context.dataStore.data.map { prefs -> prefs[USER_AGE] ?: 25 }
    val userGender: Flow<String> = context.dataStore.data.map { prefs -> prefs[USER_GENDER] ?: "男" }
    val userGoal: Flow<String> = context.dataStore.data.map { prefs -> prefs[USER_GOAL] ?: "增重增肌" }
    val userExperience: Flow<String> = context.dataStore.data.map { prefs -> prefs[USER_EXPERIENCE] ?: "新手" }
    val userEquipment: Flow<String> = context.dataStore.data.map { prefs -> prefs[USER_EQUIPMENT] ?: "" }
    val userTrainingDays: Flow<Int> = context.dataStore.data.map { prefs -> prefs[USER_TRAINING_DAYS] ?: 4 }
    val userOnboarded: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[USER_ONBOARDED] ?: false }
    val trainingPlanJson: Flow<String> = context.dataStore.data.map { prefs -> prefs[TRAINING_PLAN_JSON] ?: "" }
    val chatSummary: Flow<String> = context.dataStore.data.map { prefs -> prefs[CHAT_SUMMARY] ?: "" }
    val lastChatDate: Flow<String> = context.dataStore.data.map { prefs -> prefs[LAST_CHAT_DATE] ?: "" }
    val stepBaseTotal: Flow<Long> = context.dataStore.data.map { prefs -> prefs[STEP_BASE_TOTAL] ?: 0L }
    val stepBaseDate: Flow<String> = context.dataStore.data.map { prefs -> prefs[STEP_BASE_DATE] ?: "" }

    // ────────── 通知 ──────────
    val coachReminderHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COACH_REMINDER_HOUR] ?: DEFAULT_REMINDER_HOUR
    }
    val coachReminderMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COACH_REMINDER_MINUTE] ?: DEFAULT_REMINDER_MINUTE
    }

    // ────────── 暴躁语录 ──────────
    val coachQuotes: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[COACH_QUOTES] ?: ""
    }

    // ────────── 初始化标记 ──────────
    val foodsInitialized: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FOODS_INITIALIZED] ?: false
    }
    val exercisesInitialized: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[EXERCISES_INITIALIZED] ?: false
    }

    // ────────── 写入 ──────────
    suspend fun setVisionApiBaseUrl(url: String) { context.dataStore.edit { it[VISION_API_BASE_URL] = url } }
    suspend fun setVisionApiKey(key: String) { context.dataStore.edit { it[VISION_API_KEY] = key } }
    suspend fun setVisionModel(model: String) { context.dataStore.edit { it[VISION_MODEL] = model } }
    suspend fun setTextApiBaseUrl(url: String) { context.dataStore.edit { it[TEXT_API_BASE_URL] = url } }
    suspend fun setTextApiKey(key: String) { context.dataStore.edit { it[TEXT_API_KEY] = key } }
    suspend fun setTextModel(model: String) { context.dataStore.edit { it[TEXT_MODEL] = model } }
    suspend fun setTavilyApiKey(key: String) { context.dataStore.edit { it[TAVILY_API_KEY] = key } }
    suspend fun setTargetWeightKg(weight: Double) { context.dataStore.edit { it[TARGET_WEIGHT_KG] = weight } }
    suspend fun setTargetDailyCalories(calories: Int) { context.dataStore.edit { it[TARGET_DAILY_CALORIES] = calories } }
    suspend fun setCoachNotificationEnabled(enabled: Boolean) { context.dataStore.edit { it[COACH_NOTIFICATION_ENABLED] = enabled } }
    suspend fun setCoachReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { it[COACH_REMINDER_HOUR] = hour; it[COACH_REMINDER_MINUTE] = minute }
    }
    suspend fun setFoodsInitialized(initialized: Boolean) { context.dataStore.edit { it[FOODS_INITIALIZED] = initialized } }
    suspend fun setExercisesInitialized(initialized: Boolean) { context.dataStore.edit { it[EXERCISES_INITIALIZED] = initialized } }
    suspend fun setCoachQuotes(json: String) { context.dataStore.edit { it[COACH_QUOTES] = json } }

    suspend fun setUserProfile(
        height: Int,
        weight: Double,
        goal: String,
        experience: String,
        equipment: String,
        days: Int,
        age: Int = 25,
        gender: String = "男"
    ) {
        context.dataStore.edit {
            it[USER_HEIGHT] = height; it[USER_CURRENT_WEIGHT] = weight
            it[USER_AGE] = age; it[USER_GENDER] = gender
            it[USER_GOAL] = goal; it[USER_EXPERIENCE] = experience
            it[USER_EQUIPMENT] = equipment; it[USER_TRAINING_DAYS] = days
            it[USER_ONBOARDED] = true
        }
    }
    suspend fun setTrainingPlanJson(json: String) { context.dataStore.edit { it[TRAINING_PLAN_JSON] = json } }
    suspend fun setChatSummary(summary: String) { context.dataStore.edit { it[CHAT_SUMMARY] = summary } }
    suspend fun setLastChatDate(date: String) { context.dataStore.edit { it[LAST_CHAT_DATE] = date } }

    /** 更新步数基线（传感器累计值 + 所属日期）。 */
    suspend fun setStepBase(total: Long, date: String) {
        context.dataStore.edit {
            it[STEP_BASE_TOTAL] = total
            it[STEP_BASE_DATE] = date
        }
    }
}
