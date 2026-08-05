package com.example.health.ui.dashboard

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.health.data.local.entity.BodyWeight
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val weightRecords by viewModel.weightRecords.collectAsState()
    val targetWeight by viewModel.targetWeightKg.collectAsState()
    val targetCalories by viewModel.targetDailyCalories.collectAsState()
    val todayCals by viewModel.todayCalories.collectAsState()
    val trainingRecords by viewModel.trainingRecords.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()

    var showWeightDialog by remember { mutableStateOf(false) }
    var weightDays by remember { mutableIntStateOf(30) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }

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
            // ── 今日热量进度 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("今日摄入", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$todayCals", style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold)
                        Text(" / $targetCalories kcal", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    val progress = if (targetCalories > 0) (todayCals.toFloat() / targetCalories).coerceIn(0f, 1.5f) else 0f
                    LinearProgressIndicator(progress = { if (progress > 1f) 1f else progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp))
                    if (progress < 1f) {
                        Text("还差 ${targetCalories - todayCals} kcal", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("已达标 ✓", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
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
        var weightStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("录入体重") },
            text = {
                OutlinedTextField(value = weightStr, onValueChange = { weightStr = it },
                    label = { Text("体重 (kg)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
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
        modifier = modifier
    )
}
