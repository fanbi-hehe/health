package com.example.health.domain.plan

import android.content.Context
import com.example.health.data.local.AppDatabase
import com.example.health.data.preference.AppPreferences
import com.example.health.data.repository.AiRepository
import com.example.health.data.repository.ExerciseRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 训练计划生成器：
 * 读取用户档案/训练历史/动作库 → 调用文本模型生成 7 天计划 → JSON 校验 → 保存到 DataStore。
 * AI 失败时自动使用内置 3/4/5 分化标准计划兜底。
 */
class TrainingPlanGenerator(private val context: Context) {

    private val prefs = AppPreferences(context)
    private val aiRepo = AiRepository(context)
    private val exerciseRepo = ExerciseRepository(context)
    private val db = AppDatabase.getInstance(context)
    private val gson = Gson()

    /**
     * 生成并保存 7 天训练计划。
     * @return 成功：给用户看的计划摘要；失败：异常（message 为错误信息）
     */
    suspend fun generate(customPrompt: String = ""): Result<String> {
        return try {
            val height = prefs.userHeight.first()
            val weight = prefs.userCurrentWeight.first()
            val goal = prefs.userGoal.first()
            val experience = prefs.userExperience.first()
            val equipment = prefs.userEquipment.first()
            val trainingDays = prefs.userTrainingDays.first()

            val exercises = exerciseRepo.getAllExercises().first()
            val history = db.trainingRecordDao().getAllRecords().first()

            val prompt = buildPrompt(
                height = height,
                weight = weight,
                goal = goal,
                experience = experience,
                equipment = equipment,
                trainingDays = trainingDays,
                exerciseNames = exercises.take(60).joinToString("、") { it.name },
                recentTraining = history.take(15).joinToString("\n") {
                    "${it.date} ${it.bodyParts} ${it.exerciseName} ${it.sets}×${it.reps} ${it.weightKg}kg"
                },
                customPrompt = customPrompt
            )

            val result = aiRepo.chatCompletion(prompt, null, emptyList(), maxTokens = 4096)
            var parsed = false
            var summary = ""

            result.fold(
                onSuccess = { json ->
                    val plans = parsePlanJson(json)
                    if (plans != null && plans.isNotEmpty()) {
                        prefs.setTrainingPlanJson(gson.toJson(plans))
                        parsed = true
                        summary = buildSummary(plans)
                    }
                },
                onFailure = { }
            )

            if (!parsed) {
                val fallback = buildFallbackPlan(trainingDays)
                prefs.setTrainingPlanJson(gson.toJson(fallback))
                summary = "AI 生成失败，已使用内置标准计划" +
                    if (trainingDays >= 5) "（五分化）" else if (trainingDays >= 4) "（四分化）" else "（三分化）" +
                    "。打开「训练 → 计划」查看。"
            }
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        height: Int,
        weight: Double,
        goal: String,
        experience: String,
        equipment: String,
        trainingDays: Int,
        exerciseNames: String,
        recentTraining: String,
        customPrompt: String
    ): String {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ofPattern("MM月dd日"))
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
        val fmt = DateTimeFormatter.ofPattern("MM-dd")

        return buildString {
            append("你是一位专业健身教练。请根据以下用户信息制定7天训练计划。\n\n")
            append("当前日期：${todayStr} ${dayOfWeek}\n\n")
            append("用户档案：\n")
            append("- 身高: ${height}cm\n")
            append("- 体重: ${weight}kg\n")
            append("- 目标: $goal\n")
            append("- 训练经验: $experience\n")
            append("- 可用器材: ${equipment.ifBlank { "哑铃、杠铃、自重" }}\n")
            append("- 每周训练天数: $trainingDays\n\n")
            if (customPrompt.isNotBlank()) {
                append("用户自定义需求：$customPrompt\n\n")
            }
            if (recentTraining.isNotBlank()) {
                append("最近训练记录：\n$recentTraining\n\n")
            }
            append("可选动作库：$exerciseNames\n\n")
            append("要求：返回纯JSON数组，每天一个对象，7天。\n")
            append("格式：[{\"day\":\"${today.format(fmt)} 周X\",\"date\":\"MM-dd\",\"focus\":\"训练部位\",\"exercises\":")
            append("[{\"name\":\"动作名\",\"sets\":3,\"reps\":\"8-12\",\"notes\":\"备注\"}]}]\n")
            append("day 字段格式必须为\"MM-dd 周X\"，如\"${today.format(fmt)} ${dayOfWeek.replace("星期", "周")}\"。\n")
            append("date 字段为短日期如\"${today.format(fmt)}\"。\n")
            append("休息日 focus 写\"休息日\"，exercises 为空数组。\n")
            append("合理分配部位，符合每周${trainingDays}天训练的安排。从今天开始，连续7天。只返回JSON！")
        }
    }

    private fun parsePlanJson(json: String): List<DayPlan>? {
        return try {
            val cleaned = json
                .replace(Regex("```\\w*\\n?"), "").replace("```", "").trim()
            val start = cleaned.indexOf('[')
            val end = cleaned.lastIndexOf(']')
            val jsonStr = if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned

            val rawType = object : TypeToken<List<Map<String, Any>>>() {}.type
            val raw: List<Map<String, Any>> = gson.fromJson(jsonStr, rawType)
            raw.map { dayMap ->
                val exs = (dayMap["exercises"] as? List<Map<String, Any>>)?.map { exMap ->
                    PlanExercise(
                        name = exMap["name"] as? String ?: "",
                        sets = (exMap["sets"] as? Double)?.toInt() ?: 3,
                        reps = exMap["reps"]?.toString() ?: "8-12",
                        notes = exMap["notes"] as? String
                    )
                } ?: emptyList()
                DayPlan(
                    day = dayMap["day"] as? String ?: "",
                    date = dayMap["date"] as? String ?: "",
                    focus = dayMap["focus"] as? String ?: "",
                    exercises = exs
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildSummary(plans: List<DayPlan>): String {
        val focusList = plans.joinToString("、") { "${it.day} ${it.focus}" }
        return "已生成 7 天训练计划并保存：$focusList。打开「训练 → 计划」查看详情。"
    }

    private fun buildFallbackPlan(trainingDays: Int): List<DayPlan> {
        val now = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("MM-dd")
        val all = mutableListOf<DayPlan>()

        val plan: List<Pair<String, List<Triple<String, Int, String>>>> = when {
            trainingDays >= 5 -> listOf(
                "胸+三头" to listOf(Triple("杠铃卧推", 4, "8-12"), Triple("上斜哑铃卧推", 3, "10-12"), Triple("哑铃飞鸟", 3, "12-15"), Triple("绳索下压", 3, "12-15"), Triple("窄距俯卧撑", 3, "力竭")),
                "背+二头" to listOf(Triple("引体向上", 4, "力竭"), Triple("杠铃划船", 3, "8-12"), Triple("哑铃弯举", 3, "10-12"), Triple("坐姿划船", 3, "10-12"), Triple("锤式弯举", 3, "12-15")),
                "休息日" to emptyList(),
                "腿+肩" to listOf(Triple("杠铃深蹲", 4, "8-12"), Triple("哑铃推举", 3, "10-12"), Triple("腿举", 3, "10-12"), Triple("侧平举", 3, "15-20"), Triple("小腿提踵", 3, "15-20")),
                "胸+背" to listOf(Triple("杠铃卧推", 4, "8-12"), Triple("引体向上", 4, "力竭"), Triple("上斜哑铃卧推", 3, "10-12"), Triple("坐姿划船", 3, "10-12"), Triple("俯身飞鸟", 3, "12-15")),
                "手臂+核心" to listOf(Triple("杠铃弯举", 3, "10-12"), Triple("窄距卧推", 3, "10-12"), Triple("绳索下压", 3, "12-15"), Triple("平板支撑", 3, "60秒"), Triple("悬垂举腿", 3, "15-20")),
                "休息日" to emptyList()
            )
            trainingDays >= 4 -> listOf(
                "胸+三头" to listOf(Triple("杠铃卧推", 4, "8-12"), Triple("上斜哑铃卧推", 3, "10-12"), Triple("哑铃飞鸟", 3, "12-15"), Triple("绳索下压", 3, "12-15")),
                "背+二头" to listOf(Triple("引体向上", 4, "力竭"), Triple("杠铃划船", 3, "8-12"), Triple("哑铃弯举", 3, "10-12"), Triple("坐姿划船", 3, "10-12")),
                "休息日" to emptyList(),
                "腿" to listOf(Triple("杠铃深蹲", 4, "8-12"), Triple("腿举", 3, "10-12"), Triple("罗马尼亚硬拉", 3, "10-12"), Triple("小腿提踵", 3, "15-20")),
                "肩+核心" to listOf(Triple("哑铃推举", 3, "10-12"), Triple("侧平举", 3, "15-20"), Triple("平板支撑", 3, "60秒"), Triple("悬垂举腿", 3, "15-20")),
                "休息日" to emptyList(),
                "休息日" to emptyList()
            )
            else -> listOf(
                "胸+三头" to listOf(Triple("杠铃卧推", 4, "8-12"), Triple("上斜哑铃卧推", 3, "10-12"), Triple("哑铃飞鸟", 3, "12-15"), Triple("绳索下压", 3, "12-15")),
                "休息日" to emptyList(),
                "背+二头" to listOf(Triple("引体向上", 4, "力竭"), Triple("杠铃划船", 3, "8-12"), Triple("哑铃弯举", 3, "10-12"), Triple("坐姿划船", 3, "10-12")),
                "休息日" to emptyList(),
                "腿+肩" to listOf(Triple("杠铃深蹲", 4, "8-12"), Triple("哑铃推举", 3, "10-12"), Triple("腿举", 3, "10-12"), Triple("侧平举", 3, "15-20")),
                "休息日" to emptyList(),
                "休息日" to emptyList()
            )
        }

        plan.forEachIndexed { i, (focus, exercises) ->
            val date = now.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE).replace("星期", "周")
            val dayLabel = "${date.format(fmt)} $dayOfWeek"
            all.add(
                DayPlan(
                    day = dayLabel,
                    date = date.format(fmt),
                    focus = focus,
                    exercises = exercises.map { (n, s, r) -> PlanExercise(n, s, r, null) }
                )
            )
        }
        return all
    }
}
