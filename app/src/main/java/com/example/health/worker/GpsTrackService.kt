package com.example.health.worker

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ActivityRecord
import com.example.health.data.preference.AppPreferences
import com.example.health.domain.calorie.CalorieCalculator
import com.example.health.ui.activity.GpsTrackController
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GPS 户外运动记录前台服务。
 *
 * - 前台服务类型 location，锁屏后继续采集
 * - 每秒更新时长与预估消耗，每收到定位点累加距离
 * - 结束时将轨迹与统计写入 ActivityRecord 表
 */
class GpsTrackService : Service(), LocationListener {

    companion object {
        const val ACTION_START = "com.example.health.gpstrack.START"
        const val ACTION_STOP = "com.example.health.gpstrack.STOP"
        const val EXTRA_TYPE = "extra_type"

        private const val NOTIFICATION_ID = 2002
        private const val MIN_TIME_MS = 1000L
        private const val MIN_DISTANCE_M = 5f
        private const val MAX_POINTS = 8000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null
    private var locationManager: LocationManager? = null
    private val gson = Gson()

    private var type = "running"
    private var startTime = 0L
    private var durationSeconds = 0
    private var distanceMeters = 0.0
    private var weightKg = 65.0
    private var hasLastPoint = false
    private var lastLat = 0.0
    private var lastLon = 0.0
    private var finishing = false
    private val points = mutableListOf<TrackPoint>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                type = intent.getStringExtra(EXTRA_TYPE) ?: "running"
                startTime = System.currentTimeMillis()
                durationSeconds = 0
                distanceMeters = 0.0
                hasLastPoint = false
                points.clear()
                startForegroundAndTracking()
            }
            ACTION_STOP -> {
                finishAndSave()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundAndTracking() {
        scope.launch {
            weightKg = AppPreferences(this@GpsTrackService).userCurrentWeight.first()
            // 体重读取完成后刷新一次消耗显示（避免长时间停留在默认体重估算）
            syncStateAndNotification()
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (_: Exception) {
            // 前台服务启动失败（如权限/系统限制）：立即停止，避免后台存活被系统强杀
            stopSelf()
            return
        }
        startLocationUpdates()
        syncStateAndNotification()
        tickJob = scope.launch {
            while (true) {
                delay(1000L)
                durationSeconds++
                syncStateAndNotification()
            }
        }
    }

    private fun startLocationUpdates() {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = manager
        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) return

        try {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_M, this
            )
        } catch (_: Exception) {
            try {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_M, this
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        if (hasLastPoint) {
            distanceMeters += haversineMeters(lastLat, lastLon, lat, lon)
        } else {
            hasLastPoint = true
        }
        lastLat = lat
        lastLon = lon
        points.add(
            TrackPoint(
                lat = lat,
                lon = lon,
                time = location.time,
                altitude = location.altitude
            )
        )
        if (points.size > MAX_POINTS) {
            points.removeAt(0)
        }
        syncStateAndNotification()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    private fun syncStateAndNotification() {
        val minutes = (durationSeconds + 59) / 60
        GpsTrackController.updateState(
            GpsTrackController.TrackState(
                recording = true,
                type = type,
                startTime = startTime,
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
                caloriesKcal = CalorieCalculator.gpsActivityCalories(type, weightKg, minutes),
                pointCount = points.size
            )
        )
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(): android.app.Notification {
        return NotificationHelper.buildGpsTrackNotification(
            this, typeLabel(), durationSeconds, distanceMeters
        )
    }

    private fun finishAndSave() {
        if (finishing) return // 防双击结束导致重复入库
        finishing = true
        tickJob?.cancel()
        locationManager?.removeUpdates(this)

        val minutes = (durationSeconds + 59) / 60
        val calories = CalorieCalculator.gpsActivityCalories(type, weightKg, minutes)
        val pace = buildPaceText()

        scope.launch {
            try {
                AppDatabase.getInstance(this@GpsTrackService).activityRecordDao().insert(
                    ActivityRecord(
                        type = type,
                        startTime = startTime,
                        durationMinutes = minutes,
                        caloriesKcal = calories,
                        distanceMeters = distanceMeters,
                        avgPace = pace,
                        routeJson = gson.toJson(points),
                        source = "gps"
                    )
                )
            } catch (_: Exception) {
            }

            GpsTrackController.updateState(GpsTrackController.TrackState())
            NotificationHelper.send(
                this@GpsTrackService,
                "${typeLabel()}结束",
                "时长 ${minutes} 分钟 · 距离 ${"%.2f".format(distanceMeters / 1000)} km · 消耗约 $calories kcal"
            )
            stopTimer()
        }
    }

    private fun buildPaceText(): String? {
        if (distanceMeters < 50 || durationSeconds < 30) return null
        val km = distanceMeters / 1000
        return when (type) {
            "running", "walking" -> {
                val secPerKm = durationSeconds / km
                "%d'%02d\"".format((secPerKm / 60).toInt(), (secPerKm % 60).toInt())
            }
            "cycling" -> {
                val kmh = km / (durationSeconds / 3600.0)
                "%.1f km/h".format(kmh)
            }
            else -> null
        }
    }

    private fun typeLabel(): String = when (type) {
        "running" -> "跑步"
        "cycling" -> "骑行"
        "walking" -> "步行"
        else -> "运动"
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * asin(sqrt(a))
    }

    private fun stopTimer() {
        tickJob?.cancel()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        locationManager?.removeUpdates(this)
        super.onDestroy()
    }

    /** 轨迹点（JSON 入库，地图后置）。 */
    data class TrackPoint(
        val lat: Double,
        val lon: Double,
        val time: Long,
        val altitude: Double
    )
}
