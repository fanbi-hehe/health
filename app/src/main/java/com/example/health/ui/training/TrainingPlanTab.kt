package com.example.health.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class DayPlan(
    val day: String,
    val date: String,
    val focus: String,
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
    planError: String?,
    todayRecords: List<com.example.health.data.local.entity.TrainingRecord>,
    onGeneratePlan: (customPrompt: String) -> Unit,
    onStartOnboarding: () -> Unit,
    isOnboarded: Boolean,
    onCompleteExercise: (name: String, plannedSets: Int, plannedReps: String, weightKg: Double) -> Unit
) {
    val gson = remember { Gson() }
    val plan = remember(planJson) {
        if (planJson.isBlank()) emptyList()
        else try {
            val type = object : TypeToken<List<DayPlan>>() {}.type
            gson.fromJson<List<DayPlan>>(planJson, type)
        } catch (_: Exception) { emptyList() }
    }

    var customPrompt by remember { mutableStateOf("") }
    var completeDialog by remember { mutableStateOf<PlanExercise?>(null) }
    val today = LocalDate.now()

    // 完成训练弹窗
    completeDialog?.let { ex ->
        var actualSets by remember(ex) { mutableStateOf(ex.sets.toString()) }
        var actualReps by remember(ex) { mutableStateOf(ex.reps) }
        var actualWeight by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { completeDialog = null },
            title = { Text("完成训练: ${ex.name}") },
            text = {
                Column {
                    Text("计划: ${ex.sets}组 × ${ex.reps}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(actualSets, { actualSets = it }, label = { Text("组数") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f))
                        OutlinedTextField(actualReps, { actualReps = it }, label = { Text("次数") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f))
                        OutlinedTextField(actualWeight, { actualWeight = it }, label = { Text("重量kg") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCompleteExercise(ex.name,
                        actualSets.toIntOrNull() ?: ex.sets,
                        actualReps,
                        actualWeight.toDoubleOrNull() ?: 0.0)
                    completeDialog = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { completeDialog = null }) { Text("取消") } }
        )
    }

    // 检查某个动作今天是否已完成
    fun isCompleted(name: String): Boolean {
        return todayRecords.any { it.date == today.format(DateTimeFormatter.ISO_LOCAL_DATE) && it.exerciseName == name }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("训练计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!isOnboarded) TextButton(onClick = onStartOnboarding) { Text("设置档案") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 自定义需求输入框
        OutlinedTextField(
            value = customPrompt,
            onValueChange = { customPrompt = it },
            placeholder = { Text("自定义需求，如：我想加强腿部、用哑铃为主...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onGeneratePlan(customPrompt.trim()) },
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
        planError?.let { err ->
            val isFallback = err.contains("已使用内置")
            Text(err, style = MaterialTheme.typography.bodySmall,
                color = if (isFallback) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (plan.isNotEmpty()) {
            val todayShort = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE).replace("星期", "周")
            plan.forEach { dayPlan ->
                val isToday = dayPlan.day == todayShort
                DayPlanCard(
                    dayPlan = dayPlan,
                    isToday = isToday,
                    isCompleted = ::isCompleted,
                    onExerciseClick = { ex -> completeDialog = ex }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else if (!isGenerating && isOnboarded) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("点击上方按钮生成计划\n可在输入框中写自定义需求",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayPlanCard(
    dayPlan: DayPlan,
    isToday: Boolean,
    isCompleted: (String) -> Boolean,
    onExerciseClick: (PlanExercise) -> Unit
) {
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
                    val done = isCompleted(ex.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExerciseClick(ex) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (done) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(ex.name, style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface)
                        }
                        Text("${ex.sets}×${ex.reps}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (done) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
