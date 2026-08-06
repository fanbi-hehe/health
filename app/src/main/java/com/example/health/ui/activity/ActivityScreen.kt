package com.example.health.ui.activity

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.health.data.local.entity.ActivityRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onBack: () -> Unit = {},
    viewModel: ActivityViewModel = viewModel()
) {
    val context = LocalContext.current
    val records by viewModel.records.collectAsState()
    val todayCalories by viewModel.todayCalories.collectAsState()
    val trackState by viewModel.trackState.collectAsState()

    var selectedType by remember { mutableStateOf("running") }
    var showManualDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<ActivityRecord?>(null) }

    // 定位权限申请
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        // 用户授权后自动开始记录（无需再点一次）
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.startTracking(selectedType)
        }
    }
    fun ensureLocationPermission(onGranted: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onGranted() else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("运动记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            // ── 今日消耗 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("今日运动消耗", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "$todayCalories kcal",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = if (trackState.recording) "● 记录中" else "未在记录",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (trackState.recording) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (trackState.recording) {
                RecordingCard(
                    state = trackState,
                    onStop = { viewModel.stopTracking() }
                )
            } else {
                // ── 运动类型选择 ──
                Text("选择运动类型", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "running" to "跑步",
                        "cycling" to "骑行",
                        "walking" to "步行"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { ensureLocationPermission { viewModel.startTracking(selectedType) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("开始记录")
                }
                Text(
                    text = "记录期间请保持屏幕开启或允许后台运行，锁屏后由前台服务继续采集",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showManualDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("手动补录（没戴手环/没开 GPS）")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 历史记录 ──
            Text("历史记录", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无运动记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                records.forEach { record ->
                    ActivityRecordRow(
                        record = record,
                        onDelete = { showDeleteConfirm = record }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // ── 手动补录 ──
    if (showManualDialog) {
        ManualActivityDialog(
            onDismiss = { showManualDialog = false },
            onSave = { minutes, calories, note ->
                viewModel.addManualRecord(minutes, calories, note)
                showManualDialog = false
            }
        )
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

// ── 记录中实时卡片 ──
@Composable
private fun RecordingCard(
    state: GpsTrackController.TrackState,
    onStop: () -> Unit
) {
    val minutes = state.durationSeconds / 60
    val seconds = state.durationSeconds % 60
    val distanceText = if (state.distanceMeters >= 1000) {
        "%.2f km".format(state.distanceMeters / 1000)
    } else {
        "${state.distanceMeters.toInt()} m"
    }
    val paceText = when {
        state.durationSeconds < 10 -> "计算中…"
        state.distanceMeters < 50 -> "等待定位…"
        state.type == "cycling" -> {
            val kmh = state.distanceMeters / 1000 / (state.durationSeconds / 3600.0)
            "%.1f km/h".format(kmh)
        }
        else -> {
            val secPerKm = state.durationSeconds / (state.distanceMeters / 1000)
            "%d'%02d\"".format((secPerKm / 60).toInt(), (secPerKm % 60).toInt())
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("● 记录中（${state.typeLabel()}）", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                StatCell("时长", "%02d:%02d".format(minutes, seconds))
                StatCell("距离", distanceText)
                StatCell("配速", paceText)
                StatCell("预计消耗", "${state.caloriesKcal} kcal")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("结束并保存")
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── 历史记录行 ──
@Composable
private fun ActivityRecordRow(
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

// ── 手动补录对话框 ──
@Composable
private fun ManualActivityDialog(
    onDismiss: () -> Unit,
    onSave: (minutes: Int, calories: Int, note: String?) -> Unit
) {
    var minutes by remember { mutableStateOf("30") }
    var calories by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动补录运动") },
        text = {
            Column {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { v -> if (v.isEmpty() || v.all { it.isDigit() }) minutes = v },
                    label = { Text("时长（分钟）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = calories,
                    onValueChange = { v -> if (v.isEmpty() || v.all { it.isDigit() }) calories = v },
                    label = { Text("消耗（kcal，可参考手环/其他 App）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选，如：健身房）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val m = minutes.toIntOrNull() ?: 0
                    val c = calories.toIntOrNull() ?: 0
                    if (m > 0 && c > 0) onSave(m, c, note.trim().ifBlank { null })
                },
                enabled = (minutes.toIntOrNull() ?: 0) > 0 && (calories.toIntOrNull() ?: 0) > 0
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ── 类型显示辅助 ──
private fun GpsTrackController.TrackState.typeLabel(): String = when (type) {
    "running" -> "跑步"
    "cycling" -> "骑行"
    "walking" -> "步行"
    else -> "运动"
}

private fun ActivityRecord.typeLabel(): String = when (type) {
    "running" -> "跑步"
    "cycling" -> "骑行"
    "walking" -> "步行"
    "manual" -> "手动"
    else -> "运动"
}
