package com.example.health.domain.action

import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.FoodLibrary
import com.example.health.data.local.entity.TrainingRecord
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 执行 AI 教练解析出的本地操作（只写不删），并返回给 AI 的确认文本。
 */
class UserActionExecutor(private val db: AppDatabase) {

    suspend fun execute(action: UserAction): String = when (action) {
        is UserAction.RecordTraining -> recordTraining(action)
        is UserAction.AddFood -> addFood(action)
        is UserAction.UpdateFood -> updateFood(action)
        UserAction.DeleteRequested ->
            "出于数据安全考虑，AI 助手不会执行删除操作。如需删除，请在 App 内手动操作。"
        UserAction.None -> ""
    }

    /** 今日无该动作时写入训练记录；已有则不重复。 */
    private suspend fun recordTraining(action: UserAction.RecordTraining): String {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todayRecords = db.trainingRecordDao().getRecordsByDate(today)

        if (todayRecords.any { it.exerciseName.contains(action.exerciseName, ignoreCase = true) }) {
            return "今日已存在「${action.exerciseName}」的训练记录，未重复写入。"
        }

        // 从动作库查部位，未收录则为空
        val bodyPart = db.exerciseLibraryDao().getAllExercises().first()
            .firstOrNull { it.name == action.exerciseName }
            ?.bodyPart ?: ""

        db.trainingRecordDao().insert(
            TrainingRecord(
                date = today,
                timestamp = System.currentTimeMillis(),
                bodyParts = bodyPart,
                exerciseName = action.exerciseName,
                sets = action.sets,
                reps = action.reps,
                weightKg = action.weightKg,
                notes = "AI 教练记录"
            )
        )
        val repsText = if (action.reps == 0) "力竭" else "${action.reps}次"
        return "已写入今日训练记录：${action.exerciseName} ${action.sets}组×$repsText ${action.weightKg}kg。"
    }

    /** 添加自定义食物；同名已存在则不重复添加。 */
    private suspend fun addFood(action: UserAction.AddFood): String {
        val foods = db.foodLibraryDao().getAllFoods().first()
        if (foods.any { it.name == action.name }) {
            return "「${action.name}」已在食物库中，未重复添加。如需修改热量，请说：把${action.name}的热量改成数值。"
        }
        db.foodLibraryDao().insert(
            FoodLibrary(
                name = action.name,
                caloriesPer100g = action.caloriesPer100g,
                proteinPer100g = action.proteinPer100g,
                carbsPer100g = action.carbsPer100g,
                fatPer100g = action.fatPer100g,
                isCustom = true
            )
        )
        val macros = buildList {
            if (action.proteinPer100g > 0) add("蛋白 ${"%.1f".format(action.proteinPer100g)}g")
            if (action.carbsPer100g > 0) add("碳水 ${"%.1f".format(action.carbsPer100g)}g")
            if (action.fatPer100g > 0) add("脂肪 ${"%.1f".format(action.fatPer100g)}g")
        }
        val macrosText = if (macros.isNotEmpty()) "（${macros.joinToString(" · ")}/100g）" else ""
        return "已添加自定义食物：「${action.name}」${action.caloriesPer100g} kcal/100g$macrosText。"
    }

    /** 更新食物热量（模糊匹配名称）。 */
    private suspend fun updateFood(action: UserAction.UpdateFood): String {
        val foods = db.foodLibraryDao().getAllFoods().first()
        val match = foods.firstOrNull {
            it.name.contains(action.name, ignoreCase = true) ||
                action.name.contains(it.name, ignoreCase = true)
        }
        if (match == null) {
            return "食物库中未找到「${action.name}」。可以告诉我：添加食物 名称 热量。"
        }
        db.foodLibraryDao().update(match.copy(caloriesPer100g = action.caloriesPer100g))
        return "已更新「${match.name}」热量为 ${action.caloriesPer100g} kcal/100g。"
    }
}
