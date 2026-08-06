package com.example.health.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.DailyStepCount
import com.example.health.data.preference.AppPreferences
import com.example.health.domain.calorie.CalorieCalculator
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

/**
 * 系统步数采集（轻量档，微信同款机制）。
 *
 * 系统计步传感器只提供"开机以来累计值"：
 * - App 打开 / 定时任务触发时调用 [syncNow]；
 * - 当天步数 = 当前累计值 − 当天基线；
 * - 跨天时把上次基线后的差值尽力归到前一天，然后重置基线；
 * - 传感器重启（累计值变小）时自动重置基线，不产生负步数。
 */
object StepCounterManager {

    /** 读取传感器累计值并同步每日步数。 */
    suspend fun syncNow(context: Context) {
        // 未授予身体活动权限时直接跳过（避免每次空等 5 秒超时）
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val sensorTotal = readSensorTotal(context) ?: return
        val prefs = AppPreferences(context)
        val dao = AppDatabase.getInstance(context).dailyStepCountDao()

        val baseTotal = prefs.stepBaseTotal.first()
        val baseDate = prefs.stepBaseDate.first()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val weight = prefs.userCurrentWeight.first()

        if (baseDate == today) {
            // 当天已设基线：正常差值
            val steps = (sensorTotal - baseTotal).coerceAtLeast(0).toInt()
            dao.upsert(
                DailyStepCount(
                    date = today,
                    steps = steps,
                    caloriesKcal = CalorieCalculator.stepCalories(steps, weight)
                )
            )
        } else {
            // 跨天：把上次基线后的差值尽力归到前一天（0 点定时任务没跑时的兜底）
            if (baseDate.isNotBlank() && baseTotal > 0 && sensorTotal >= baseTotal) {
                val yesterdaySteps = (sensorTotal - baseTotal).toInt()
                val prev = dao.getByDate(baseDate)
                val steps = (prev?.steps ?: 0) + yesterdaySteps
                dao.upsert(
                    DailyStepCount(
                        date = baseDate,
                        steps = steps,
                        caloriesKcal = CalorieCalculator.stepCalories(steps, weight)
                    )
                )
            }
            // 重置基线到今天，今天从 0 开始
            prefs.setStepBase(sensorTotal, today)
            dao.upsert(DailyStepCount(date = today, steps = 0, caloriesKcal = 0))
        }
    }

    /** 读取 TYPE_STEP_COUNTER 累计值（带 5 秒超时；传感器缺失或未授权返回 null）。 */
    private suspend fun readSensorTotal(context: Context): Long? = withTimeoutOrNull(5000) {
        suspendCancellableCoroutine { cont ->
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (sensor == null) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    sensorManager.unregisterListener(this)
                    if (cont.isActive) cont.resume(event.values[0].toLong())
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            val registered = sensorManager.registerListener(
                listener, sensor, SensorManager.SENSOR_DELAY_NORMAL
            )
            if (!registered) {
                cont.resume(null)
            } else {
                cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            }
        }
    }
}
