package com.example.health.ui.training

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.health.ui.components.DropdownItem
import com.example.health.ui.components.ScrollableDropdown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
    onNavigateToExerciseDetail: (String) -> Unit = {},
    viewModel: TrainingViewModel = viewModel()
) {
    val todayRecords by viewModel.todayRecords.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val planGenerating by viewModel.isGenerating.collectAsState()
    val planJson by viewModel.planJson.collectAsState()
    val isOnboarded by viewModel.isOnboarded.collectAsState()
    val planError by viewModel.planError.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditTrainingDialog by remember { mutableStateOf<TrainingRecord?>(null) }
    var showTimer by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=计划, 1=记录, 2=动作库
    var exerciseSearchQuery by remember { mutableStateOf("") }
    var showOnboarding by remember { mutableStateOf(false) }
    // 折叠状态：记录每个"部位_器械组"是否展开，器械组默认折叠
    val expandedEquipGroups = remember { mutableStateMapOf<String, Boolean>() }

    // 动作库过滤
    val filteredExercises = if (exerciseSearchQuery.isBlank()) {
        allExercises
    } else {
        allExercises.filter { it.name.contains(exerciseSearchQuery, ignoreCase = true) }
    }
    // 按部位 → 器械类型 两级分组
    val groupedExercises = remember(filteredExercises) {
        filteredExercises
            .groupBy { it.bodyPart.ifBlank { "其他" } }
            .mapValues { (_, exercises) ->
                exercises.groupBy { equipmentGroup(it.equipment) }
            }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("🏋️ 训练") },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("计划", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("记录", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Text("动作库", modifier = Modifier.padding(12.dp))
                    }
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (selectedTab == 0) {
                    ExtendedFloatingActionButton(
                        onClick = { showTimer = !showTimer },
                        icon = { Icon(Icons.Default.Timer, contentDescription = "休息") },
                        text = { Text(if (showTimer) "隐藏计时" else "组间休息") },
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 休息倒计时 ──
            if (showTimer && selectedTab == 0) {
                RestTimer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (selectedTab == 0) {
                // ── 训练计划页 ──
                TrainingPlanTab(
                    isGenerating = planGenerating,
                    planJson = planJson,
                    planError = planError,
                    todayRecords = todayRecords,
                    allExercises = allExercises,
                    onGeneratePlan = { prompt -> viewModel.generatePlan(prompt) },
                    onStartOnboarding = { showOnboarding = true },
                    isOnboarded = isOnboarded,
                    onCompleteExercise = { name, sets, reps, w ->
                        viewModel.completePlanExercise(name, sets, reps, w)
                    },
                    onAddExercise = { day -> viewModel.addPlanExercise(day) },
                    onDeleteExercise = { day, ex -> viewModel.removePlanExercise(day, ex) },
                    onExerciseDetail = { name -> onNavigateToExerciseDetail(name) }
                )
            } else if (selectedTab == 1) {
                // ── 训练记录页 ──
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
                    val grouped = todayRecords.groupBy { it.bodyParts }
                    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                        grouped.forEach { (bodyParts, records) ->
                            item {
                                Text(bodyParts, style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                            }
                            items(records, key = { it.id }) { record ->
                                TrainingRecordCard(
                                    record = record,
                                    onClick = { showEditTrainingDialog = record }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            } else {
                // ── 动作库浏览页 ──
                Column(modifier = Modifier.fillMaxSize()) {
                    // 搜索栏
                    OutlinedTextField(
                        value = exerciseSearchQuery,
                        onValueChange = { exerciseSearchQuery = it },
                        placeholder = { Text("搜索动作...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (filteredExercises.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无动作数据", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            groupedExercises.forEach { (bodyPart, equipmentGroups) ->
                                val totalCount = equipmentGroups.values.sumOf { it.size }
                                val isSearching = exerciseSearchQuery.isNotBlank()

                                // 部位标题（始终展开，不可折叠）
                                item(key = "header_$bodyPart") {
                                    Text(
                                        "$bodyPart ($totalCount)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                                    )
                                }

                                equipmentGroups.forEach { (equipGroup, exercises) ->
                                    val equipKey = "${bodyPart}_$equipGroup"
                                    val isExpanded = isSearching || (expandedEquipGroups[equipKey] ?: false)

                                    // 器械组标题（可点击折叠）
                                    item(key = "sub_$equipKey") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (!isSearching) {
                                                        expandedEquipGroups[equipKey] = !isExpanded
                                                    }
                                                }
                                                .padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded)
                                                    Icons.Default.KeyboardArrowUp
                                                else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "$equipGroup (${exercises.size})",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 2.dp)
                                            )
                                        }
                                    }

                                    // 展开后显示动作列表
                                    if (isExpanded) {
                                        items(exercises, key = { it.id }) { exercise ->
                                            ExerciseLibraryCard(
                                                exercise = exercise,
                                                onClick = { onNavigateToExerciseDetail(exercise.name) }
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
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
    showEditTrainingDialog?.let { record ->
        EditTrainingDialog(
            record = record,
            allExercises = allExercises,
            viewModel = viewModel,
            onDismiss = { showEditTrainingDialog = null },
            onSaved = { showEditTrainingDialog = null }
        )
    }
    if (showOnboarding) OnboardingDialog(viewModel, onDismiss = { showOnboarding = false })
}

// ──────────────────────────────────────────────────────────
// 训练记录卡片
// ──────────────────────────────────────────────────────────
@Composable
private fun TrainingRecordCard(record: TrainingRecord, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                Text(record.exerciseName, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
                Text("${record.sets}组 × ${record.reps}次  ${record.weightKg}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                record.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        }
    }
}

// ──────────────────────────────────────────────────────────
// 动作库卡片
// ──────────────────────────────────────────────────────────
@Composable
private fun ExerciseLibraryCard(exercise: ExerciseLibrary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                Text(exercise.name, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (exercise.equipment.isNotEmpty()) {
                        Text(exercise.equipment, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (exercise.target.isNotEmpty()) {
                        Text("· ${exercise.target}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (exercise.gifUrl.isNotEmpty()) {
                Text("🎬", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// 器械分类
// ──────────────────────────────────────────────────────────
private fun equipmentGroup(equipment: String): String = when {
    equipment == "自重" -> "自重"
    equipment in listOf("哑铃") -> "哑铃"
    equipment in listOf("杠铃", "EZ杠铃", "六角杠") -> "杠铃"
    equipment in listOf("绳索") -> "绳索"
    equipment in listOf("固定器械", "史密斯机", "腿举机") -> "固定器械"
    equipment.isNotEmpty() -> "其他器械"
    else -> "其他"
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

    val exerciseSuggestions = buildList {
        addAll(allExercises.filter {
            it.name.contains(exerciseName, ignoreCase = true) ||
            selectedBodyParts.any { bp -> it.bodyPart.contains(bp, ignoreCase = true) }
        })
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("训练部位", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    bodyPartOptions.take(3).forEach { part ->
                        FilterChip(selected = part in selectedBodyParts, onClick = {
                            selectedBodyParts = if (part in selectedBodyParts) {
                                selectedBodyParts - part
                            } else {
                                viewModel.loadHistoryExercises(part)
                                selectedBodyParts + part
                            }
                        }, label = { Text(part, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    bodyPartOptions.drop(3).forEach { part ->
                        FilterChip(selected = part in selectedBodyParts, onClick = {
                            selectedBodyParts = if (part in selectedBodyParts) {
                                selectedBodyParts - part
                            } else {
                                viewModel.loadHistoryExercises(part)
                                selectedBodyParts + part
                            }
                        }, label = { Text(part, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box {
                    OutlinedTextField(value = exerciseName, onValueChange = {
                        exerciseName = it; showExerciseSuggestions = it.isNotEmpty()
                    }, label = { Text("动作名称") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    ScrollableDropdown(
                        expanded = showExerciseSuggestions && exerciseSuggestions.isNotEmpty(),
                        onDismiss = { showExerciseSuggestions = false },
                        modifier = Modifier.fillMaxWidth(),
                        items = exerciseSuggestions.map { exercise ->
                            DropdownItem(
                                key = "ex_${exercise.name}",
                                content = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(exercise.name, fontWeight = FontWeight.Medium)
                                        if (exercise.bodyPart.isNotEmpty()) {
                                            Text(" · ${exercise.bodyPart}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = { exerciseName = exercise.name }
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = sets, onValueChange = { sets = it },
                        label = { Text("组数") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(value = reps, onValueChange = { reps = it },
                        label = { Text("次数") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(value = weightKg, onValueChange = { weightKg = it },
                        label = { Text("重量kg") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("备注（可选）") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
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
            }, enabled = exerciseName.isNotBlank() && selectedBodyParts.isNotEmpty()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ──────────────────────────────────────────────────────────
// 编辑训练记录弹窗（含删除）
// ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTrainingDialog(
    record: TrainingRecord,
    allExercises: List<ExerciseLibrary>,
    viewModel: TrainingViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val bodyPartOptions = listOf("胸", "背", "腿", "肩", "手臂", "核心")
    var selectedBodyParts by remember {
        mutableStateOf(record.bodyParts.split(",").map { it.trim() }.toSet())
    }
    var exerciseName by remember { mutableStateOf(record.exerciseName) }
    var showExerciseSuggestions by remember { mutableStateOf(false) }
    var sets by remember { mutableStateOf(record.sets.toString()) }
    var reps by remember { mutableStateOf(record.reps.toString()) }
    var weightKg by remember { mutableStateOf(record.weightKg.toString()) }
    var notes by remember { mutableStateOf(record.notes ?: "") }

    val exerciseSuggestions = buildList {
        addAll(allExercises.filter {
            it.name.contains(exerciseName, ignoreCase = true) ||
            selectedBodyParts.any { bp -> it.bodyPart.contains(bp, ignoreCase = true) }
        })
    }.take(8)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改记录") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("训练部位", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    bodyPartOptions.take(3).forEach { part ->
                        FilterChip(selected = part in selectedBodyParts, onClick = {
                            selectedBodyParts = if (part in selectedBodyParts) selectedBodyParts - part
                            else selectedBodyParts + part
                        }, label = { Text(part, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    bodyPartOptions.drop(3).forEach { part ->
                        FilterChip(selected = part in selectedBodyParts, onClick = {
                            selectedBodyParts = if (part in selectedBodyParts) selectedBodyParts - part
                            else selectedBodyParts + part
                        }, label = { Text(part, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box {
                    OutlinedTextField(exerciseName, { exerciseName = it; showExerciseSuggestions = it.isNotEmpty() },
                        label = { Text("动作名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    ScrollableDropdown(
                        expanded = showExerciseSuggestions && exerciseSuggestions.isNotEmpty(),
                        onDismiss = { showExerciseSuggestions = false },
                        modifier = Modifier.fillMaxWidth(),
                        items = exerciseSuggestions.map { ex ->
                            DropdownItem(
                                key = "edit_${ex.name}",
                                content = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(ex.name, fontWeight = FontWeight.Medium)
                                        if (ex.bodyPart.isNotEmpty()) Text(" · ${ex.bodyPart}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = { exerciseName = ex.name }
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(sets, { sets = it }, label = { Text("组数") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(reps, { reps = it }, label = { Text("次数") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(weightKg, { weightKg = it }, label = { Text("重量kg") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("备注（可选）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val name = exerciseName.trim()
                val parts = selectedBodyParts.toList()
                val s = sets.toIntOrNull() ?: 3
                val r = reps.toIntOrNull() ?: 10
                val w = weightKg.toDoubleOrNull() ?: 0.0
                if (name.isNotEmpty() && parts.isNotEmpty()) {
                    viewModel.updateRecord(record.id, parts, name, s, r, w, notes.ifBlank { null })
                    onSaved()
                }
            }, enabled = exerciseName.isNotBlank() && selectedBodyParts.isNotEmpty()) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    viewModel.deleteRecord(record); onSaved()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

// ──────────────────────────────────────────────────────────
// 新手引导 / 档案设置弹窗
// ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingDialog(viewModel: TrainingViewModel, onDismiss: () -> Unit) {
    var height by remember { mutableStateOf("170") }
    var weight by remember { mutableStateOf("65") }
    var goal by remember { mutableStateOf("增重增肌") }
    var experience by remember { mutableStateOf("新手") }
    var equipment by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("4") }

    val goals = listOf("增重增肌", "减脂塑形", "维持体型", "提升力量")
    val experiences = listOf("新手", "有一定基础", "中级", "高级")
    val equipOptions = listOf("哑铃", "杠铃", "固定器械", "绳索", "自重", "壶铃", "弹力带")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置训练档案") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(height, { height = it }, label = { Text("身高 (cm)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(weight, { weight = it }, label = { Text("体重 (kg)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                Text("训练目标", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    goals.forEach { g ->
                        FilterChip(selected = goal == g, onClick = { goal = g },
                            label = { Text(g, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("训练经验", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    experiences.forEach { e ->
                        FilterChip(selected = experience == e, onClick = { experience = e },
                            label = { Text(e, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("可用器材", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    equipOptions.take(4).forEach { eq ->
                        val selected = eq in equipment.split(",")
                        FilterChip(selected = selected, onClick = {
                            val list = equipment.split(",").filter { it.isNotBlank() }.toMutableList()
                            if (selected) list.remove(eq) else list.add(eq)
                            equipment = list.joinToString(",")
                        }, label = { Text(eq, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    equipOptions.drop(4).forEach { eq ->
                        val selected = eq in equipment.split(",")
                        FilterChip(selected = selected, onClick = {
                            val list = equipment.split(",").filter { it.isNotBlank() }.toMutableList()
                            if (selected) list.remove(eq) else list.add(eq)
                            equipment = list.joinToString(",")
                        }, label = { Text(eq, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(days, { days = it }, label = { Text("每周训练天数") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveUserProfile(
                    height.toIntOrNull() ?: 170,
                    weight.toDoubleOrNull() ?: 65.0,
                    goal, experience, equipment,
                    days.toIntOrNull() ?: 4
                )
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
