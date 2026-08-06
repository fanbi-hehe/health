package com.example.health.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import com.example.health.data.local.AppDatabase
import com.example.health.data.preference.AppPreferences
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 桌面小组件：今日热量进度 + 暴躁教练简讯。
 *
 * - 每 30 分钟由系统自动刷新（见 res/xml/calorie_widget_info.xml）
 * - 保存/修改饮食记录时由 [CalorieWidget.Companion.updateAll] 主动刷新
 */
class CalorieWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val calories = AppDatabase.getInstance(context)
            .dietRecordDao()
            .getTotalCaloriesByDate(today) ?: 0
        val target = AppPreferences(context).targetDailyCalories.first()

        provideContent {
            CalorieWidgetContent(calories = calories, target = target)
        }
    }

    companion object {
        /** 主动刷新所有已添加的小组件。 */
        suspend fun updateAll(context: Context) {
            val widget = CalorieWidget()
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(CalorieWidget::class.java).forEach { id ->
                widget.update(context, id)
            }
        }
    }
}

/** 小组件接收器（manifest 注册入口）。 */
class CalorieWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalorieWidget()
}

// ── 小组件内容 ──

private val TextWhite = ColorProvider(0xFFFFFFFF.toInt())
private val TextMuted = ColorProvider(0xFFB0B0B8.toInt())
private val AccentOrange = ColorProvider(0xFFFF9800.toInt())
private val BackgroundDark = ColorProvider(0xFF16121F.toInt())
private val TrackGray = ColorProvider(0xFF33303F.toInt())

@androidx.compose.runtime.Composable
private fun CalorieWidgetContent(calories: Int, target: Int) {
    val percent = if (target > 0) calories * 100 / target else 0
    val progress = if (target > 0) (calories.toFloat() / target).coerceIn(0f, 1f) else 0f

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(12.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥 今日摄入",
                style = TextStyle(
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$calories",
                style = TextStyle(color = TextWhite, fontWeight = FontWeight.Bold)
            )
            Text(
                text = " / $target kcal（$percent%）",
                style = TextStyle(color = TextMuted)
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(8.dp),
            color = AccentOrange,
            backgroundColor = TrackGray
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = if (calories < target) "还差 ${(target - calories).coerceAtLeast(0)} kcal，冲！💪" else "今日目标已达成 ✓",
            style = TextStyle(color = TextMuted)
        )
    }
}
