package com.example.health

import android.app.Application
import com.example.health.data.local.AppDatabase
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.health.data.preference.AppPreferences
import com.example.health.data.repository.ExerciseRepository
import com.example.health.data.repository.FoodRepository
import com.example.health.worker.CoachNotificationWorker
import com.example.health.worker.NotificationHelper
import com.example.health.worker.PhotoCleanupWorker
import com.example.health.worker.StepSyncWorker
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

        // 步数基线定时同步（每 6 小时；App 打开/看板进入时另有即时同步）
        val stepSyncRequest = PeriodicWorkRequestBuilder<StepSyncWorker>(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "step_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            stepSyncRequest
        )

        // 暴躁教练每日提醒：首次计算到提醒时间的延迟，之后每次 Worker 自己排下一次
        val coachRequest = OneTimeWorkRequestBuilder<CoachNotificationWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES) // 启动 1 分钟后首次检查
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "coach_notification",
            ExistingWorkPolicy.KEEP,
            coachRequest
        )
    }
}
