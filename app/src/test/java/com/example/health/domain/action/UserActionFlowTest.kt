package com.example.health.domain.action

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.dao.ActivityRecordDao
import com.example.health.data.local.dao.AdviceLogDao
import com.example.health.data.local.dao.BodyWeightDao
import com.example.health.data.local.dao.ChatMessageDao
import com.example.health.data.local.dao.DailyStepCountDao
import com.example.health.data.local.dao.DietRecordDao
import com.example.health.data.local.dao.ExerciseLibraryDao
import com.example.health.data.local.dao.FoodLibraryDao
import com.example.health.data.local.dao.MealTemplateDao
import com.example.health.data.local.dao.TrainingRecordDao
import com.example.health.data.local.entity.ExerciseLibrary
import com.example.health.data.local.entity.ActivityRecord
import com.example.health.data.local.entity.DietRecord
import com.example.health.data.local.entity.FoodLibrary
import com.example.health.data.local.entity.TrainingRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AI 教练动作流程集成测试（JVM 内存实现）：
 * 用户句子 → 解析器 → 执行器 → 写库 → 读回。
 */
class UserActionFlowTest {

    @Test
    fun `记录训练完整流程`() = runBlocking {
        val db = FakeAppDatabase()

        // 1. 解析（动作库没有"深蹲"时也能提取）
        val action = UserActionParser.parse(
            "记录深蹲 4组 10次 60kg",
            listOf("杠铃卧推", "哑铃卧推")
        )
        assertTrue(action is UserAction.RecordTraining)

        // 2. 执行
        val feedback = UserActionExecutor(db).execute(action)
        assertTrue(feedback, feedback.contains("已写入"))

        // 3. 读回验证
        val saved = db.trainingRecordDao().getAllRecordsOnce()
        assertEquals(1, saved.size)
        assertEquals("深蹲", saved[0].exerciseName)
        assertEquals(4, saved[0].sets)
        assertEquals(10, saved[0].reps)
        assertEquals(60.0, saved[0].weightKg, 0.01)
    }

    @Test
    fun `今日已有同动作不重复写入`() = runBlocking {
        val db = FakeAppDatabase()
        val executor = UserActionExecutor(db)

        executor.execute(UserAction.RecordTraining("深蹲", 4, 10, 60.0))
        val feedback = executor.execute(UserAction.RecordTraining("深蹲", 5, 5, 80.0))

        assertTrue(feedback.contains("已存在"))
        assertEquals(1, db.trainingRecordDao().getAllRecordsOnce().size)
    }

    @Test
    fun `添加食物完整流程`() = runBlocking {
        val db = FakeAppDatabase()
        val action = UserActionParser.parse("添加食物 红烧肉 300kcal", emptyList())
        assertTrue(action is UserAction.AddFood)

        val feedback = UserActionExecutor(db).execute(action)
        assertTrue(feedback.contains("已添加"))

        val foods = db.foodLibraryDao().getAllFoodsOnce()
        assertEquals(1, foods.size)
        assertEquals("红烧肉", foods[0].name)
        assertEquals(300, foods[0].caloriesPer100g)
    }

    @Test
    fun `添加蛋白粉完整流程（千焦换算+蛋白质）`() = runBlocking {
        val db = FakeAppDatabase()
        val action = UserActionParser.parse(
            "添加食物，蛋白粉。60克。它的总能量是每百克，1768千焦，75.7克蛋白质。",
            emptyList()
        )
        assertTrue(action is UserAction.AddFood)

        val feedback = UserActionExecutor(db).execute(action)
        assertTrue(feedback, feedback.contains("已添加"))
        assertTrue(feedback.contains("423 kcal/100g"))
        assertTrue(feedback.contains("75.7"))

        val foods = db.foodLibraryDao().getAllFoodsOnce()
        assertEquals(1, foods.size)
        assertEquals("蛋白粉", foods[0].name)
        assertEquals(423, foods[0].caloriesPer100g)
        assertEquals(75.7, foods[0].proteinPer100g, 0.01)
    }

    @Test
    fun `修改食物完整流程`() = runBlocking {
        val db = FakeAppDatabase()
        db.foodLibraryDao().insert(
            FoodLibrary(name = "红烧肉", caloriesPer100g = 300, isCustom = true)
        )

        val action = UserActionParser.parse("把红烧肉的热量改成 350", emptyList())
        assertTrue(action is UserAction.UpdateFood)

        val feedback = UserActionExecutor(db).execute(action)
        assertTrue(feedback.contains("已更新"))
        assertEquals(350, db.foodLibraryDao().getAllFoodsOnce()[0].caloriesPer100g)
    }

    @Test
    fun `删除意图不写入任何数据`() = runBlocking {
        val db = FakeAppDatabase()
        val feedback = UserActionExecutor(db).execute(UserActionParser.parse("删除深蹲", listOf("深蹲")))
        assertTrue(feedback.contains("不会执行删除"))
        assertEquals(0, db.trainingRecordDao().getAllRecordsOnce().size)
    }
}

// ── 内存假实现（仅测试用） ──

internal class FakeAppDatabase : AppDatabase() {
    val trainingDao = FakeTrainingRecordDao()
    val foodDao = FakeFoodLibraryDao()
    val exerciseDao = FakeExerciseLibraryDao()
    val dietDao = FakeDietRecordDao()
    val activityDao = FakeActivityRecordDao()

    override fun trainingRecordDao(): TrainingRecordDao = trainingDao
    override fun foodLibraryDao(): FoodLibraryDao = foodDao
    override fun exerciseLibraryDao(): ExerciseLibraryDao = exerciseDao
    override fun dietRecordDao(): DietRecordDao = dietDao
    override fun activityRecordDao(): ActivityRecordDao = activityDao

    override fun bodyWeightDao(): BodyWeightDao = throw UnsupportedOperationException()
    override fun chatMessageDao(): ChatMessageDao = throw UnsupportedOperationException()
    override fun adviceLogDao(): AdviceLogDao = throw UnsupportedOperationException()
    override fun mealTemplateDao(): MealTemplateDao = throw UnsupportedOperationException()
    override fun dailyStepCountDao(): DailyStepCountDao = throw UnsupportedOperationException()

    override fun clearAllTables() {}
    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper =
        throw UnsupportedOperationException("fake")

    override fun createInvalidationTracker(): InvalidationTracker =
        throw UnsupportedOperationException("fake")
}

internal class FakeTrainingRecordDao : TrainingRecordDao {
    val records = mutableListOf<TrainingRecord>()
    private var nextId = 1L

    override suspend fun insert(record: TrainingRecord): Long {
        val withId = record.copy(id = nextId++)
        records.add(withId)
        return withId.id
    }

    override suspend fun insertAll(records: List<TrainingRecord>) {
        records.forEach { insert(it) }
    }

    override fun getAllRecords(): Flow<List<TrainingRecord>> = flowOf(records.toList())
    override suspend fun getAllRecordsOnce(): List<TrainingRecord> = records.toList()
    override suspend fun getRecordsByDate(date: String): List<TrainingRecord> =
        records.filter { it.date == date }

    override suspend fun delete(record: TrainingRecord) { records.remove(record) }
    override suspend fun getRecordById(id: Long): TrainingRecord? = records.firstOrNull { it.id == id }
    override suspend fun getDistinctExercisesByBodyPart(bodyPart: String): List<String> = emptyList()
    override suspend fun getRecordsByExerciseName(exerciseName: String): List<TrainingRecord> = emptyList()
    override suspend fun getRecentRecordsByExercise(exerciseName: String, limit: Int): List<TrainingRecord> = emptyList()
    override suspend fun update(record: TrainingRecord) {
        val idx = records.indexOfFirst { it.id == record.id }
        if (idx >= 0) records[idx] = record
    }
    override suspend fun getDistinctExerciseNames(): List<String> = records.map { it.exerciseName }.distinct()
    override suspend fun getRecordsBetweenDates(startDate: String, endDate: String): List<TrainingRecord> = emptyList()
    override suspend fun deleteAll() { records.clear() }
}

internal class FakeFoodLibraryDao : FoodLibraryDao {
    val foods = mutableListOf<FoodLibrary>()
    private var nextId = 1L

    override suspend fun insert(food: FoodLibrary): Long {
        val withId = food.copy(id = nextId++)
        foods.add(withId)
        return withId.id
    }

    override suspend fun insertAll(foods: List<FoodLibrary>) { foods.forEach { insert(it) } }
    override suspend fun update(food: FoodLibrary) {
        val idx = foods.indexOfFirst { it.id == food.id }
        if (idx >= 0) foods[idx] = food
    }
    override suspend fun delete(food: FoodLibrary) { foods.remove(food) }
    override fun getAllFoods(): Flow<List<FoodLibrary>> = flowOf(foods.toList())
    override suspend fun getAllFoodsOnce(): List<FoodLibrary> = foods.toList()
    override fun searchFoods(query: String): Flow<List<FoodLibrary>> = flowOf(emptyList())
    override fun getCustomFoods(): Flow<List<FoodLibrary>> = flowOf(foods.filter { it.isCustom })
    override suspend fun deleteAllCustom() { foods.removeAll { it.isCustom } }
}

internal class FakeExerciseLibraryDao : ExerciseLibraryDao {
    override fun getAllExercises(): Flow<List<ExerciseLibrary>> = flowOf(emptyList())
    override suspend fun getAllExercisesOnce(): List<ExerciseLibrary> = emptyList()
    override fun searchExercises(query: String): Flow<List<ExerciseLibrary>> = flowOf(emptyList())
    override fun getExercisesByBodyPart(bodyPart: String): Flow<List<ExerciseLibrary>> = flowOf(emptyList())
    override fun getCustomExercises(): Flow<List<ExerciseLibrary>> = flowOf(emptyList())
    override suspend fun insert(exercise: ExerciseLibrary): Long = throw UnsupportedOperationException()
    override suspend fun insertAll(exercises: List<ExerciseLibrary>) = throw UnsupportedOperationException()
    override suspend fun update(exercise: ExerciseLibrary) = throw UnsupportedOperationException()
    override suspend fun delete(exercise: ExerciseLibrary) = throw UnsupportedOperationException()
    override suspend fun deleteAllCustom() = throw UnsupportedOperationException()
}

internal class FakeDietRecordDao : DietRecordDao {
    val records = mutableListOf<DietRecord>()
    private var nextId = 1L

    override suspend fun insert(record: DietRecord): Long {
        val withId = record.copy(id = nextId++)
        records.add(withId)
        return withId.id
    }

    override suspend fun insertAll(records: List<DietRecord>) { records.forEach { insert(it) } }
    override suspend fun delete(record: DietRecord) { records.remove(record) }
    override fun getAllRecords(): Flow<List<DietRecord>> = flowOf(records.toList())
    override fun getRecordsByMealType(mealType: String): Flow<List<DietRecord>> = flowOf(emptyList())
    override suspend fun getRecordsByDate(date: String): List<DietRecord> = emptyList()
    override suspend fun getTotalCaloriesByDate(date: String): Int? = null
    override suspend fun getRecordsBetweenDates(startDate: String, endDate: String): List<DietRecord> = emptyList()
    override suspend fun getTotalCaloriesBetweenDates(startDate: String, endDate: String): Int? = null
    override suspend fun getRecordById(id: Long): DietRecord? = records.firstOrNull { it.id == id }
    override suspend fun getAllRecordsOnce(): List<DietRecord> = records.toList()
    override suspend fun getRecentDatesWithRecords(limit: Int): List<String> = emptyList()
    override suspend fun update(record: DietRecord) {
        val idx = records.indexOfFirst { it.id == record.id }
        if (idx >= 0) records[idx] = record
    }
    override suspend fun deleteAll() { records.clear() }
}

internal class FakeActivityRecordDao : ActivityRecordDao {
    val records = mutableListOf<ActivityRecord>()
    private var nextId = 1L

    override suspend fun insert(record: ActivityRecord): Long {
        val withId = record.copy(id = nextId++)
        records.add(withId)
        return withId.id
    }

    override suspend fun insertAll(records: List<ActivityRecord>) { records.forEach { insert(it) } }
    override suspend fun delete(record: ActivityRecord) { records.remove(record) }
    override fun getAllRecords(): Flow<List<ActivityRecord>> = flowOf(records.toList())
    override suspend fun getRecordsByDate(date: String): List<ActivityRecord> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return records.filter { sdf.format(java.util.Date(it.startTime)) == date }
    }
    override suspend fun getRecordsBetweenDates(startDate: String, endDate: String): List<ActivityRecord> = emptyList()
    override suspend fun getTotalCaloriesByDate(date: String): Int = records.sumOf { it.caloriesKcal }
    override suspend fun getTotalCaloriesBetweenDates(startDate: String, endDate: String): Int = 0
    override suspend fun getAllRecordsOnce(): List<ActivityRecord> = records.toList()
    override suspend fun deleteAll() { records.clear() }
}
