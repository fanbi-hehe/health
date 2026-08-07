package com.example.health.domain.plan

/** 训练计划的一天（7 天计划数组元素）。 */
data class DayPlan(
    val day: String,
    val date: String,
    val focus: String,
    val exercises: List<PlanExercise> = emptyList()
)

/** 计划中的单个动作。 */
data class PlanExercise(
    val name: String,
    val sets: Int,
    val reps: String,
    val notes: String? = null
)
