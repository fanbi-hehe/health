package com.example.health.domain.context

import com.example.health.data.local.AppDatabase
import com.example.health.data.preference.AppPreferences
import com.example.health.domain.router.IntentQuery
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 用户上下文构建器。
 *
 * 根据 [IntentQuery] 意图，按需查询 DAO 并将结果格式化为 AI 可读的文本块。
 * 实现长短期记忆分层：
 * - 近3天：详细数据（具体食物名/动作/组数/重量）
 * - 7天以上：仅统计值（平均值/总量/频率）
 */
class UserContextBuilder(
    private val db: AppDatabase,
    private val prefs: AppPreferences
) {
    private val dietDao get() = db.dietRecordDao()
    private val trainingDao get() = db.trainingRecordDao()
    private val weightDao get() = db.bodyWeightDao()
    private val exerciseLibDao get() = db.exerciseLibraryDao()
    private val activityDao get() = db.activityRecordDao()
    private val stepDao get() = db.dailyStepCountDao()

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ── 公共 API ──

    /** 获取当前用户档案文本（目标/体重/训练天数等），每次对话都可用。 */
    suspend fun buildProfileText(): String {
        val goal = prefs.userGoal.first()
        val targetCal = prefs.targetDailyCalories.first()
        val currentWt = prefs.userCurrentWeight.first()
        val targetWt = prefs.targetWeightKg.first()
        val exp = prefs.userExperience.first()
        val days = prefs.userTrainingDays.first()
        val height = prefs.userHeight.first()
        val equipment = prefs.userEquipment.first()

        return buildString {
            appendLine("- 目标：${goal}")
            appendLine("- 每日目标热量：${targetCal} kcal")
            appendLine("- 当前体重：${currentWt} kg  |  目标体重：${targetWt} kg")
            appendLine("- 身高：${height} cm")
            appendLine("- 训练经验：${exp}  |  每周训练：${days} 天")
            if (equipment.isNotBlank()) appendLine("- 可用器械：${equipment}")
        }.trim()
    }

    /**
     * 根据意图查询并返回格式化的上下文数据。
     *
     * @return 格式化文本，如果没有数据则返回空字符串。
     */
    suspend fun buildContextForIntent(intent: IntentQuery): String {
        return when (intent) {
            is IntentQuery.DietCalories -> buildDietContext(intent.timeRange)
            is IntentQuery.ExerciseProgress -> buildExerciseContext(intent.exerciseName)
            IntentQuery.OverallSummary -> buildOverallContext()
            IntentQuery.ActivitySummary -> buildActivityContext()
            IntentQuery.UserProfile -> "" // 已在系统提示中通过 buildProfileText 提供
            IntentQuery.GeneralChat -> ""
        }
    }

    /**
     * 运动/步数上下文：今日运动明细 + 步数 + 近 3 天运动消耗统计。
     */
    private suspend fun buildActivityContext(): String {
        val sb = StringBuilder()
        val today = today()
        val recent3 = (0..2).map { daysAgo(it) }

        // 今日运动记录
        val todayActivities = activityDao.getRecordsByDate(today)
        val todayCal = todayActivities.sumOf { it.caloriesKcal }
        sb.appendLine("【今日运动】")
        if (todayActivities.isEmpty()) {
            sb.appendLine("- 无运动记录")
        } else {
            todayActivities.forEach {
                val dist = if (it.distanceMeters > 0) {
                    "，${"%.2f".format(it.distanceMeters / 1000)} km"
                } else ""
                sb.appendLine("- ${it.typeLabel()} ${it.durationMinutes} 分钟$dist，约 ${it.caloriesKcal} kcal")
            }
        }
        sb.appendLine("- 运动消耗合计：$todayCal kcal")

        // 今日步数
        val steps = stepDao.getByDate(today)
        if (steps != null && steps.steps > 0) {
            sb.appendLine("- 今日步数：${steps.steps} 步，约 ${steps.caloriesKcal} kcal")
        } else {
            sb.appendLine("- 今日步数：暂无数据")
        }

        // 近 3 天运动消耗统计
        val total3 = recent3.sumOf { activityDao.getTotalCaloriesByDate(it) }
        sb.appendLine("- 近 3 天运动消耗合计：$total3 kcal")

        return sb.toString().trim()
    }

    /** 获取用户历史训练过的所有动作名称，供 IntentRouter 使用。 */
    suspend fun getKnownExerciseNames(): List<String> {
        val fromRecords = trainingDao.getDistinctExerciseNames()
        return if (fromRecords.isNotEmpty()) fromRecords
        else exerciseLibDao.getAllExercises().first().map { it.name }
    }

    /**
     * 全量数据注入（不再按意图选择）：
     * 当前时间 + 今日饮食/运动/步数 + 本周训练 + 本月运动明细与饮食统计，全部带日期时间。
     */
    suspend fun buildFullContext(): String {
        val sb = StringBuilder()
        val now = LocalDateTime.now()
        val nowLabel = "${now.year}年${now.monthValue}月${now.dayOfMonth}日 " +
            "${now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)} " +
            "%02d:%02d".format(now.hour, now.minute)
        sb.appendLine("当前时间：$nowLabel")
        sb.appendLine("（以下数据均已标注日期时间；回答今天的问题请只使用今天的数据，历史数据仅作参考。）")

        val todayStr = today()
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateTimeFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        // ── 今日饮食 ──
        val diet = dietDao.getRecordsByDate(todayStr)
        sb.appendLine()
        sb.appendLine("【今日饮食】")
        if (diet.isEmpty()) {
            sb.appendLine("- 无记录")
        } else {
            diet.forEach {
                val t = timeFmt.format(Date(it.timestamp))
                sb.appendLine("- $t ${it.mealType} ${it.foodName} ${it.weightG}g ${it.caloriesKcal}kcal" +
                    "（蛋白${it.proteinG}g 碳水${it.carbsG}g 脂肪${it.fatG}g）")
            }
        }

        // ── 今日运动 / 步数 ──
        val activities = activityDao.getRecordsByDate(todayStr)
        sb.appendLine()
        sb.appendLine("【今日运动】")
        if (activities.isEmpty()) {
            sb.appendLine("- 无记录")
        } else {
            activities.forEach {
                val t = timeFmt.format(Date(it.startTime))
                val dist = if (it.distanceMeters > 0) "，${"%.2f".format(it.distanceMeters / 1000)}km" else ""
                sb.appendLine("- $t ${it.typeLabel()} ${it.durationMinutes}分钟$dist，约${it.caloriesKcal}kcal")
            }
        }
        val stepToday = stepDao.getByDate(todayStr)
        sb.appendLine("- 今日步数：${stepToday?.steps ?: 0} 步（约 ${stepToday?.caloriesKcal ?: 0} kcal）")

        // ── 本周训练（周一到今天） ──
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val weekTrainings = trainingDao.getRecordsBetweenDates(weekStart, todayStr)
        sb.appendLine()
        sb.appendLine("【本周训练】")
        if (weekTrainings.isEmpty()) {
            sb.appendLine("- 无记录")
        } else {
            weekTrainings.groupBy { it.date }.toSortedMap(compareByDescending { it }).forEach { (date, list) ->
                sb.appendLine("- $date（${weekLabel(date)}）")
                list.forEach {
                    val note = it.notes?.takeIf { n -> n.isNotBlank() }?.let { n -> "（$n）" } ?: ""
                    sb.appendLine("  · ${it.exerciseName} ${it.sets}组×${it.reps}次 ${it.weightKg}kg$note")
                }
            }
        }

        // ── 本月运动 ──
        val monthStart = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val monthActivities = activityDao.getRecordsBetweenDates(monthStart, todayStr)
        sb.appendLine()
        sb.appendLine("【本月运动】")
        if (monthActivities.isEmpty()) {
            sb.appendLine("- 无记录")
        } else {
            monthActivities.forEach {
                val t = dateTimeFmt.format(Date(it.startTime))
                sb.appendLine("- $t ${it.typeLabel()} ${it.durationMinutes}分钟，约${it.caloriesKcal}kcal")
            }
        }

        // ── 本月饮食统计 ──
        val monthDiet = dietDao.getRecordsBetweenDates(monthStart, todayStr)
        val monthTotal = monthDiet.sumOf { it.caloriesKcal }
        val monthDays = LocalDate.now().dayOfMonth
        sb.appendLine()
        sb.appendLine("【本月饮食统计】")
        sb.appendLine("- 总摄入 $monthTotal kcal，日均 ${monthTotal / monthDays} kcal（$monthDays 天）")
        sb.appendLine("- 蛋白质 ${monthDiet.sumOf { it.proteinG }}g · 碳水 ${monthDiet.sumOf { it.carbsG }}g · 脂肪 ${monthDiet.sumOf { it.fatG }}g")

        return sb.toString().trim()
    }

    // ── 三大聚合模块 ──

    /**
     * 热量/饮食上下文。
     * 近3天给明细，更早给统计值。
     */
    private suspend fun buildDietContext(timeRange: String): String {
        val dates = when (timeRange) {
            "today" -> listOf(today())
            "yesterday" -> listOf(daysAgo(1))
            "3days" -> (0..2).map { daysAgo(it) }
            "7days" -> (0..6).map { daysAgo(it) }
            else -> (0..2).map { daysAgo(it) }
        }

        val detailDates = if (dates.size <= 3) dates else dates.take(3)
        val summaryDates = if (dates.size > 3) dates.drop(3) else emptyList()

        val sb = StringBuilder()

        // 近3天明细
        for (date in detailDates) {
            val totalCal = dietDao.getTotalCaloriesByDate(date) ?: 0
            val records = dietDao.getRecordsByDate(date)
            val dateLabel = dateLabel(date)
            if (records.isNotEmpty()) {
                val items = records.joinToString("、") {
                    "${it.foodName}(${it.weightG}g,${it.caloriesKcal}kcal)"
                }
                sb.appendLine("- ${dateLabel}：${items}，合计 ${totalCal} kcal")
            } else {
                sb.appendLine("- ${dateLabel}：无记录")
            }
        }

        // 更早的统计值
        if (summaryDates.isNotEmpty()) {
            val firstSummary = summaryDates.first()
            val lastSummary = summaryDates.last()
            val totalInRange = dietDao.getTotalCaloriesBetweenDates(lastSummary, firstSummary)
            val avgCal = if (summaryDates.isNotEmpty()) (totalInRange ?: 0) / summaryDates.size else 0
            sb.appendLine("- ${dateLabel(lastSummary)}至${dateLabel(firstSummary)}：日均约 ${avgCal} kcal（总计 ${totalInRange ?: 0} kcal）")
        }

        val targetCal = prefs.targetDailyCalories.first()
        sb.appendLine("- 每日目标热量：${targetCal} kcal")

        return sb.toString().trim()
    }

    /**
     * 训练动作进度上下文。
     * 展示指定动作的近期训练历史。
     */
    private suspend fun buildExerciseContext(exerciseName: String): String {
        val recentRecords = trainingDao.getRecentRecordsByExercise(exerciseName, limit = 10)

        if (recentRecords.isEmpty()) {
            return "未找到「${exerciseName}」的历史训练记录。"
        }

        val sb = StringBuilder()
        sb.appendLine("「${exerciseName}」近期训练记录：")

        // 近3条详细，其余统计
        val detail = recentRecords.take(3)
        val summary = recentRecords.drop(3)

        for (r in detail) {
            sb.appendLine("- ${r.date}：${r.sets}组×${r.reps}次 @${r.weightKg}kg")
        }

        if (summary.isNotEmpty()) {
            val avgWeight = summary.map { it.weightKg }.average()
            val maxWeight = summary.maxOfOrNull { it.weightKg } ?: 0.0
            val totalSessions = recentRecords.size
            sb.appendLine("- 近${totalSessions}次训练：平均重量 ${String.format("%.1f", avgWeight)} kg，最大重量 ${maxWeight} kg")
        }

        // 趋势判断
        if (recentRecords.size >= 2) {
            val latest = recentRecords.first().weightKg
            val earliest = recentRecords.last().weightKg
            val trend = when {
                latest > earliest -> "↑ 上升趋势"
                latest < earliest -> "↓ 下降趋势"
                else -> "→ 维持不变"
            }
            sb.appendLine("- 重量变化：${earliest}kg → ${latest}kg (${trend})")
        }

        return sb.toString().trim()
    }

    /**
     * 整体趋势上下文：近3天精简摘要 + 30天统计。
     */
    private suspend fun buildOverallContext(): String {
        val sb = StringBuilder()

        // 近3天饮食摘要
        val recent3Dates = (0..2).map { daysAgo(it) }
        val dietSummary = mutableListOf<String>()
        for (date in recent3Dates) {
            val cal = dietDao.getTotalCaloriesByDate(date) ?: 0
            if (cal > 0) dietSummary.add("${dateLabel(date)} ${cal} kcal")
        }
        if (dietSummary.isNotEmpty()) {
            sb.appendLine("【近3天饮食】${dietSummary.joinToString(" | ")}")
        } else {
            sb.appendLine("【近3天饮食】无记录")
        }

        // 近3天训练摘要
        var trainingCount = 0
        for (date in recent3Dates) {
            val records = trainingDao.getRecordsByDate(date)
            if (records.isNotEmpty()) trainingCount++
        }
        sb.appendLine("【近3天训练】${trainingCount}/3 天有训练记录")

        // 30天统计
        val thirtyDaysAgo = daysAgo(29)
        val todayStr = today()

        // 30天平均体重
        val avgWeight30 = weightDao.getAverageWeightBetween(thirtyDaysAgo, todayStr)
        val latestWeight = weightDao.getLatestWeight()
        if (avgWeight30 != null && latestWeight != null) {
            val change = latestWeight - avgWeight30
            val sign = if (change >= 0) "+" else ""
            sb.appendLine("【30天体重】平均 ${String.format("%.1f", avgWeight30)} kg | 最新 ${latestWeight} kg | 变化 ${sign}${String.format("%.1f", change)} kg")
        } else if (latestWeight != null) {
            sb.appendLine("【体重】最新 ${latestWeight} kg")
        }

        // 30天训练频率
        val allTraining30 = trainingDao.getRecordsBetweenDates(thirtyDaysAgo, todayStr)
        val trainingDays30 = allTraining30.map { it.date }.distinct().size
        sb.appendLine("【30天训练】${trainingDays30} 天有训练记录，共 ${allTraining30.size} 组动作")

        val targetCal = prefs.targetDailyCalories.first()
        sb.appendLine("【每日目标热量】${targetCal} kcal")

        return sb.toString().trim()
    }

    // ── 日期工具 ──

    private fun today(): String = LocalDate.now().format(dateFmt)
    private fun daysAgo(n: Int): String = LocalDate.now().minusDays(n.toLong()).format(dateFmt)
    private fun weekLabel(dateStr: String): String {
        return LocalDate.parse(dateStr)
            .dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
            .replace("星期", "周")
    }

    private fun dateLabel(dateStr: String): String {
        val today = today()
        val yesterday = daysAgo(1)
        return when (dateStr) {
            today -> "今天"
            yesterday -> "昨天"
            else -> dateStr
        }
    }

    private fun com.example.health.data.local.entity.ActivityRecord.typeLabel(): String = when (type) {
        "running" -> "跑步"
        "cycling" -> "骑行"
        "walking" -> "步行"
        "manual" -> "手动补录"
        else -> "运动"
    }
}
