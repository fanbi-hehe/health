package com.example.health.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.health.util.StepCounterManager

/**
 * 定时同步系统步数（每 6 小时一次，App 打开时另有即时同步）。
 */
class StepSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            StepCounterManager.syncNow(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
