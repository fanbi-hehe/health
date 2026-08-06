package com.example.health.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.health.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "coach_notification"
    const val CHANNEL_NAME = "暴躁教练"
    const val REST_TIMER_CHANNEL_ID = "rest_timer"
    const val REST_TIMER_CHANNEL_NAME = "组间休息倒计时"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "每日饮食提醒与激励"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            // 倒计时通知通道：高优先级但静音、不震动，适合常驻显示
            val restChannel = NotificationChannel(
                REST_TIMER_CHANNEL_ID,
                REST_TIMER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "训练组间休息倒计时（常驻通知）"
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(restChannel)
        }
    }

    fun send(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }

    /**
     * 构建倒计时常驻通知（含"结束"操作按钮）。
     */
    fun buildRestTimerNotification(
        context: Context,
        remainingSeconds: Int,
        totalSeconds: Int,
        running: Boolean
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 通知上的"结束"按钮 → 停止服务
        val stopIntent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeText = "%02d:%02d".format(minutes, seconds)

        return NotificationCompat.Builder(context, REST_TIMER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (running) "组间休息中" else "休息已暂停")
            .setContentText("剩余 $timeText / ${totalSeconds} 秒")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(0, "结束", stopPendingIntent)
            .build()
    }
}
