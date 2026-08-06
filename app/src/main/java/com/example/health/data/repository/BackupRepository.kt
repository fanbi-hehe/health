package com.example.health.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.BodyWeight
import com.example.health.data.local.entity.ChatMessage
import com.example.health.data.local.entity.DietRecord
import com.example.health.data.local.entity.ExerciseLibrary
import com.example.health.data.local.entity.FoodLibrary
import com.example.health.data.local.entity.TrainingRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 统一的数据备份/恢复仓库。
 * 收敛 DashboardViewModel 与 SettingsViewModel 中重复的导入导出逻辑。
 */
class BackupRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val gson = Gson()

    // ── 导出 ────────────────────────────────────────────────

    data class BackupData(
        val exportTime: String,
        val version: Int = 1,
        val diet_records: List<DietRecord>,
        val training_records: List<TrainingRecord>,
        val body_weights: List<BodyWeight>,
        val chat_messages: List<ChatMessage>,
        val food_library: List<FoodLibrary>,
        val exercise_library: List<ExerciseLibrary>
    )

    /**
     * 导出所有数据到公共 Downloads 目录（通过 MediaStore，兼容 targetSdk 30+）。
     * @return 成功时返回文件路径，失败抛异常。
     */
    suspend fun exportAll(): String = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        // 收集所有数据（使用 suspend 一次性查询，避免 stateIn 反模式）
        val backup = BackupData(
            exportTime = java.time.LocalDateTime.now().toString(),
            diet_records = db.dietRecordDao().getAllRecordsOnce(),
            training_records = db.trainingRecordDao().getAllRecordsOnce(),
            body_weights = db.bodyWeightDao().getAllRecordsOnce(),
            chat_messages = db.chatMessageDao().getAllMessagesOnce(),
            food_library = db.foodLibraryDao().getAllFoodsOnce(),
            exercise_library = db.exerciseLibraryDao().getAllExercisesOnce()
        )

        val json = gson.toJson(backup)
        val fileName = "增重助手备份_$today.json"

        // 使用 MediaStore 写入公共 Downloads（API 29+）
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
        ) ?: throw Exception("无法创建下载文件")

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } ?: throw Exception("无法写入文件")

        fileName // 返回文件名
    }

    // ── 导入 ────────────────────────────────────────────────

    data class ImportResult(
        val dietCount: Int,
        val trainingCount: Int,
        val weightCount: Int,
        val chatCount: Int,
        val foodCount: Int,
        val exerciseCount: Int
    )

    /**
     * 从 JSON 字符串导入数据。
     * 1. 先校验 JSON 结构 → 格式非法直接抛异常，不触碰现有数据
     * 2. 校验通过后在数据库事务中：清空 → 批量插入
     * 3. 事务失败整体回滚
     * @return 各表导入记录数
     */
    suspend fun importFromJson(jsonString: String): ImportResult = withContext(Dispatchers.IO) {
        if (jsonString.isBlank()) throw Exception("JSON 数据为空")

        // ── 步骤 1：解析 + 校验（在事务外，不触碰数据） ──
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        val map: Map<String, Any> = try {
            gson.fromJson(jsonString, mapType) ?: throw Exception("JSON 解析失败")
        } catch (e: Exception) {
            if (e.message == "JSON 解析失败") throw e
            throw Exception("JSON 格式非法: ${e.message}")
        }

        // 校验必需字段
        val requiredKeys = listOf("diet_records", "training_records", "body_weights", "chat_messages")
        for (key in requiredKeys) {
            if (!map.containsKey(key)) {
                throw Exception("JSON 缺少必需字段: $key，请检查备份文件是否完整")
            }
        }

        // 解析各表数据
        val dietRecords: List<DietRecord> = parseList(map["diet_records"])
        val trainingRecords: List<TrainingRecord> = parseList(map["training_records"])
        val bodyWeights: List<BodyWeight> = parseList(map["body_weights"])
        val chatMessages: List<ChatMessage> = parseList(map["chat_messages"])
        val foodLibrary: List<FoodLibrary> = parseList(map["food_library"])
        val exerciseLibrary: List<ExerciseLibrary> = parseList(map["exercise_library"])

        // ── 步骤 2：在事务中清空 + 写入 ──
        db.withTransaction {
            // 清空现有数据
            db.dietRecordDao().deleteAll()
            db.trainingRecordDao().deleteAll()
            db.bodyWeightDao().deleteAll()
            db.chatMessageDao().deleteAll()
            db.foodLibraryDao().deleteAllCustom()
            db.exerciseLibraryDao().deleteAllCustom()

            // 批量插入
            // 注意：chatMessageDao 没有 insertAll，foodLibraryDao 的 insertAll 对内置食物是 replace
            if (dietRecords.isNotEmpty()) db.dietRecordDao().insertAll(dietRecords)
            if (trainingRecords.isNotEmpty()) db.trainingRecordDao().insertAll(trainingRecords)
            if (bodyWeights.isNotEmpty()) db.bodyWeightDao().insertAll(bodyWeights)
            chatMessages.forEach { db.chatMessageDao().insert(it) }
            // 只导入自定义食物和动作（内置的在首次启动时已初始化）
            val customFoods = foodLibrary.filter { it.isCustom }
            if (customFoods.isNotEmpty()) db.foodLibraryDao().insertAll(customFoods)
            val customExercises = exerciseLibrary.filter { it.isCustom }
            if (customExercises.isNotEmpty()) db.exerciseLibraryDao().insertAll(customExercises)
        }

        ImportResult(
            dietCount = dietRecords.size,
            trainingCount = trainingRecords.size,
            weightCount = bodyWeights.size,
            chatCount = chatMessages.size,
            foodCount = foodLibrary.count { it.isCustom },
            exerciseCount = exerciseLibrary.count { it.isCustom }
        )
    }

    /**
     * 安全解析 JSON 列表为实体列表。
     */
    private inline fun <reified T> parseList(data: Any?): List<T> {
        if (data == null) return emptyList()
        return try {
            val json = gson.toJson(data)
            val listType = TypeToken.getParameterized(List::class.java, T::class.java).type
            gson.fromJson<List<T>>(json, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
