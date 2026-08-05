package com.example.health

import android.app.Application
import com.example.health.data.local.AppDatabase
import com.example.health.data.preference.AppPreferences
import com.example.health.data.repository.ExerciseRepository
import com.example.health.data.repository.FoodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HealthApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: AppPreferences
        private set

    lateinit var foodRepository: FoodRepository
        private set

    lateinit var exerciseRepository: ExerciseRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        preferences = AppPreferences(this)
        foodRepository = FoodRepository(this)
        exerciseRepository = ExerciseRepository(this)

        // 首次启动：导入内置食物 + 内置动作
        appScope.launch {
            foodRepository.initializeBuiltinFoodsIfNeeded()
            exerciseRepository.initializeBuiltinExercisesIfNeeded()
        }
    }
}
