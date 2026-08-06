package com.example.health.ui.training

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 组间休息倒计时组件（Canvas 圆环）。
 *
 * 状态由 [RestTimerController] 提供，实际计时运行在 [com.example.health.worker.RestTimerService]
 * 前台服务中：退出页面 / 锁屏后仍继续，通知栏常驻显示，结束播放提示音 + 震动。
 */
@Composable
fun RestTimer(
    modifier: Modifier = Modifier,
    defaultSeconds: Int = 60
) {
    val context = LocalContext.current
    val state by RestTimerController.state.collectAsState()

    // 服务未启动时用默认时长渲染，避免 0 进度闪烁
    val totalSeconds = if (state.totalSeconds > 0) state.totalSeconds else defaultSeconds
    val remainingSeconds = if (state.remainingSeconds >= 0) state.remainingSeconds else totalSeconds
    val isRunning = state.running

    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f,
        animationSpec = tween(300)
    )

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "组间休息",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.size(8.dp))

            val arcActiveColor = MaterialTheme.colorScheme.primary
            val arcEndColor = MaterialTheme.colorScheme.error
            // 圆环倒计时
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )
                    val arcSize = Size(radius * 2, radius * 2)

                    // 背景圆环
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // 进度弧
                    drawArc(
                        color = if (remainingSeconds > 10) arcActiveColor else arcEndColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (remainingSeconds > 10)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            // 控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    if (!isRunning) RestTimerController.adjust(context, -5, totalSeconds)
                }) { Text("-5s", style = MaterialTheme.typography.labelMedium) }

                TextButton(onClick = {
                    if (isRunning) {
                        RestTimerController.pause(context)
                    } else {
                        // 剩余为 0 时重新开始
                        if (remainingSeconds <= 0) {
                            RestTimerController.start(context, totalSeconds)
                        } else {
                            RestTimerController.resume(context)
                        }
                    }
                }) {
                    Text(
                        if (isRunning) "⏸ 暂停" else "▶ 开始",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = { RestTimerController.reset(context) }) {
                    Text("↺ 重置", style = MaterialTheme.typography.labelMedium)
                }

                TextButton(onClick = {
                    if (!isRunning) RestTimerController.adjust(context, 5, totalSeconds)
                }) { Text("+5s", style = MaterialTheme.typography.labelMedium) }
            }

            // 结束（停止服务与通知）
            TextButton(onClick = { RestTimerController.stop(context) }) {
                Text("结束", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
