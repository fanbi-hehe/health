package com.example.health.domain.action

/**
 * AI 教练可执行的本地操作（只写不删）。
 */
sealed class UserAction {

    /** 记录一条训练（今日无该动作时写入）。 */
    data class RecordTraining(
        val exerciseName: String,
        val sets: Int,
        val reps: Int,
        val weightKg: Double
    ) : UserAction()

    /** 添加自定义食物到食物库。 */
    data class AddFood(
        val name: String,
        val caloriesPer100g: Int,
        val proteinPer100g: Double = 0.0,
        val carbsPer100g: Double = 0.0,
        val fatPer100g: Double = 0.0
    ) : UserAction()

    /** 修改食物库中已有食物的热量。 */
    data class UpdateFood(
        val name: String,
        val caloriesPer100g: Int
    ) : UserAction()

    /** 检测到删除意图（不执行，仅提示）。 */
    object DeleteRequested : UserAction()

    /** 无操作，走普通对话。 */
    object None : UserAction()
}
