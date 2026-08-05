package com.example.health

import android.app.Application
import com.example.health.data.local.AppDatabase
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.health.data.preference.AppPreferences
import com.example.health.data.repository.ExerciseRepository
import com.example.health.data.repository.FoodRepository
import com.example.health.worker.CoachNotificationWorker
import com.example.health.worker.NotificationHelper
import com.example.health.worker.PhotoCleanupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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

        // 创建通知渠道
        NotificationHelper.createChannel(this)

        // 定期清理 30 天前的旧照片（每天一次）
        val cleanupRequest = PeriodicWorkRequestBuilder<PhotoCleanupWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "photo_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )

        // 暴躁教练每日提醒（每 15 分钟检查一次，Worker 内部判断是否到时间 + 是否达标）
        val coachRequest = PeriodicWorkRequestBuilder<CoachNotificationWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "coach_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            coachRequest
        )
    }
}
