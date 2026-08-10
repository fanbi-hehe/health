package com.example.health.domain.calorie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieCalculatorTest {

    @Test
    fun `bmr male`() {
        // 10×70 + 6.25×175 − 5×30 + 5 = 1648.75
        val result = CalorieCalculator.bmr(weightKg = 70.0, heightCm = 175, age = 30, gender = "男")
        assertEquals(1648.75, result, 0.01)
    }

    @Test
    fun `bmr female`() {
        // 10×60 + 6.25×165 − 5×28 − 161 = 1330.25
        val result = CalorieCalculator.bmr(weightKg = 60.0, heightCm = 165, age = 28, gender = "女")
        assertEquals(1330.25, result, 0.01)
    }

    @Test
    fun `keytel exercise calories per minute male`() {
        // 男 70kg 30岁 HR140：约 12.71 kcal/min
        val result = CalorieCalculator.exerciseCaloriesPerMinute(140, 70.0, 30, "男")
        assertEquals(12.71, result, 0.1)
    }

    @Test
    fun `keytel exercise calories per minute female`() {
        // 女 60kg 30岁 HR140：约 8.81 kcal/min
        val result = CalorieCalculator.exerciseCaloriesPerMinute(140, 60.0, 30, "女")
        assertEquals(8.81, result, 0.1)
    }

    @Test
    fun `exercise calories total`() {
        val result = CalorieCalculator.exerciseCalories(140, 70.0, 30, "男", minutes = 30)
        assertEquals(381, result)
    }

    @Test
    fun `step calories estimate`() {
        // 10000 步 × 0.0005 × 70kg = 350
        assertEquals(350, CalorieCalculator.stepCalories(10000, 70.0))
    }

    @Test
    fun `gps activity calories running`() {
        // 跑步 30 分钟 70kg：9.8 × 3.5 × 70 / 200 × 30 ≈ 360
        val result = CalorieCalculator.gpsActivityCalories("running", 70.0, 30)
        assertEquals(360, result)
    }

    @Test
    fun `gps activity calories cycling`() {
        // 骑行 30 分钟 70kg：6.8 × 3.5 × 70 / 200 × 30 ≈ 250
        val result = CalorieCalculator.gpsActivityCalories("cycling", 70.0, 30)
        assertEquals(250, result)
    }

    @Test
    fun `strength training calories estimate`() {
        // 10 组 × 45 秒 = 7.5 分钟；6.0 × 3.5 × 70 / 200 × 7.5 ≈ 55 kcal
        val result = CalorieCalculator.strengthTrainingCalories(totalSets = 10, weightKg = 70.0)
        assertEquals(55, result)
    }

    @Test
    fun `strength training zero sets`() {
        assertEquals(0, CalorieCalculator.strengthTrainingCalories(0, 70.0))
        assertEquals(0, CalorieCalculator.strengthTrainingCalories(10, 0.0))
    }

    @Test
    fun `max heart rate`() {
        assertEquals(190, CalorieCalculator.maxHeartRate(30))
    }

    @Test
    fun `zone for heart rate`() {
        // 133 / 190 ≈ 70% → 有氧区间
        assertEquals("有氧", CalorieCalculator.zoneForHeartRate(133, 30))
        // 95 / 190 = 50% → 热身
        assertEquals("热身", CalorieCalculator.zoneForHeartRate(95, 30))
        // 180 / 190 ≈ 94% → 极限
        assertEquals("极限", CalorieCalculator.zoneForHeartRate(180, 30))
    }

    @Test
    fun `invalid heart rate does not crash`() {
        val result = CalorieCalculator.exerciseCaloriesPerMinute(0, 70.0, 30, "男")
        assertTrue(result >= 0.0)
    }
}
