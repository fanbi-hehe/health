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
    val todayDateStr = today.format(DateTimeFormatter.ofPattern("MM-dd"))
    val todayIndex = plan.indexOfFirst { it.date == todayDateStr }.coerceAtLeast(0)
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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // ── 紧凑顶部栏：标题 + 一键生成 ──
        var showPrompt by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (plan.isEmpty()) "训练计划" else "今日计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row {
                if (plan.isNotEmpty()) {
                    TextButton(onClick = { showPrompt = !showPrompt }, modifier = Modifier.padding(0.dp)) {
                        Text(if (showPrompt) "收起" else "自定义", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (!isOnboarded) TextButton(onClick = onStartOnboarding) { Text("设置", style = MaterialTheme.typography.labelSmall) }
                IconButton(onClick = { onGeneratePlan("") }, enabled = !isGenerating && isOnboarded, modifier = Modifier.size(32.dp)) {
                    if (isGenerating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.AutoAwesome, "生成", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (showPrompt || plan.isEmpty()) {
            OutlinedTextField(customPrompt, { customPrompt = it }, placeholder = { Text("需求：加强腿部、只用哑铃...") },
                modifier = Modifier.fillMaxWidth().height(52.dp), maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                if (plan.isNotEmpty()) TextButton(onClick = { showPrompt = false }) { Text("取消") }
                Button(onClick = { onGeneratePlan(customPrompt.trim()); showPrompt = false },
                    enabled = !isGenerating && isOnboarded, modifier = Modifier.height(36.dp)) {
                    Text(if (plan.isEmpty()) "AI 生成计划" else "重新生成", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        planError?.let { err -> val ok = err.contains("已使用内置"); Text(err, style = MaterialTheme.typography.bodySmall, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 2.dp)) }

        if (plan.isEmpty()) {
            if (!isGenerating && isOnboarded) Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏋️", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("输入需求，点击生成", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // ── 日期指示器（显示 MM-DD 周X，点击切换日） ──
            Column {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                        plan.forEachIndexed { i, dp ->
                            val isCurrent = i == pagerState.currentPage
                            val isToday = dp.date == todayDateStr
                            // 从 "08-06 周三" 格式提取日期短名
                            val shortLabel = dp.date // "08-06"
                            Box(
                                Modifier.clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                                    .padding(horizontal = 3.dp)
                                    .size(32.dp).clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(shortLabel, style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isCurrent || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    IconButton(onClick = { showWeekView = !showWeekView }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DateRange, if (showWeekView) "日" else "周", Modifier.size(16.dp))
                    }
                }
                // 当前日期标签
                val currentPlan = plan.getOrNull(pagerState.currentPage)
                if (currentPlan != null) {
                    Text(currentPlan.day, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }

            if (showWeekView) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    plan.forEachIndexed { i, dp -> WeekDayCard(dp, dp.date == todayDateStr, ::isCompleted, { onExerciseDetail(it) }); Spacer(Modifier.height(4.dp)) }
                }
            } else {
                // ── 核心：大卡片动作列表 ──
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                    val dayPlan = plan.getOrNull(page) ?: return@HorizontalPager
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(4.dp))
                        // 部位标签 + 完整日期
                        Text(dayPlan.focus, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(dayPlan.day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))

                        // 每个动作 = 一张大卡片
                        dayPlan.exercises.forEachIndexed { i, ex ->
                            val done = isCompleted(ex.name)
                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onExerciseDetail(ex.name) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            if (done) { Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(6.dp)) }
                                            Text(ex.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                                                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface)
                                        }
                                        Text("${ex.sets}×${ex.reps}", style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        ex.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        Row {
                                            TextButton(onClick = { completeDialog = ex }, modifier = Modifier.padding(0.dp)) {
                                                Text(if (done) "重做" else "完成", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            IconButton(onClick = { onDeleteExercise(page, i) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, "删除", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // 添加动作
                        TextButton(onClick = { onAddExercise(page) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Text(" 添加动作", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(16.dp))
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
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dp.day, style = MaterialTheme.typography.labelMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                Spacer(Modifier.width(8.dp))
                Text(dp.focus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
