package com.example.health.data.repository

import android.content.Context
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.FoodLibrary
import com.example.health.data.preference.AppPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * 食物库仓库 —— 管理内置食物初始化 + 自定义食物 CRUD + 搜索。
 */
class FoodRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).foodLibraryDao()
    private val prefs = AppPreferences(context)
    private val gson = Gson()

    // ── 查询 ──

    fun getAllFoods(): Flow<List<FoodLibrary>> = dao.getAllFoods()

    fun searchFoods(query: String): Flow<List<FoodLibrary>> = dao.searchFoods(query)

    fun getCustomFoods(): Flow<List<FoodLibrary>> = dao.getCustomFoods()

    // ── 自定义食物 CRUD ──

    suspend fun insertCustomFood(name: String, caloriesPer100g: Int): Long {
        return dao.insert(FoodLibrary(name = name, caloriesPer100g = caloriesPer100g, isCustom = true))
    }

    suspend fun updateFood(food: FoodLibrary) = dao.update(food)

    suspend fun deleteFood(food: FoodLibrary) = dao.delete(food)

    // ── 内置食物初始化 ──

    /**
     * 首次启动时从 assets/builtin_foods.json 导入内置食物到 Room。
     * 通过 DataStore 标记确保只执行一次。
     */
    suspend fun initializeBuiltinFoodsIfNeeded() {
        // 已初始化且表非空时才跳过；若表被清空（如数据库升级重建），自动重新导入内置食物
        if (prefs.foodsInitialized.first() && dao.getAllFoods().first().isNotEmpty()) return

        try {
            val json = context.assets.open("builtin_foods.json")
                .bufferedReader()
                .use { it.readText() }

            val listType = object : TypeToken<List<BuiltinFoodDto>>() {}.type
            val foods: List<BuiltinFoodDto> = gson.fromJson(json, listType) ?: emptyList()

            val entities = foods.map { dto ->
                FoodLibrary(
                    name = dto.name,
                    caloriesPer100g = dto.caloriesPer100g,
                    isCustom = false
                )
            }

            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
            }

            prefs.setFoodsInitialized(true)
        } catch (e: IOException) {
            // JSON 文件读取失败（assets 中不存在或损坏），跳过
            e.printStackTrace()
        }
    }

    // JSON 解析对应的 DTO
    private data class BuiltinFoodDto(
        val name: String,
        val caloriesPer100g: Int,
        val isCustom: Boolean = false
    )
}
