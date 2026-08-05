package com.example.health.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

class PhotoCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val cacheDir = applicationContext.cacheDir
            val cutoff = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()
            var deleted = 0

            cacheDir.listFiles()?.forEach { file ->
                val name = file.name
                val isPhoto = name.startsWith("food_") || name.startsWith("chat_") || name.startsWith("photo_")
                if (isPhoto && name.endsWith(".jpg") && file.lastModified() < cutoff) {
                    if (file.delete()) deleted++
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
