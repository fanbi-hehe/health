package com.example.health.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class DayPlan(
    val day: String,          // "周一", "周二" ...
    val date: String,         // "08-11"
    val focus: String,        // "胸+三头" / "休息日"
    val exercises: List<PlanExercise> = emptyList()
)

data class PlanExercise(
    val name: String,
    val sets: Int,
    val reps: String,
    val notes: String? = null
)

@Composable
fun TrainingPlanTab(
    isGenerating: Boolean,
    planJson: String,
    onGeneratePlan: () -> Unit,
    onStartOnboarding: () -> Unit,
    isOnboarded: Boolean
) {
    val gson = remember { Gson() }
    val plan = remember(planJson) {
        if (planJson.isBlank()) emptyList()
        else try {
            val type = object : TypeToken<List<DayPlan>>() {}.type
            gson.fromJson<List<DayPlan>>(planJson, type)
        } catch (_: Exception) { emptyList() }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // 顶部操作
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("训练计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!isOnboarded) {
                TextButton(onClick = onStartOnboarding) { Text("设置档案") }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // AI 生成按钮
        Button(
            onClick = onGeneratePlan,
            enabled = !isGenerating && isOnboarded,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(" AI 生成中...", modifier = Modifier.padding(start = 8.dp))
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(if (plan.isEmpty()) "AI 生成训练计划" else "AI 重新生成", modifier = Modifier.padding(start = 6.dp))
            }
        }

        if (!isOnboarded) {
            Text("请先设置训练档案", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 周历视图
        if (plan.isNotEmpty()) {
            val weekDays = plan.map { it.day }
            val today = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
            // Normalize: "星期一" -> "周一"
            val todayShort = today.replace("星期", "周")

            plan.forEach { dayPlan ->
                val isToday = dayPlan.day == todayShort
                DayPlanCard(dayPlan = dayPlan, isToday = isToday)
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else if (!isGenerating && isOnboarded) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("点击上方按钮，AI 将根据你的档案和训练历史\n为你定制一周训练计划",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayPlanCard(dayPlan: DayPlan, isToday: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dayPlan.day, style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(dayPlan.date, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                Text(dayPlan.focus, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }

            if (dayPlan.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                dayPlan.exercises.forEach { ex ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ex.name, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${ex.sets}×${ex.reps}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
