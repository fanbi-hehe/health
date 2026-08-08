package com.example.health.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.AdviceLog
import com.example.health.data.local.entity.BodyWeight
import com.example.health.data.local.entity.DailyStepCount
import com.example.health.data.preference.AppPreferences
import com.example.health.data.repository.AiRepository
import com.example.health.domain.calorie.CalorieCalculator
import com.example.health.data.remote.dto.RecognizedFood
import com.example.health.util.StepCounterManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val prefs = AppPreferences(application)
    private val backupRepo = com.example.health.data.repository.BackupRepository(application)
    private val aiRepo = AiRepository(application)
    // ── 体重记录 ──
    val weightRecords: StateFlow<List<BodyWeight>> = db.bodyWeightDao().getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 每日步数 ──
    val stepCounts: StateFlow<List<DailyStepCount>> = db.dailyStepCountDao().getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 今日运动消耗 / 步数消耗 / BMR ──
    private val _todayActivityCalories = MutableStateFlow(0)
    val todayActivityCalories: StateFlow<Int> = _todayActivityCalories.asStateFlow()

    private val _todayStepCalories = MutableStateFlow(0)
    val todayStepCalories: StateFlow<Int> = _todayStepCalories.asStateFlow()

    data class TodayMacros(
        val proteinG: Int = 0,
        val carbsG: Int = 0,
        val fatG: Int = 0
    )

    private val _todayMacros = MutableStateFlow(TodayMacros())
    val todayMacros: StateFlow<TodayMacros> = _todayMacros.asStateFlow()

    init {
        viewModelScope.launch {
            db.activityRecordDao().getAllRecords().collect { list ->
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                _todayActivityCalories.value = list
                    .filter { sdf.format(java.util.Date(it.startTime)) == todayStr }
                    .sumOf { it.caloriesKcal }
            }
        }
        viewModelScope.launch {
            stepCounts.collect { list ->
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                _todayStepCalories.value = list.firstOrNull { it.date == todayStr }?.caloriesKcal ?: 0
            }
        }
        viewModelScope.launch {
            db.dietRecordDao().getAllRecords().collect { list ->
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val todayRecords = list.filter { sdf.format(java.util.Date(it.timestamp)) == todayStr }
                _todayMacros.value = TodayMacros(
                    proteinG = todayRecords.sumOf { it.proteinG },
                    carbsG = todayRecords.sumOf { it.carbsG },
                    fatG = todayRecords.sumOf { it.fatG }
                )
            }
        }
    }

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
    val userAge: StateFlow<Int> = prefs.userAge.stateIn(viewModelScope, SharingStarted.Eagerly, 25)
    val userGender: StateFlow<String> = prefs.userGender.stateIn(viewModelScope, SharingStarted.Eagerly, "男")
    val userGoal: StateFlow<String> = prefs.userGoal.stateIn(viewModelScope, SharingStarted.Eagerly, "增重增肌")
    val userExperience: StateFlow<String> = prefs.userExperience.stateIn(viewModelScope, SharingStarted.Eagerly, "新手")
    val userEquipment: StateFlow<String> = prefs.userEquipment.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val userTrainingDays: StateFlow<Int> = prefs.userTrainingDays.stateIn(viewModelScope, SharingStarted.Eagerly, 4)

    /** 基础代谢（Mifflin-St Jeor），档案变化时自动重算。 */
    val bmr: StateFlow<Int> = combine(
        userHeight, userWeight, userAge, userGender
    ) { height, weight, age, gender ->
        CalorieCalculator.bmr(weight, height, age, gender).roundToInt()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun saveUserProfile(
        h: Int,
        w: Double,
        g: String,
        e: String,
        eq: String,
        d: Int,
        age: Int = 25,
        gender: String = "男"
    ) {
        viewModelScope.launch { prefs.setUserProfile(h, w, g, e, eq, d, age, gender) }
    }

    // ── 备份状态 ──
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    // ── AI 每日评估 ──
    val adviceLogs: StateFlow<List<AdviceLog>> = db.adviceLogDao().getAllLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Idle)
    val reviewState: StateFlow<ReviewState> = _reviewState.asStateFlow()

    fun clearReviewState() {
        _reviewState.value = ReviewState.Idle
    }

    /**
     * 生成今日综合评估：
     * 本地组装今日饮食/训练 + 近3天摄入 + 体重趋势 → 调用文本模型 → 存入 AdviceLog。
     */
    fun generateDailyReview() {
        viewModelScope.launch {
            _reviewState.value = ReviewState.Generating
            try {
                val contextText = withContext(Dispatchers.IO) { buildDailyReviewContext() }
                val result = aiRepo.chatCompletion(
                    userText = "请根据以上用户今日数据进行综合评估：先点出做得好的地方，再指出需要改进的地方，最后给出明天可执行的具体建议。",
                    imageFile = null,
                    history = emptyList(),
                    systemPrompt = "你是一位专业的私人健康与体能管家（CSCS认证级别）。" +
                        "根据用户提供的数据和问题，给出个性化、具体、可行的建议。" +
                        "回答简洁有力，控制在200字以内。\n\n## 用户今日数据\n$contextText"
                )
                result.fold(
                    onSuccess = { reply ->
                        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        db.adviceLogDao().insert(
                            AdviceLog(
                                date = todayStr,
                                requestSnapshot = contextText,
                                aiResponse = reply
                            )
                        )
                        _reviewState.value = ReviewState.Success(reply)
                    },
                    onFailure = { e ->
                        _reviewState.value = ReviewState.Error(e.message ?: "评估失败，请检查 API 配置")
                    }
                )
            } catch (e: Exception) {
                _reviewState.value = ReviewState.Error(e.message ?: "评估失败")
            }
        }
    }

    /** 组装每日评估上下文：今日明细 + 近3天摄入 + 体重趋势（仅统计值）。 */
    private suspend fun buildDailyReviewContext(): String {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val todayStr = LocalDate.now().format(fmt)
        val sb = StringBuilder()

        // ── 今日饮食（明细） ──
        val dietRecords = db.dietRecordDao().getRecordsByDate(todayStr)
        val dietCal = db.dietRecordDao().getTotalCaloriesByDate(todayStr) ?: 0
        sb.appendLine("【今日饮食】")
        if (dietRecords.isEmpty()) {
            sb.appendLine("- 无记录")
        } else {
            dietRecords.forEach {
                sb.appendLine("- ${it.mealType} ${it.foodName} ${it.weightG}g ${it.caloriesKcal}kcal")
            }
        }
        sb.appendLine("- 合计：$dietCal kcal")
        val targetCal = prefs.targetDailyCalories.first()
        sb.appendLine("- 每日目标：$targetCal kcal（差额 ${targetCal - dietCal} kcal）")

        // ── 今日训练（明细） ──
        val trainingRecords = db.trainingRecordDao().getRecordsByDate(todayStr)
        sb.appendLine("【今日训练】")
        if (trainingRecords.isEmpty()) {
            sb.appendLine("- 无记录")
        } else {
            trainingRecords.forEach {
                sb.appendLine("- ${it.exerciseName} ${it.sets}组×${it.reps}次 ${it.weightKg}kg")
            }
        }

        // ── 近3天摄入（统计） ──
        sb.appendLine("【近3天摄入】")
        for (i in 0..2) {
            val d = LocalDate.now().minusDays(i.toLong()).format(fmt)
            val cal = db.dietRecordDao().getTotalCaloriesByDate(d) ?: 0
            sb.appendLine("- $d: $cal kcal")
        }

        // ── 今日运动与步数 ──
        val activityRecords = db.activityRecordDao().getRecordsByDate(todayStr)
        val activityCal = activityRecords.sumOf { it.caloriesKcal }
        sb.appendLine("【今日运动】")
        if (activityRecords.isEmpty()) {
            sb.appendLine("- 无运动记录")
        } else {
            activityRecords.forEach {
                val dist = if (it.distanceMeters > 0) {
                    "，${"%.2f".format(it.distanceMeters / 1000)} km"
                } else ""
                sb.appendLine("- ${it.typeLabel()} ${it.durationMinutes} 分钟$dist，约 ${it.caloriesKcal} kcal")
            }
        }
        sb.appendLine("- 运动消耗合计：$activityCal kcal")

        val stepToday = db.dailyStepCountDao().getByDate(todayStr)
        val stepCal = stepToday?.caloriesKcal ?: 0
        if (stepToday != null && stepToday.steps > 0) {
            sb.appendLine("- 今日步数：${stepToday.steps} 步（约 $stepCal kcal）")
        }

        // ── 净摄入与缺口 ──
        val netIntake = dietCal - activityCal - stepCal
        sb.appendLine("【净摄入】$dietCal − $activityCal（运动） − $stepCal（步数） = $netIntake kcal")
        sb.appendLine("- 目标 $targetCal kcal，缺口 ${targetCal - netIntake} kcal（正=还差，负=超出）")
        // 今日宏量：数据库已有 + 缺失项由语言模型估算（仅用于总结，不写库）
        val missingMacros = dietRecords.filter {
            it.proteinG == 0 && it.carbsG == 0 && it.fatG == 0
        }
        val estimatedMacros = if (missingMacros.isNotEmpty()) {
            aiRepo.estimateMacros(
                missingMacros.map { RecognizedFood(it.foodName, it.weightG, it.caloriesKcal) }
            )
        } else {
            emptyList()
        }
        var missingIdx = 0
        val proteinTotal = dietRecords.sumOf { r ->
            if (r.proteinG == 0 && r.carbsG == 0 && r.fatG == 0) {
                estimatedMacros.getOrNull(missingIdx++)?.proteinG ?: 0
            } else {
                r.proteinG
            }
        }
        missingIdx = 0
        val carbsTotal = dietRecords.sumOf { r ->
            if (r.proteinG == 0 && r.carbsG == 0 && r.fatG == 0) {
                estimatedMacros.getOrNull(missingIdx++)?.carbsG ?: 0
            } else {
                r.carbsG
            }
        }
        missingIdx = 0
        val fatTotal = dietRecords.sumOf { r ->
            if (r.proteinG == 0 && r.carbsG == 0 && r.fatG == 0) {
                estimatedMacros.getOrNull(missingIdx++)?.fatG ?: 0
            } else {
                r.fatG
            }
        }
        sb.appendLine("- 今日宏量：蛋白质 ${proteinTotal}g · 碳水 ${carbsTotal}g · 脂肪 ${fatTotal}g")

        // ── 体重趋势（统计值，不传原始数据） ──
        val thirtyDaysAgo = LocalDate.now().minusDays(29).format(fmt)
        val avgWeight = db.bodyWeightDao().getAverageWeightBetween(thirtyDaysAgo, todayStr)
        val latestWeight = db.bodyWeightDao().getLatestWeight()
        if (avgWeight != null && latestWeight != null) {
            val change = latestWeight - avgWeight
            val sign = if (change >= 0) "+" else ""
            sb.appendLine("【30天体重】平均 ${"%.1f".format(avgWeight)} kg，最新 $latestWeight kg，较平均${sign}${"%.1f".format(change)} kg")
        } else if (latestWeight != null) {
            sb.appendLine("【体重】最新 $latestWeight kg")
        } else {
            sb.appendLine("【体重】暂无记录")
        }

        return sb.toString()
    }

    private fun com.example.health.data.local.entity.ActivityRecord.typeLabel(): String = when (type) {
        "running" -> "跑步"
        "cycling" -> "骑行"
        "walking" -> "步行"
        "manual" -> "手动补录"
        else -> "运动"
    }

    // ── 今日摄入 ──
    val todayCalories: StateFlow<Int> = db.dietRecordDao().getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        .let { flow ->
            val result = MutableStateFlow(0)
            viewModelScope.launch {
                flow.collect { records ->
                    // 动态取当天日期，避免跨天后仍显示昨天的摄入
                    val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
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
            // 动态取当天日期，跨天后体重不再记到昨天
            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
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
                    if (result.activityCount > 0) append("、运动${result.activityCount}条")
                    if (result.stepCount > 0) append("、步数${result.stepCount}天")
                    if (result.adviceCount > 0) append("、评估${result.adviceCount}条")
                    if (result.templateCount > 0) append("、模板${result.templateCount}个")
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
                    // 近7天（含今天，动态取日期，跨天窗口自动滑动）
                    val days = (0..6).map { LocalDate.now().minusDays(it.toLong()) }.reversed()
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

    /** 即时同步系统步数（传感器 → 每日步数表）。 */
    fun syncSteps() {
        viewModelScope.launch {
            StepCounterManager.syncNow(getApplication())
        }
    }
}

// ── AI 每日评估状态 ──
sealed class ReviewState {
    data object Idle : ReviewState()
    data object Generating : ReviewState()
    data class Success(val response: String) : ReviewState()
    data class Error(val message: String) : ReviewState()
}
