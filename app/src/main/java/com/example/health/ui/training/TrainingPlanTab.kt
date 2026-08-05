package com.example.health.ui.training

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanTab(
    isGenerating: Boolean,
    planJson: String,
    planError: String?,
    todayRecords: List<com.example.health.data.local.entity.TrainingRecord>,
    allExercises: List<com.example.health.data.local.entity.ExerciseLibrary>,
    onGeneratePlan: (customPrompt: String) -> Unit,
    onStartOnboarding: () -> Unit,
    isOnboarded: Boolean,
    onCompleteExercise: (name: String, plannedSets: Int, plannedReps: String, weightKg: Double) -> Unit,
    onAddExercise: (dayIndex: Int) -> Unit = {},
    onDeleteExercise: (dayIndex: Int, exIndex: Int) -> Unit = { _, _ -> },
    onExerciseDetail: (name: String) -> Unit = {}
) {
    val gson = remember { Gson() }
    val plan = remember(planJson) {
        if (planJson.isBlank()) emptyList()
        else try { val t = object : TypeToken<List<DayPlan>>() {}.type; gson.fromJson<List<DayPlan>>(planJson, t) } catch (_: Exception) { emptyList() }
    }

    var customPrompt by remember { mutableStateOf("") }
    var completeDialog by remember { mutableStateOf<PlanExercise?>(null) }
    var showWeekView by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val todayShort = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE).replace("星期", "周")
    val todayIndex = plan.indexOfFirst { it.day == todayShort }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = todayIndex, pageCount = { plan.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()

    fun isCompleted(name: String): Boolean {
        return todayRecords.any { it.date == today.format(DateTimeFormatter.ISO_LOCAL_DATE) && it.exerciseName == name }
    }

    // 完成训练弹窗
    completeDialog?.let { ex ->
        var s by remember(ex) { mutableStateOf(ex.sets.toString()) }
        var r by remember(ex) { mutableStateOf(ex.reps) }
        var w by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { completeDialog = null },
            title = { Text("完成: ${ex.name}") },
            text = {
                Column {
                    Text("计划: ${ex.sets}组 × ${ex.reps}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(s, { s = it }, label = { Text("组数") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(r, { r = it }, label = { Text("次数") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(w, { w = it }, label = { Text("kg") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onCompleteExercise(ex.name, s.toIntOrNull() ?: ex.sets, r, w.toDoubleOrNull() ?: 0.0); completeDialog = null }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { completeDialog = null }) { Text("取消") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("训练计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row { if (!isOnboarded) TextButton(onClick = onStartOnboarding) { Text("设置") } }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Custom prompt + generate
        OutlinedTextField(customPrompt, { customPrompt = it }, placeholder = { Text("自定义需求，如：加强腿部、只用哑铃...") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
        Spacer(modifier = Modifier.height(6.dp))
        Button(onClick = { onGeneratePlan(customPrompt.trim()) }, enabled = !isGenerating && isOnboarded, modifier = Modifier.fillMaxWidth()) {
            if (isGenerating) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Text(" 生成中...", modifier = Modifier.padding(start = 8.dp)) }
            else { Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)); Text(if (plan.isEmpty()) "AI 生成计划" else "重新生成", modifier = Modifier.padding(start = 6.dp)) }
        }
        planError?.let { err -> val ok = err.contains("已使用内置"); Text(err, style = MaterialTheme.typography.bodySmall, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }

        Spacer(modifier = Modifier.height(12.dp))

        if (plan.isEmpty()) {
            if (!isGenerating && isOnboarded) Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("点击上方按钮生成计划\n可在输入框中写自定义需求", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // Day indicator row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    plan.forEachIndexed { i, dp ->
                        val isCurrent = i == pagerState.currentPage
                        val isToday = dp.day == todayShort
                        Box(
                            modifier = Modifier
                                .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                                .padding(horizontal = 2.dp)
                                .size(if (isCurrent) 36.dp else 30.dp)
                                .clip(RoundedCornerShape(if (isCurrent) 10.dp else 8.dp))
                                .background(
                                    when {
                                        isCurrent && isToday -> MaterialTheme.colorScheme.primary
                                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                        isToday -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dp.day.replace("周", ""), style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent && isToday) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                IconButton(onClick = { showWeekView = !showWeekView }) {
                    Icon(Icons.Default.DateRange, if (showWeekView) "日视图" else "周视图", modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (showWeekView) {
                // Week view: compact cards
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    plan.forEachIndexed { i, dp ->
                        WeekDayCard(dp, dp.day == todayShort, ::isCompleted, { onExerciseDetail(it) })
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            } else {
                // Day pager
                val dayIndex = pagerState.currentPage.coerceIn(0, plan.size - 1)
                val dp = plan.getOrNull(dayIndex) ?: return@Column
                Column(Modifier.fillMaxSize()) {
                    Text("${dp.day} · ${dp.date}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(dp.focus, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                        val dayPlan = plan.getOrNull(page) ?: return@HorizontalPager
                        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            dayPlan.exercises.forEachIndexed { i, ex ->
                                val done = isCompleted(ex.name)
                                Row(
                                    Modifier.fillMaxWidth().clickable { onExerciseDetail(ex.name) }.padding(vertical = 6.dp),
                                    Arrangement.SpaceBetween, Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        if (done) { Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(4.dp)) }
                                        Text(ex.name, style = MaterialTheme.typography.bodyLarge,
                                            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { completeDialog = ex }, modifier = Modifier.padding(0.dp)) { Text("完成", style = MaterialTheme.typography.labelSmall) }
                                        Spacer(Modifier.width(2.dp))
                                        IconButton(onClick = { onDeleteExercise(page, i) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Close, "删除", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth().clickable { onAddExercise(page) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                Text(" 添加动作", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDayCard(dp: DayPlan, isToday: Boolean, isCompleted: (String) -> Boolean, onNameClick: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(dp.day, style = MaterialTheme.typography.labelMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(36.dp))
            Text(dp.focus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(70.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dp.exercises.take(4).forEach { ex ->
                    val done = isCompleted(ex.name)
                    Text(ex.name, style = MaterialTheme.typography.labelSmall,
                        color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onNameClick(ex.name) })
                }
                if (dp.exercises.size > 4) Text("+${dp.exercises.size - 4}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
