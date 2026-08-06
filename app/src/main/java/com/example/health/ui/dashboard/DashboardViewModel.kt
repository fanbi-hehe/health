package com.example.health.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.BodyWeight
import com.example.health.data.preference.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val prefs = AppPreferences(application)
    private val backupRepo = com.example.health.data.repository.BackupRepository(application)
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
                val fileName = backupRepo.exportAll()
                _backupStatus.value = "导出成功: $fileName"
            } catch (e: Exception) {
                _backupStatus.value = "导出失败: ${e.message}"
            }
        }
    }

    // ── 数据导入 ──
    fun importData(jsonString: String) {
        viewModelScope.launch {
            try {
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

    // ── 近7天每日摄入（用于柱状图） ──
    data class DailyCalorie(
        val date: String,       // "MM-dd"
        val dateFull: String,   // "yyyy-MM-dd"
        val calories: Int
    )

    val sevenDayCalories: StateFlow<List<DailyCalorie>> = db.dietRecordDao().getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        .let { flow ->
            val result = MutableStateFlow<List<DailyCalorie>>(emptyList())
            viewModelScope.launch {
                flow.collect { records ->
                    val fmt = DateTimeFormatter.ofPattern("MM-dd")
                    val fullFmt = DateTimeFormatter.ISO_LOCAL_DATE
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    // 近7天（含今天）
                    val days = (0..6).map { today.minusDays(it.toLong()) }.reversed()
                    result.value = days.map { day ->
                        val dateStr = day.format(fullFmt)
                        val cals = records
                            .filter { sdf.format(java.util.Date(it.timestamp)) == dateStr }
                            .sumOf { it.caloriesKcal }
                        DailyCalorie(
                            date = day.format(fmt),
                            dateFull = dateStr,
                            calories = cals
                        )
                    }
                }
            }
            result.asStateFlow()
        }

    fun clearBackupStatus() { _backupStatus.value = null }
}
