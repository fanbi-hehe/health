package com.example.health.ui.training

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

@Composable
fun RestTimer(
    modifier: Modifier = Modifier,
    defaultSeconds: Int = 60
) {
    val context = LocalContext.current
    var totalSeconds by remember { mutableStateOf(defaultSeconds) }
    var remainingSeconds by remember { mutableStateOf(defaultSeconds) }
    var isRunning by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f,
        animationSpec = tween(300)
    )

    // 倒计时逻辑
    LaunchedEffect(isRunning) {
        if (isRunning && remainingSeconds > 0) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            // 倒计时结束 → 震动
            if (remainingSeconds == 0) {
                isRunning = false
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = context.getSystemService(
                            android.content.Context.VIBRATOR_MANAGER_SERVICE
                        ) as? VibratorManager
                        vibratorManager?.defaultVibrator?.vibrate(
                            VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        val vibrator = context.getSystemService(
                            android.content.Context.VIBRATOR_SERVICE
                        ) as? Vibrator
                        vibrator?.vibrate(
                            VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

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
            Text("组间休息", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer)

            Spacer(modifier = Modifier.height(8.dp))

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
                        color = if (remainingSeconds > 10)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
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

            Spacer(modifier = Modifier.height(8.dp))

            // 控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 时间调节
                TextButton(onClick = {
                    if (!isRunning) {
                        totalSeconds = (totalSeconds - 5).coerceAtLeast(15)
                        remainingSeconds = totalSeconds
                    }
                }) { Text("-5s", style = MaterialTheme.typography.labelMedium) }

                TextButton(onClick = {
                    if (isRunning) {
                        // 暂停
                        isRunning = false
                    } else {
                        // 开始 / 继续
                        if (remainingSeconds <= 0) {
                            remainingSeconds = totalSeconds
                        }
                        isRunning = true
                    }
                }) {
                    Text(
                        if (isRunning) "⏸ 暂停" else "▶ 开始",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 重置
                TextButton(onClick = {
                    isRunning = false
                    remainingSeconds = totalSeconds
                }) { Text("↺ 重置", style = MaterialTheme.typography.labelMedium) }

                TextButton(onClick = {
                    if (!isRunning) {
                        totalSeconds = (totalSeconds + 5).coerceAtMost(180)
                        remainingSeconds = totalSeconds
                    }
                }) { Text("+5s", style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}
