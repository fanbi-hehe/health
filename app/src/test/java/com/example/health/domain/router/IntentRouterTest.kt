package com.example.health.domain.router

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IntentRouter 单元测试 — 覆盖所有 4 类意图 + 边界条件。
 *
 * 测试用例：
 * 1. 热量查询 — 今天
 * 2. 热量查询 — 近3天（默认）
 * 3. 热量查询 — 近7天
 * 4. 动作进度查询
 * 5. 整体趋势查询
 * 6. 用户档案查询
 * 7. 闲聊/未命中
 * 8. 组合意图（动作+进度优先于整体趋势）
 * 9. 空字符串
 * 10. 无已知动作名时动作关键词降级
 */
class IntentRouterTest {

    private val sampleExercises = listOf("深蹲", "杠铃卧推", "哑铃卧推", "硬拉", "引体向上", "二头弯举")

    // ── 热量/饮食类 ──

    @Test
    fun `热量查询 — 今天`() {
        val result = IntentRouter.resolve("我今天吃了多少热量？", sampleExercises)
        assertTrue(result is IntentQuery.DietCalories)
        assertEquals("today", (result as IntentQuery.DietCalories).timeRange)
    }

    @Test
    fun `热量查询 — 近3天（默认）`() {
        val result = IntentRouter.resolve("最近吃了多少卡路里？", sampleExercises)
        assertTrue(result is IntentQuery.DietCalories)
        assertEquals("3days", (result as IntentQuery.DietCalories).timeRange)
    }

    @Test
    fun `热量查询 — 近7天`() {
        val result = IntentRouter.resolve("这一周摄入的热量达标了吗？", sampleExercises)
        assertTrue(result is IntentQuery.DietCalories)
        assertEquals("7days", (result as IntentQuery.DietCalories).timeRange)
    }

    @Test
    fun `热量查询 — 昨天`() {
        val result = IntentRouter.resolve("昨天吃了什么？", sampleExercises)
        assertTrue(result is IntentQuery.DietCalories)
        assertEquals("yesterday", (result as IntentQuery.DietCalories).timeRange)
    }

    // ── 运动消耗/步数类 ──

    @Test
    fun `运动消耗 — 今日消耗`() {
        val result = IntentRouter.resolve("我今天消耗了多少卡路里？", sampleExercises)
        assertEquals(IntentQuery.ActivitySummary, result)
    }

    @Test
    fun `运动消耗 — 今日步数`() {
        val result = IntentRouter.resolve("今天走了多少步？", sampleExercises)
        assertEquals(IntentQuery.ActivitySummary, result)
    }

    @Test
    fun `运动消耗 — 跑了多远`() {
        val result = IntentRouter.resolve("我昨天跑了多远？", sampleExercises)
        assertEquals(IntentQuery.ActivitySummary, result)
    }

    @Test
    fun `运动消耗 — 运动记录优先于整体趋势`() {
        val result = IntentRouter.resolve("最近运动记录怎么样？", sampleExercises)
        assertEquals(IntentQuery.ActivitySummary, result)
    }

    // ── 动作进度类 ──

    @Test
    fun `动作进度 — 深蹲重量查询`() {
        val result = IntentRouter.resolve("深蹲重量进步了吗？", sampleExercises)
        assertTrue(result is IntentQuery.ExerciseProgress)
        assertEquals("深蹲", (result as IntentQuery.ExerciseProgress).exerciseName)
    }

    @Test
    fun `动作进度 — 长动作名优先匹配`() {
        // "杠铃卧推" 比 "卧推" 更长，应优先匹配
        val exercises = listOf("卧推", "杠铃卧推", "哑铃卧推")
        val result = IntentRouter.resolve("杠铃卧推最近进步了吗？", exercises)
        assertTrue(result is IntentQuery.ExerciseProgress)
        assertEquals("杠铃卧推", (result as IntentQuery.ExerciseProgress).exerciseName)
    }

    // ── 整体趋势类 ──

    @Test
    fun `整体趋势 — 最近怎么样`() {
        val result = IntentRouter.resolve("我最近状态怎么样？", sampleExercises)
        assertTrue(result is IntentQuery.OverallSummary)
    }

    @Test
    fun `整体趋势 — 趋势分析`() {
        val result = IntentRouter.resolve("帮我分析一下最近的训练趋势", sampleExercises)
        assertTrue(result is IntentQuery.OverallSummary)
    }

    // ── 用户档案类 ──

    @Test
    fun `用户档案 — 目标查询`() {
        val result = IntentRouter.resolve("我的体重目标是多少？", sampleExercises)
        assertTrue(result is IntentQuery.UserProfile)
    }

    // ── 闲聊/未命中 ──

    @Test
    fun `闲聊 — 打招呼`() {
        val result = IntentRouter.resolve("你好", sampleExercises)
        assertEquals(IntentQuery.GeneralChat, result)
    }

    @Test
    fun `闲聊 — 空字符串`() {
        val result = IntentRouter.resolve("", sampleExercises)
        assertEquals(IntentQuery.GeneralChat, result)
    }

    @Test
    fun `闲聊 — 纯空白`() {
        val result = IntentRouter.resolve("   ", sampleExercises)
        assertEquals(IntentQuery.GeneralChat, result)
    }

    // ── 组合意图优先级 ──

    @Test
    fun `组合意图 — 动作+进度优先于整体趋势`() {
        // "最近深蹲进步了吗" 同时命中 overall("最近") 和 exercise("深蹲"+"进步")
        // 预期：ExerciseProgress 优先
        val result = IntentRouter.resolve("最近深蹲进步了吗？", sampleExercises)
        assertTrue(result is IntentQuery.ExerciseProgress)
        assertEquals("深蹲", (result as IntentQuery.ExerciseProgress).exerciseName)
    }

    @Test
    fun `组合意图 — 饮食优先于整体`() {
        // "最近饮食趋势" — "饮食"命中热量，"最近"+"趋势"命中整体
        // 热量优先于整体
        val result = IntentRouter.resolve("最近饮食趋势怎么样？", sampleExercises)
        assertTrue(result is IntentQuery.DietCalories)
    }

    // ── 边界条件 ──

    @Test
    fun `无已知动作名时进度关键词不匹配`() {
        // 没有已知动作列表时，"深蹲"不在列表中，不应匹配为动作进度
        val result = IntentRouter.resolve("深蹲进步了吗？", emptyList())
        // 没有已知动作 → 不命中 ExerciseProgress → 检查其他意图
        // "进步"不在 overallKeywords 中，不在 dietKeywords 中，不在 profileKeywords 中
        assertTrue(result is IntentQuery.GeneralChat)
    }

    @Test
    fun `仅动作名无进度关键词不匹配`() {
        // "深蹲怎么做" — 只有动作名，没有进度关键词 → 不应匹配为动作进度
        val result = IntentRouter.resolve("深蹲怎么做？", sampleExercises)
        // "深蹲"命中 exercise name 但缺少"进步/变强"等关键词 → 不应是 ExerciseProgress
        // 也不命中其他意图 → GeneralChat
        assertTrue(result is IntentQuery.GeneralChat)
    }

    @Test
    fun `长文本中提取意图`() {
        val longText = "教练你好，我最近开始健身了，想问问你我最近三天吃了大概多少热量，因为我担心摄入不够影响增肌效果"
        val result = IntentRouter.resolve(longText, sampleExercises)
        assertTrue(result is IntentQuery.DietCalories)
        assertEquals("3days", (result as IntentQuery.DietCalories).timeRange)
    }
}
