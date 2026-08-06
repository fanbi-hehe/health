package com.example.health.domain.calorie

import kotlin.math.roundToInt

/**
 * 热量评估引擎（纯 Kotlin，可单测）。
 *
 * - 基础代谢：Mifflin-St Jeor
 * - 运动消耗（心率法）：Keytel 公式
 * - 步数消耗：粗略估算（0.0005 kcal / 步 / kg）
 * - 心率区间：按最大心率百分比
 */
object CalorieCalculator {

    /** Mifflin-St Jeor 基础代谢（kcal/天）。gender 支持 "男"/"女"/"male"/"female"。 */
    fun bmr(weightKg: Double, heightCm: Int, age: Int, gender: String): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return if (isFemale(gender)) base - 161 else base + 5
    }

    /** Keytel 心率运动消耗（kcal/分钟）。 */
    fun exerciseCaloriesPerMinute(heartRate: Int, weightKg: Double, age: Int, gender: String): Double {
        val hr = heartRate.coerceIn(40, 220)
        val kcalPerMin = if (isFemale(gender)) {
            (-20.4022 + 0.4472 * hr - 0.1263 * weightKg + 0.074 * age) / 4.184
        } else {
            (-55.0969 + 0.6309 * hr + 0.1988 * weightKg + 0.2017 * age) / 4.184
        }
        return kcalPerMin.coerceAtLeast(0.0)
    }

    /** 按平均心率与时长的运动消耗（kcal）。 */
    fun exerciseCalories(heartRate: Int, weightKg: Double, age: Int, gender: String, minutes: Int): Int {
        return (exerciseCaloriesPerMinute(heartRate, weightKg, age, gender) * minutes).roundToInt()
    }

    /** 步数消耗粗略估算：步数 × 0.0005 × 体重(kg)。 */
    fun stepCalories(steps: Int, weightKg: Double): Int {
        return (steps * 0.0005 * weightKg).roundToInt()
    }

    /** 最大心率（220 − 年龄）。 */
    fun maxHeartRate(age: Int): Int = 220 - age

    data class HeartRateZone(val name: String, val minPercent: Int, val maxPercent: Int)

    /** 心率区间（百分比区间，含下限不含上限）。 */
    fun heartRateZones(age: Int): List<HeartRateZone> = listOf(
        HeartRateZone("热身", 50, 60),
        HeartRateZone("燃脂", 60, 70),
        HeartRateZone("有氧", 70, 80),
        HeartRateZone("无氧", 80, 90),
        HeartRateZone("极限", 90, 100)
    )

    /** 给定实时心率，返回所属区间名称。 */
    fun zoneForHeartRate(heartRate: Int, age: Int): String {
        val max = maxHeartRate(age)
        if (max <= 0 || heartRate <= 0) return "未知"
        val pct = heartRate * 100 / max
        return heartRateZones(age)
            .firstOrNull { pct in it.minPercent until it.maxPercent }
            ?.name ?: if (pct >= 90) "极限" else "热身"
    }

    private fun isFemale(gender: String): Boolean =
        gender.trim().lowercase() in setOf("女", "女性", "female", "f")
}
