package com.example.health.ui.stats

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.health.data.local.entity.ActivityRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 底部"运动"Tab：步数统计 + 运动统计 + 运动记录列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityStatsScreen(
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ActivityStatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val stepCounts by viewModel.stepCounts.collectAsState()
    val records by viewModel.records.collectAsState()
    val todaySteps by viewModel.todaySteps.collectAsState()
    val todayStepCalories by viewModel.todayStepCalories.collectAsState()
    val todayActivityCalories by viewModel.todayActivityCalories.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf<ActivityRecord?>(null) }

    val stepPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.syncSteps() }

    // 近 7 天步数（日期倒序 → 升序展示）
    val recentSteps = remember(stepCounts) { stepCounts.take(7).reversed() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏃 运动") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── 今日概览 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("今日概览", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCell("步数", "$todaySteps")
                        StatCell("步数消耗", "$todayStepCalories kcal")
                        StatCell("运动消耗", "$todayActivityCalories kcal")
                        StatCell("合计", "${todayStepCalories + todayActivityCalories} kcal")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACTIVITY_RECOGNITION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.syncSteps()
                            } else {
                                stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("同步步数", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 开始运动记录 ──
            Button(
                onClick = onNavigateToActivity,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("开始运动记录（跑步/骑行/步行）", modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 本周运动统计 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("近 7 天运动", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCell("次数", "${weekStats.sessions}")
                        StatCell("时长", "${weekStats.minutes} 分钟")
                        StatCell("距离", if (weekStats.distanceMeters >= 1000)
                            "%.2f km".format(weekStats.distanceMeters / 1000)
                        else "${weekStats.distanceMeters.toInt()} m")
                        StatCell("消耗", "${weekStats.calories} kcal")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 近 7 天步数 ──
            Text("近 7 天步数", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            if (recentSteps.isEmpty()) {
                Text("暂无步数数据，点击上方「同步步数」",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        recentSteps.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.date, style = MaterialTheme.typography.bodyMedium)
                                Text("${item.steps} 步 · ${item.caloriesKcal} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (index < recentSteps.size - 1) HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 运动记录列表 ──
            Text("运动记录", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            if (records.isEmpty()) {
                Text("暂无运动记录", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                records.forEach { record ->
                    StatsRecordRow(
                        record = record,
                        onDelete = { showDeleteConfirm = record }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // ── 删除确认 ──
    showDeleteConfirm?.let { record ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除记录") },
            text = { Text("确定删除这条运动记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(record)
                    showDeleteConfirm = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatsRecordRow(
    record: ActivityRecord,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.typeLabel()} · ${timeFormat.format(Date(record.startTime))}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                val detail = buildList {
                    add("${record.durationMinutes} 分钟")
                    if (record.distanceMeters > 0) {
                        add(if (record.distanceMeters >= 1000)
                            "%.2f km".format(record.distanceMeters / 1000)
                        else "${record.distanceMeters.toInt()} m")
                    }
                    record.avgPace?.let { add(it) }
                    record.note?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" · ")
                Text(detail, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${record.caloriesKcal} kcal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun ActivityRecord.typeLabel(): String = when (type) {
    "running" -> "跑步"
    "cycling" -> "骑行"
    "walking" -> "步行"
    "manual" -> "手动"
    else -> "运动"
}
