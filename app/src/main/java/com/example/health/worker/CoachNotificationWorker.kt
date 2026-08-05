package com.example.health.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.health.data.local.AppDatabase
import com.example.health.data.preference.AppPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class CoachNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = AppPreferences(applicationContext)
            val enabled = prefs.coachNotificationEnabled.first()
            if (!enabled) {
                scheduleNext(prefs)
                return Result.success()
            }

            val targetCalories = prefs.targetDailyCalories.first()
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val db = AppDatabase.getInstance(applicationContext)
            val todayCals = db.dietRecordDao().getTotalCaloriesByDate(today) ?: 0

            if (todayCals < targetCalories * 0.8) {
                val quotes = loadQuotes(prefs)
                val quote = if (quotes.isNotEmpty()) quotes.random() else defaultQuote()
                val deficit = targetCalories - todayCals
                val message = quote.replace("{deficit}", deficit.toString())
                    .replace("{target}", targetCalories.toString())
                    .replace("{today}", todayCals.toString())

                NotificationHelper.send(applicationContext, "暴躁教练提醒 🔥", message)
            }

            scheduleNext(prefs)
            Result.success()
        } catch (e: Exception) {
            scheduleNext(AppPreferences(applicationContext))
            Result.success()
        }
    }

    /**
     * 排定下一次提醒：明天配置的时间。
     */
    private suspend fun scheduleNext(prefs: AppPreferences) {
        val hour = prefs.coachReminderHour.first()
        val minute = prefs.coachReminderMinute.first()

        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0)
        if (next <= now) next = next.plusDays(1)

        val delayMs = ChronoUnit.MILLIS.between(now, next)

        val request = OneTimeWorkRequestBuilder<CoachNotificationWorker>()
            .setInitialDelay(delayMs.coerceAtLeast(60000), TimeUnit.MILLISECONDS) // 最少 1 分钟
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "coach_notification",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private suspend fun loadQuotes(prefs: AppPreferences): List<String> {
        val json = prefs.coachQuotes.first()
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, listType) ?: defaultQuotes()
        } catch (_: Exception) { defaultQuotes() }
    }

    private fun defaultQuotes(): List<String> = listOf(
        "还差 {deficit} kcal！你是想靠光合作用增重吗？😤",
        "今天才吃 {today} kcal？目标可是 {target} kcal！起来吃！",
        "别找借口了，差 {deficit} kcal，现在去补一顿！💪",
        "增重路上最大的敌人不是代谢，是你的懒惰！还差 {deficit} kcal！",
        "看看你的热量缺口 {deficit} kcal，鸡看了都摇头 🐔",
        "{deficit} kcal 的缺口，这不是增重，这是减肥！醒醒！",
        "你的肌肉在哭泣，它们饿了一天了！还差 {deficit} kcal 😢",
        "目标 {target}，实际 {today}？差距 {deficit}，你自己算算 🙄",
    )

    private fun defaultQuote(): String = "今天的摄入还差一点，赶紧补上！💪"
}
