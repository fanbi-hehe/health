package com.example.health.domain.action

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolExecutorTest {

    @Test
    fun `record_training 工具写入`() = runBlocking {
        val db = FakeAppDatabase()
        val feedback = ToolExecutor(db).execute(
            "record_training",
            """{"exercise_name":"深蹲","sets":4,"reps":10,"weight_kg":60}"""
        )
        assertTrue(feedback, feedback.contains("已写入"))
        val saved = db.trainingDao.getAllRecordsOnce()
        assertEquals(1, saved.size)
        assertEquals("深蹲", saved[0].exerciseName)
        assertEquals(4, saved[0].sets)
        assertEquals(60.0, saved[0].weightKg, 0.01)
    }

    @Test
    fun `add_food 工具写入完整宏量`() = runBlocking {
        val db = FakeAppDatabase()
        val feedback = ToolExecutor(db).execute(
            "add_food",
            """{"name":"蛋白粉","calories_per_100g":423,"protein_per_100g":75.7,"carbs_per_100g":50,"fat_per_100g":8}"""
        )
        assertTrue(feedback, feedback.contains("已添加"))
        val food = db.foodDao.getAllFoodsOnce()[0]
        assertEquals("蛋白粉", food.name)
        assertEquals(423, food.caloriesPer100g)
        assertEquals(75.7, food.proteinPer100g, 0.01)
        assertEquals(50.0, food.carbsPer100g, 0.01)
        assertEquals(8.0, food.fatPer100g, 0.01)
    }

    @Test
    fun `非法参数被拒绝`() = runBlocking {
        val db = FakeAppDatabase()
        val feedback = ToolExecutor(db).execute(
            "record_training",
            """{"exercise_name":"深蹲","sets":999,"reps":10}"""
        )
        assertTrue(feedback.contains("合理范围"))
        assertEquals(0, db.trainingDao.getAllRecordsOnce().size)
    }

    @Test
    fun `未知工具被拒绝`() = runBlocking {
        val db = FakeAppDatabase()
        val feedback = ToolExecutor(db).execute("delete_food", """{"name":"红烧肉"}""")
        assertTrue(feedback.contains("拒绝"))
    }

    @Test
    fun `工具集不含任何删除能力`() {
        val names = ToolDefinitions.coachTools.map { it.function.name }
        assertTrue(names.contains("record_training"))
        assertTrue(names.contains("add_food"))
        assertTrue(names.contains("update_food"))
        assertTrue(names.none { it.contains("delete") || it.contains("remove") })
    }

    @Test
    fun `工具 schema 必填参数完整`() {
        val addFood = ToolDefinitions.coachTools.first { it.function.name == "add_food" }
        @Suppress("UNCHECKED_CAST")
        val required = addFood.function.parameters["required"] as? List<String>
        assertTrue(required != null && required.contains("name") && required.contains("calories_per_100g"))
    }
}
