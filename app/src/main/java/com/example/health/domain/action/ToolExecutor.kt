package com.example.health.domain.action

import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ActivityRecord
import com.example.health.data.local.entity.DietRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * 执行 AI 模型返回的工具调用。
 *
 * 只接受白名单内的工具名与合法参数，其余一律拒绝（不提供删除能力）。
 */
class ToolExecutor(
    private val db: AppDatabase,
    private val planGenerator: (suspend (String) -> String)? = null,
    private val searchExecutor: (suspend (String) -> String)? = null
) {

    private val gson = Gson()

    suspend fun execute(name: String, argumentsJson: String): String {
        val args = try {
            gson.fromJson<Map<String, Any>>(
                argumentsJson,
                object : TypeToken<Map<String, Any>>() {}.type
            ) ?: emptyMap()
        } catch (_: Exception) {
            return "操作参数解析失败，未执行任何写入。"
        }

        return when (name) {
            "record_training" -> recordTraining(args)
            "add_food" -> addFood(args)
            "update_food" -> updateFood(args)
            "generate_training_plan" -> generateTrainingPlan(args)
            "record_activity_calories" -> recordActivityCalories(args)
            "web_search" -> webSearch(args)
            else -> "未知操作类型，已拒绝执行。"
        }
    }

    /** 生成并保存训练计划；生成器由调用方注入（依赖 Android 上下文与 AI 仓库）。 */
    private suspend fun generateTrainingPlan(args: Map<String, Any>): String {
        val generator = planGenerator ?: return "计划生成功能暂不可用。"
        val customPrompt = stringArg(args, "custom_prompt") ?: ""
        return generator(customPrompt)
    }

    /** 手动记录今日运动消耗（source=manual，逐条累加）。 */
    private suspend fun recordActivityCalories(args: Map<String, Any>): String {
        val type = stringArg(args, "type") ?: return "参数错误：缺少运动类型。"
        val calories = intArg(args, "calories_kcal") ?: return "参数错误：缺少消耗热量。"
        if (calories !in 1..5000) return "热量超出合理范围（1-5000 kcal），未写入。"
        val duration = intArg(args, "duration_minutes") ?: 0
        if (duration !in 0..600) return "时长超出合理范围，未写入。"
        val note = stringArg(args, "note")

        db.activityRecordDao().insert(
            ActivityRecord(
                type = type,
                startTime = System.currentTimeMillis(),
                durationMinutes = duration,
                caloriesKcal = calories,
                source = "manual",
                note = note
            )
        )
        val durationText = if (duration > 0) "，$duration 分钟" else ""
        return "已记录运动消耗：$type $calories kcal$durationText。"
    }

    /** 联网搜索；执行器由调用方注入（依赖网络与 API Key）。 */
    private suspend fun webSearch(args: Map<String, Any>): String {
        val executor = searchExecutor ?: return "搜索功能暂不可用。"
        val query = stringArg(args, "query") ?: return "参数错误：缺少搜索关键词。"
        return executor(query)
    }

    private suspend fun recordTraining(args: Map<String, Any>): String {
        val exerciseName = stringArg(args, "exercise_name") ?: return "参数错误：缺少动作名称。"
        val sets = intArg(args, "sets") ?: return "参数错误：组数无效。"
        // "力竭" 没有数字次数：映射为 0 表示力竭
        val reps = when (val raw = args["reps"]) {
            is Double -> raw.toInt()
            is String -> if (raw.trim() == "力竭") 0 else raw.toIntOrNull()
            else -> null
        } ?: return "参数错误：次数无效。"
        if (sets !in 1..50 || reps !in 0..200) return "参数超出合理范围（组数 1-50、次数 0-200），未写入。"
        val weightKg = doubleArg(args, "weight_kg") ?: 0.0
        if (weightKg !in 0.0..1000.0) return "重量参数超出合理范围，未写入。"

        return UserActionExecutor(db).execute(
            UserAction.RecordTraining(exerciseName, sets, reps, weightKg)
        )
    }

    private suspend fun addFood(args: Map<String, Any>): String {
        val name = stringArg(args, "name") ?: return "参数错误：缺少食物名称。"
        val calories = intArg(args, "calories_per_100g") ?: return "参数错误：缺少每 100 克热量。"
        if (calories !in 1..2000) return "热量超出合理范围（1-2000 kcal/100g），未写入。"
        val protein = doubleArg(args, "protein_per_100g") ?: 0.0
        val carbs = doubleArg(args, "carbs_per_100g") ?: 0.0
        val fat = doubleArg(args, "fat_per_100g") ?: 0.0
        val amountG = doubleArg(args, "amount_g") ?: 0.0
        if (amountG < 0 || amountG > 5000) return "分量参数超出合理范围，未写入。"

        val foodFeedback = UserActionExecutor(db).execute(
            UserAction.AddFood(name, calories, protein, carbs, fat)
        )

        // 用户说了本次分量（如"吃了60克"）：同步记录今日饮食
        if (amountG > 0) {
            val grams = amountG.roundToInt()
            db.dietRecordDao().insert(
                DietRecord(
                    foodName = name,
                    weightG = grams,
                    caloriesKcal = (calories * amountG / 100).roundToInt(),
                    proteinG = (protein * amountG / 100).roundToInt(),
                    carbsG = (carbs * amountG / 100).roundToInt(),
                    fatG = (fat * amountG / 100).roundToInt(),
                    mealType = detectMealType(),
                    timestamp = System.currentTimeMillis()
                )
            )
            val dietCal = (calories * amountG / 100).roundToInt()
            return "$foodFeedback\n已同步记录今日饮食：$name $grams g，约 $dietCal kcal。"
        }
        return foodFeedback
    }

    private suspend fun updateFood(args: Map<String, Any>): String {
        val name = stringArg(args, "name") ?: return "参数错误：缺少食物名称。"
        val calories = intArg(args, "calories_per_100g") ?: return "参数错误：缺少热量。"
        if (calories !in 1..2000) return "热量超出合理范围，未修改。"

        return UserActionExecutor(db).execute(
            UserAction.UpdateFood(name, calories)
        )
    }

    private fun stringArg(args: Map<String, Any>, key: String): String? {
        return (args[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun intArg(args: Map<String, Any>, key: String): Int? {
        return (args[key] as? Double)?.toInt()
            ?: (args[key] as? String)?.toIntOrNull()
    }

    private fun doubleArg(args: Map<String, Any>, key: String): Double? {
        return (args[key] as? Double)
            ?: (args[key] as? String)?.toDoubleOrNull()
    }

    /** 按当前时间推断餐别（与 App 内一致）。 */
    private fun detectMealType(): String = when (LocalTime.now().hour) {
        in 5..10 -> "早餐"
        in 10..14 -> "午餐"
        in 14..20 -> "晚餐"
        else -> "加餐"
    }
}
