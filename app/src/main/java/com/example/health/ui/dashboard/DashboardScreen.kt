package com.example.health.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.example.health.data.local.entity.AdviceLog
import com.example.health.data.local.entity.BodyWeight
import androidx.compose.ui.graphics.Color
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val weightRecords by viewModel.weightRecords.collectAsState()
    val stepCounts by viewModel.stepCounts.collectAsState()
    val targetWeight by viewModel.targetWeightKg.collectAsState()
    val targetCalories by viewModel.targetDailyCalories.collectAsState()
    val todayCals by viewModel.todayCalories.collectAsState()
    val todayActivityCals by viewModel.todayActivityCalories.collectAsState()
    val todayStepCals by viewModel.todayStepCalories.collectAsState()
    val todayMacros by viewModel.todayMacros.collectAsState()
    val bmr by viewModel.bmr.collectAsState()
    val sevenDayCals by viewModel.sevenDayCalories.collectAsState()
    val trainingRecords by viewModel.trainingRecords.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val reviewState by viewModel.reviewState.collectAsState()
    val adviceLogs by viewModel.adviceLogs.collectAsState()

    var showWeightDialog by remember { mutableStateOf(false) }
    var weightDays by remember { mutableIntStateOf(30) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var showReviewHistory by remember { mutableStateOf(false) }

    // 今日步数（每日步数表按日期倒序）
    val todaySteps = remember(stepCounts) {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        stepCounts.firstOrNull { it.date == todayStr }
    }
    val stepPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.syncSteps() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 数据看板") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // ── 今日热量评估 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("今日热量评估", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("摄入", style = MaterialTheme.typography.bodyMedium)
                        Text("$todayCals / $targetCalories kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("运动消耗", style = MaterialTheme.typography.bodyMedium)
                        Text("$todayActivityCals kcal", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("步数消耗", style = MaterialTheme.typography.bodyMedium)
                        Text("$todayStepCals kcal", style = MaterialTheme.typography.bodyMedium)
                    }
                    val netIntake = todayCals - todayActivityCals - todayStepCals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("净摄入", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("$netIntake kcal", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("宏量营养", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "蛋白 ${todayMacros.proteinG}g · 碳水 ${todayMacros.carbsG}g · 脂肪 ${todayMacros.fatG}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("基础代谢约 $bmr kcal/天", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (targetCalories > 0) (netIntake.toFloat() / targetCalories).coerceIn(0f, 1.5f) else 0f
                    LinearProgressIndicator(progress = { if (progress > 1f) 1f else progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp))
                    val deficit = targetCalories - netIntake
                    if (deficit > 0) {
                        Text("还差 $deficit kcal 达目标，建议加餐",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("热量盈余 ${-deficit} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 近7天热量柱状图 ──
            if (sevenDayCals.isNotEmpty()) {
                Text("近7天摄入", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                CalorieBarChart(
                    dailyData = sevenDayCals,
                    targetCalories = targetCalories,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── 训练档案 ──
            val height by viewModel.userHeight.collectAsState()
            val uWeight by viewModel.userWeight.collectAsState()
            val uAge by viewModel.userAge.collectAsState()
            val uGender by viewModel.userGender.collectAsState()
            val uGoal by viewModel.userGoal.collectAsState()
            val uExp by viewModel.userExperience.collectAsState()
            val uEquip by viewModel.userEquipment.collectAsState()
            val uDays by viewModel.userTrainingDays.collectAsState()
            var showProfileEdit by remember { mutableStateOf(false) }

            Card(modifier = Modifier.fillMaxWidth().clickable { showProfileEdit = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("训练档案", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("编辑", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${uGoal} · ${uExp} · ${uDays}天/周 · ${height}cm ${uWeight}kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (uEquip.isNotBlank()) {
                        Text("器材: $uEquip", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (showProfileEdit) {
                ProfileEditDialog(
                    initialHeight = height.toString(),
                    initialWeight = uWeight.toString(),
                    initialGoal = uGoal,
                    initialExperience = uExp,
                    initialEquipment = uEquip,
                    initialDays = uDays.toString(),
                    initialAge = uAge.toString(),
                    initialGender = uGender,
                    onDismiss = { showProfileEdit = false },
                    onSave = { h, w, g, e, eq, d, age, gender ->
                        viewModel.saveUserProfile(h, w, g, e, eq, d, age, gender)
                        showProfileEdit = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 体重 + 录入 ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("体重趋势", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(7, 30, 90).forEach { d ->
                        FilterChip(selected = weightDays == d, onClick = { weightDays = d },
                            label = { Text("${d}天", style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }

            val filteredWeights = remember(weightRecords, weightDays) {
                val cutoff = LocalDate.now().minusDays(weightDays.toLong())
                weightRecords.filter { it.date >= cutoff.format(DateTimeFormatter.ISO_LOCAL_DATE) }
                    .sortedBy { it.date }
            }

            if (filteredWeights.isNotEmpty()) {
                WeightLineChart(weights = filteredWeights, modifier = Modifier.fillMaxWidth().height(180.dp))
            } else {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("暂无体重数据", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedButton(onClick = { showWeightDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("录入体重", modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 训练概览 ──
            val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
            val weekStart = remember { LocalDate.now().minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE) }
            val thisWeekRecords = remember(trainingRecords) {
                trainingRecords.filter { it.date in weekStart..todayStr }
            }
            val thisWeekDays = remember(thisWeekRecords) { thisWeekRecords.map { it.date }.distinct().size }
            val thisWeekParts = remember(thisWeekRecords) {
                thisWeekRecords.flatMap { it.bodyParts.split(",").map { p -> p.trim() } }
                    .groupingBy { it }.eachCount()
            }

            Text("训练概览", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItem("本周训练", "$thisWeekDays 天")
                        StatItem("总训练次数", "${thisWeekRecords.size} 次")
                    }
                    if (thisWeekParts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("训练部位分布", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            thisWeekParts.entries.take(6).forEach { (part, count) ->
                                Card(colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))) {
                                    Text("$part ×$count", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 今日步数 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("今日步数", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${todaySteps?.steps ?: 0}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold)
                            Text(" 步", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("约 ${todaySteps?.caloriesKcal ?: 0} kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACTIVITY_RECOGNITION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.syncSteps()
                            } else {
                                stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            }
                        }) { Text("同步", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 运动记录入口 ──
            Text("运动记录", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "GPS 记录跑步 / 骑行 / 步行，支持手动补录运动消耗",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigateToActivity,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("进入运动记录")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── AI 每日复盘 ──
            Text("AI 每日复盘", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "基于今日饮食、训练与体重趋势生成个性化建议",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.generateDailyReview() },
                            enabled = reviewState !is ReviewState.Generating,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (reviewState is ReviewState.Generating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                if (reviewState is ReviewState.Generating) "生成中..."
                                else "生成今日评估"
                            )
                        }
                        OutlinedButton(
                            onClick = { showReviewHistory = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("历史评估")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 备份/恢复 ──
            Text("数据管理", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { viewModel.exportAllData() }, modifier = Modifier.weight(1f)) {
                    Text("导出备份")
                }
                OutlinedButton(onClick = { showImportDialog = true }, modifier = Modifier.weight(1f)) {
                    Text("导入恢复")
                }
            }
            backupStatus?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.contains("成功")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    // ── 体重录入弹窗 ──
    if (showWeightDialog) {
        val lastWeight = remember(weightRecords) { weightRecords.firstOrNull()?.weightKg }
        var weightStr by remember(lastWeight) { mutableStateOf(lastWeight?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("录入体重") },
            text = {
                Column {
                    lastWeight?.let {
                        Text("上次: ${it}kg", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(value = weightStr, onValueChange = { weightStr = it },
                        label = { Text("体重 (kg)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))

                    // 快捷调整
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(-0.5, -0.1, 0.1, 0.5).forEach { delta ->
                            val current = weightStr.toDoubleOrNull() ?: (lastWeight ?: 0.0)
                            OutlinedButton(
                                onClick = {
                                    val newVal = (current + delta).coerceAtLeast(0.0)
                                    weightStr = if (delta == (delta.toInt().toDouble()))
                                        "%.0f".format(newVal) else "%.1f".format(newVal)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (delta > 0) "+$delta" else "$delta",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    weightStr.toDoubleOrNull()?.let { viewModel.addWeight(it) }
                    showWeightDialog = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showWeightDialog = false }) { Text("取消") } }
        )
    }

    // ── 导入弹窗 ──
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入数据") },
            text = {
                OutlinedTextField(value = importJson, onValueChange = { importJson = it },
                    label = { Text("粘贴 JSON 数据") }, modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 20)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importData(importJson)
                    showImportDialog = false
                }) { Text("导入") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("取消") } }
        )
    }

    // ── 今日评估结果 ──
    when (val rs = reviewState) {
        is ReviewState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearReviewState() },
                title = { Text("今日评估") },
                text = {
                    Text(
                        text = rs.response,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearReviewState() }) { Text("关闭") }
                }
            )
        }
        is ReviewState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearReviewState() },
                title = { Text("评估失败") },
                text = { Text(rs.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearReviewState() }) { Text("关闭") }
                }
            )
        }
        else -> {}
    }

    // ── 历史评估 ──
    if (showReviewHistory) {
        AlertDialog(
            onDismissRequest = { showReviewHistory = false },
            title = { Text("历史评估") },
            text = {
                if (adviceLogs.isEmpty()) {
                    Text(
                        text = "暂无历史评估",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(adviceLogs, key = { it.id }) { log ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = log.date,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.aiResponse,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReviewHistory = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ──────────────────────────────────────────────────────────
// Vico 近7天热量柱状图（橙=摄入，虚线=目标，蓝=消耗预留）
// ──────────────────────────────────────────────────────────
@Composable
private fun CalorieBarChart(
    dailyData: List<DashboardViewModel.DailyCalorie>,
    targetCalories: Int,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(dailyData, targetCalories) {
        modelProducer.runTransaction {
            // 橙柱：每日实际摄入
            columnSeries {
                series(dailyData.map { it.calories.toFloat() })
            }
            // 虚线：每日目标
            lineSeries {
                series(dailyData.map { targetCalories.toFloat() })
            }
            // 蓝线：后期消耗能量（当前为 0，占位）
            lineSeries {
                series(dailyData.map { 0f })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            rememberLineCartesianLayer(),
            rememberLineCartesianLayer()
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        animationSpec = null
    )
}

// ──────────────────────────────────────────────────────────
// Vico 体重折线图
// ──────────────────────────────────────────────────────────
@Composable
private fun WeightLineChart(weights: List<BodyWeight>, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(weights) {
        modelProducer.runTransaction {
            lineSeries {
                series(weights.map { it.weightKg.toFloat() })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(rememberLineCartesianLayer()),
        modelProducer = modelProducer,
        modifier = modifier,
        animationSpec = null
    )
}

// ──────────────────────────────────────────────────────────
// 训练档案编辑弹窗
// ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditDialog(
    initialHeight: String, initialWeight: String, initialGoal: String,
    initialExperience: String, initialEquipment: String, initialDays: String,
    initialAge: String, initialGender: String,
    onDismiss: () -> Unit,
    onSave: (h: Int, w: Double, g: String, e: String, eq: String, d: Int, age: Int, gender: String) -> Unit
) {
    var h by remember { mutableStateOf(initialHeight) }
    var w by remember { mutableStateOf(initialWeight) }
    var age by remember { mutableStateOf(initialAge) }
    var gender by remember { mutableStateOf(initialGender) }
    var g by remember { mutableStateOf(initialGoal) }
    var e by remember { mutableStateOf(initialExperience) }
    var eq by remember { mutableStateOf(initialEquipment) }
    var d by remember { mutableStateOf(initialDays) }

    val goals = listOf("增重增肌", "减脂塑形", "维持体型", "提升力量")
    val exps = listOf("新手", "有一定基础", "中级", "高级")
    val equipOpts = listOf("哑铃", "杠铃", "固定器械", "绳索", "自重", "壶铃", "弹力带")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("训练档案") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(h, { h = it }, label = { Text("身高 (cm)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(w, { w = it }, label = { Text("体重 (kg)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(age, { age = it }, label = { Text("年龄") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("性别", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("男", "女").forEach { option ->
                        FilterChip(
                            selected = gender == option,
                            onClick = { gender = option },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("目标", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    goals.forEach { goal ->
                        FilterChip(selected = g == goal, onClick = { g = goal },
                            label = { Text(goal, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("经验", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    exps.forEach { exp ->
                        FilterChip(selected = e == exp, onClick = { e = exp },
                            label = { Text(exp, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("器材", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    equipOpts.take(4).forEach { opt ->
                        val sel = opt in eq.split(",")
                        FilterChip(selected = sel, onClick = {
                            val list = eq.split(",").filter { it.isNotBlank() }.toMutableList()
                            if (sel) list.remove(opt) else list.add(opt); eq = list.joinToString(",")
                        }, label = { Text(opt, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    equipOpts.drop(4).forEach { opt ->
                        val sel = opt in eq.split(",")
                        FilterChip(selected = sel, onClick = {
                            val list = eq.split(",").filter { it.isNotBlank() }.toMutableList()
                            if (sel) list.remove(opt) else list.add(opt); eq = list.joinToString(",")
                        }, label = { Text(opt, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(d, { d = it }, label = { Text("每周训练天数") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    h.toIntOrNull() ?: 170,
                    w.toDoubleOrNull() ?: 65.0,
                    g, e, eq,
                    d.toIntOrNull() ?: 4,
                    age.toIntOrNull() ?: 25,
                    gender
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
