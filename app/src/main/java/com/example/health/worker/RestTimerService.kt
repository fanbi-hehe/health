package com.example.health.worker

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.health.ui.training.RestTimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 组间休息倒计时前台服务。
 *
 * 职责：
 * - 常驻后台倒计时，退出页面 / 锁屏后继续
 * - 每秒更新通知栏剩余时间
 * - 结束时播放系统提示音 + 震动 + 发结束通知
 * - 通过 [RestTimerController] 与 UI 同步状态
 */
class RestTimerService : Service() {

    companion object {
        const val ACTION_START = "com.example.health.resttimer.START"
        const val ACTION_PAUSE = "com.example.health.resttimer.PAUSE"
        const val ACTION_RESUME = "com.example.health.resttimer.RESUME"
        const val ACTION_ADJUST = "com.example.health.resttimer.ADJUST"
        const val ACTION_RESET = "com.example.health.resttimer.RESET"
        const val ACTION_STOP = "com.example.health.resttimer.STOP"
        const val EXTRA_SECONDS = "extra_seconds"

        private const val NOTIFICATION_ID = 2001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null

    private var totalSeconds = 60
    private var remainingSeconds = 60
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                totalSeconds = intent.getIntExtra(EXTRA_SECONDS, 60).coerceIn(15, 180)
                remainingSeconds = totalSeconds
                running = true
                startTimerForeground()
                startTicking()
            }
            ACTION_PAUSE -> {
                running = false
                tickJob?.cancel()
                syncStateAndNotification()
            }
            ACTION_RESUME -> {
                if (remainingSeconds > 0) {
                    running = true
                    startTicking()
                }
            }
            ACTION_ADJUST -> {
                val newTotal = intent.getIntExtra(EXTRA_SECONDS, totalSeconds).coerceIn(15, 180)
                totalSeconds = newTotal
                remainingSeconds = newTotal
                running = false
                tickJob?.cancel()
                syncStateAndNotification()
            }
            ACTION_RESET -> {
                remainingSeconds = totalSeconds
                running = false
                tickJob?.cancel()
                syncStateAndNotification()
            }
            ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimerForeground() {
        try {
            val notification = NotificationHelper.buildRestTimerNotification(
                this, remainingSeconds, totalSeconds, running
            )
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // 未授予通知权限等场景：仍继续计时，只是不显示前台通知
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        syncStateAndNotification()
        tickJob = scope.launch {
            while (running && remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
                syncStateAndNotification()
                if (remainingSeconds <= 0) {
                    onFinished()
                    break
                }
            }
        }
    }

    private fun syncStateAndNotification() {
        RestTimerController.updateState(
            RestTimerController.State(
                running = running,
                remainingSeconds = remainingSeconds.coerceAtLeast(0),
                totalSeconds = totalSeconds
            )
        )
        try {
            val notification = NotificationHelper.buildRestTimerNotification(
                this, remainingSeconds.coerceAtLeast(0), totalSeconds, running
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // 忽略通知更新失败（如权限被拒）
        }
    }

    /** 倒计时结束：提示音 + 震动 + 结束通知 + 停止服务。 */
    private fun onFinished() {
        running = false
        remainingSeconds = 0
        RestTimerController.updateState(
            RestTimerController.State(
                running = false,
                remainingSeconds = 0,
                totalSeconds = totalSeconds
            )
        )
        playFinishFeedback()
        NotificationHelper.send(this, "组间休息结束 🔔", "休息结束，起来做下一组吧！")
        stopTimer()
    }

    private fun playFinishFeedback() {
        // 系统提示音
        try {
            val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone: Ringtone? = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
        } catch (_: Exception) {
        }

        // 震动
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun stopTimer() {
        tickJob?.cancel()
        running = false
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        super.onDestroy()
    }
}
