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
    fun `add_food 带分量同时记今日饮食`() = runBlocking {
        val db = FakeAppDatabase()
        val feedback = ToolExecutor(db).execute(
            "add_food",
            """{"name":"蛋白粉","calories_per_100g":423,"protein_per_100g":75.7,"carbs_per_100g":50,"fat_per_100g":8,"amount_g":60}"""
        )
        assertTrue(feedback, feedback.contains("已同步记录今日饮食"))

        // 食物库已写入
        val food = db.foodDao.getAllFoodsOnce()[0]
        assertEquals("蛋白粉", food.name)
        assertEquals(423, food.caloriesPer100g)

        // 饮食记录已写入：60g → 253.8 ≈ 254 kcal，蛋白 45g，碳水 30g，脂肪 5g
        val diet = db.dietDao.getAllRecordsOnce()
        assertEquals(1, diet.size)
        assertEquals("蛋白粉", diet[0].foodName)
        assertEquals(60, diet[0].weightG)
        assertEquals(254, diet[0].caloriesKcal)
        assertEquals(45, diet[0].proteinG)
        assertEquals(30, diet[0].carbsG)
        assertEquals(5, diet[0].fatG)
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
    fun `record_training 支持力竭次数`() = runBlocking {
        val db = FakeAppDatabase()
        val feedback = ToolExecutor(db).execute(
            "record_training",
            """{"exercise_name":"反手引体向上","sets":3,"reps":"力竭"}"""
        )
        assertTrue(feedback, feedback.contains("已写入"))
        val saved = db.trainingDao.getAllRecordsOnce()[0]
        assertEquals(0, saved.reps)
        assertTrue(feedback.contains("力竭"))
    }

    @Test
    fun `record_training 忽略未知参数`() = runBlocking {
        val db = FakeAppDatabase()
        val feedback = ToolExecutor(db).execute(
            "record_training",
            """{"exercise_name":"死悬垂","sets":2,"reps":0,"duration_minutes":0.75}"""
        )
        assertTrue(feedback, feedback.contains("已写入"))
        assertEquals(1, db.trainingDao.getAllRecordsOnce().size)
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

    @Test
    fun `generate_training_plan 无生成器时提示不可用`() = runBlocking {
        val db = FakeAppDatabase()
        val feedback = ToolExecutor(db).execute(
            "generate_training_plan",
            """{"custom_prompt":"侧重腿部"}"""
        )
        assertTrue(feedback.contains("暂不可用"))
    }

    @Test
    fun `generate_training_plan 调用注入的生成器`() = runBlocking {
        val db = FakeAppDatabase()
        var called = ""
        val executor = ToolExecutor(
            db,
            planGenerator = { custom ->
                called = custom
                "已生成计划（$custom）"
            }
        )
        val feedback = executor.execute(
            "generate_training_plan",
            """{"custom_prompt":"侧重腿部"}"""
        )
        assertEquals("侧重腿部", called)
        assertTrue(feedback.contains("已生成计划"))
    }

    @Test
    fun `工具集包含计划生成且无删除能力`() {
        val names = ToolDefinitions.coachTools.map { it.function.name }
        assertTrue(names.contains("generate_training_plan"))
        assertTrue(names.none { it.contains("delete") || it.contains("remove") })
    }
}
