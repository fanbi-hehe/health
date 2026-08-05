package com.example.health.ui.training

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.health.data.local.entity.ExerciseLibrary
import com.example.health.data.local.entity.TrainingRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: TrainingViewModel = viewModel()
) {
    val todayRecords by viewModel.todayRecords.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏋️ 训练记录") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // 休息倒计时按钮
                ExtendedFloatingActionButton(
                    onClick = { showTimer = !showTimer },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "休息") },
                    text = { Text(if (showTimer) "隐藏计时器" else "组间休息") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "添加") },
                    text = { Text("记录训练") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 休息倒计时 ──
            if (showTimer) {
                RestTimer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ── 今日训练列表 ──
            if (todayRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "今天还没有训练记录\n点击右下角开始记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 按部位分组显示
                val grouped = todayRecords.groupBy { it.bodyParts }
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    grouped.forEach { (bodyParts, records) ->
                        item {
                            Text(
                                text = bodyParts,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(records, key = { it.id }) { record ->
                            TrainingRecordCard(record)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── 录入弹窗 ──
    if (showAddDialog) {
        AddTrainingDialog(
            allExercises = allExercises,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onSaved = { showAddDialog = false }
        )
    }
}

// ──────────────────────────────────────────────────────────
// 训练记录卡片
// ──────────────────────────────────────────────────────────
@Composable
private fun TrainingRecordCard(record: TrainingRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.exerciseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
                Text("${record.sets}组 × ${record.reps}次  ${record.weightKg}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                record.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// 录入训练弹窗
// ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTrainingDialog(
    allExercises: List<ExerciseLibrary>,
    viewModel: TrainingViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val bodyPartOptions = listOf("胸", "背", "腿", "肩", "手臂", "核心")
    var selectedBodyParts by remember { mutableStateOf(setOf<String>()) }

    var exerciseName by remember { mutableStateOf("") }
    var showExerciseSuggestions by remember { mutableStateOf(false) }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var weightKg by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val historyExercises by viewModel.historyExercises.collectAsState()

    // 合并动作库 + 历史推荐 → 去重
    val exerciseSuggestions = buildList {
        // 自定义优先 + 内置
        addAll(allExercises.filter {
            it.name.contains(exerciseName, ignoreCase = true) ||
            selectedBodyParts.any { bp -> it.bodyPart.contains(bp, ignoreCase = true) }
        })
        // 历史动作补充
        historyExercises.filter { hist ->
            hist.contains(exerciseName, ignoreCase = true) &&
            none { it.name.equals(hist, ignoreCase = true) }
        }.forEach { hist ->
            add(ExerciseLibrary(name = hist, bodyPart = "", isCustom = false))
        }
    }.take(8)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录训练") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // ── 部位多选 ──
                Text("训练部位", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    bodyPartOptions.take(3).forEach { part ->
                        FilterChip(
                            selected = part in selectedBodyParts,
                            onClick = {
                                selectedBodyParts = if (part in selectedBodyParts) {
                                    selectedBodyParts - part
                                } else {
                                    viewModel.loadHistoryExercises(part)
                                    selectedBodyParts + part
                                }
                            },
                            label = { Text(part, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    bodyPartOptions.drop(3).forEach { part ->
                        FilterChip(
                            selected = part in selectedBodyParts,
                            onClick = {
                                selectedBodyParts = if (part in selectedBodyParts) {
                                    selectedBodyParts - part
                                } else {
                                    viewModel.loadHistoryExercises(part)
                                    selectedBodyParts + part
                                }
                            },
                            label = { Text(part, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 动作名称 + 自动补全 ──
                Box {
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = {
                            exerciseName = it
                            showExerciseSuggestions = it.isNotEmpty()
                        },
                        label = { Text("动作名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = showExerciseSuggestions && exerciseSuggestions.isNotEmpty(),
                        onDismissRequest = { showExerciseSuggestions = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        exerciseSuggestions.forEach { exercise ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(exercise.name, fontWeight = FontWeight.Medium)
                                        if (exercise.bodyPart.isNotEmpty()) {
                                            Text(" · ${exercise.bodyPart}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = {
                                    exerciseName = exercise.name
                                    showExerciseSuggestions = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 组数 / 次数 / 重量 ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = { Text("组数") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("次数") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        label = { Text("重量(kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 备注 ──
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（可选 RPE 等）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = exerciseName.trim()
                    val parts = selectedBodyParts.toList()
                    val setCount = sets.toIntOrNull() ?: 3
                    val repCount = reps.toIntOrNull() ?: 10
                    val weight = weightKg.toDoubleOrNull() ?: 0.0
                    if (name.isNotEmpty() && parts.isNotEmpty()) {
                        viewModel.saveRecord(parts, name, setCount, repCount, weight,
                            notes.ifBlank { null })
                        onSaved()
                    }
                },
                enabled = exerciseName.isNotBlank() && selectedBodyParts.isNotEmpty()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
