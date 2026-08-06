package com.example.health.domain.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserActionParserTest {

    private val exercises = listOf("深蹲", "杠铃卧推", "哑铃卧推", "硬拉", "引体向上")

    @Test
    fun `记录训练 — 标准句式`() {
        val action = UserActionParser.parse("我做了深蹲 4组 10次 60kg", exercises)
        assertTrue(action is UserAction.RecordTraining)
        action as UserAction.RecordTraining
        assertEquals("深蹲", action.exerciseName)
        assertEquals(4, action.sets)
        assertEquals(10, action.reps)
        assertEquals(60.0, action.weightKg, 0.01)
    }

    @Test
    fun `记录训练 — 乘号句式`() {
        val action = UserActionParser.parse("今天练了杠铃卧推 3x12 50kg", exercises)
        assertTrue(action is UserAction.RecordTraining)
        action as UserAction.RecordTraining
        assertEquals("杠铃卧推", action.exerciseName)
        assertEquals(3, action.sets)
        assertEquals(12, action.reps)
        assertEquals(50.0, action.weightKg, 0.01)
    }

    @Test
    fun `记录训练 — 无重量`() {
        val action = UserActionParser.parse("记录一下 引体向上 4组 8次", exercises)
        assertTrue(action is UserAction.RecordTraining)
        action as UserAction.RecordTraining
        assertEquals(4, action.sets)
        assertEquals(8, action.reps)
        assertEquals(0.0, action.weightKg, 0.01)
    }

    @Test
    fun `记录训练 — 未知动作名也能提取`() {
        val action = UserActionParser.parse("我做了仰卧起坐 3组 20次", exercises)
        assertTrue(action is UserAction.RecordTraining)
        action as UserAction.RecordTraining
        assertEquals("仰卧起坐", action.exerciseName)
        assertEquals(3, action.sets)
        assertEquals(20, action.reps)
    }

    @Test
    fun `添加食物 — kcal`() {
        val action = UserActionParser.parse("添加食物 红烧肉 300kcal", exercises)
        assertTrue(action is UserAction.AddFood)
        action as UserAction.AddFood
        assertEquals("红烧肉", action.name)
        assertEquals(300, action.caloriesPer100g)
    }

    @Test
    fun `添加食物 — 千卡`() {
        val action = UserActionParser.parse("新增食物 鸡胸肉 200千卡", exercises)
        assertTrue(action is UserAction.AddFood)
        action as UserAction.AddFood
        assertEquals("鸡胸肉", action.name)
        assertEquals(200, action.caloriesPer100g)
    }

    @Test
    fun `修改食物热量`() {
        val action = UserActionParser.parse("把红烧肉的热量改成 350", exercises)
        assertTrue(action is UserAction.UpdateFood)
        action as UserAction.UpdateFood
        assertEquals("红烧肉", action.name)
        assertEquals(350, action.caloriesPer100g)
    }

    @Test
    fun `修改食物热量 — 无把字`() {
        val action = UserActionParser.parse("鸡胸肉热量改为 250 kcal", exercises)
        assertTrue(action is UserAction.UpdateFood)
        action as UserAction.UpdateFood
        assertEquals("鸡胸肉", action.name)
        assertEquals(250, action.caloriesPer100g)
    }

    @Test
    fun `删除意图被拦截`() {
        val action = UserActionParser.parse("删除红烧肉", exercises)
        assertEquals(UserAction.DeleteRequested, action)
    }

    @Test
    fun `删除训练记录被拦截`() {
        val action = UserActionParser.parse("把今天深蹲的记录删掉", exercises)
        assertEquals(UserAction.DeleteRequested, action)
    }

    @Test
    fun `普通对话不解析为动作`() {
        val action = UserActionParser.parse("今天天气怎么样", exercises)
        assertEquals(UserAction.None, action)
    }

    @Test
    fun `空文本不解析`() {
        assertEquals(UserAction.None, UserActionParser.parse("", exercises))
    }
}
