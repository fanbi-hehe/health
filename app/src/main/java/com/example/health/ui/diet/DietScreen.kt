package com.example.health.ui.diet

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.health.data.local.entity.DietRecord
import com.example.health.data.local.entity.FoodLibrary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(
    onNavigateToConfirm: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DietViewModel = viewModel()
) {
    val context = LocalContext.current
    val todayRecords by viewModel.todayRecords.collectAsState()
    val recognitionState by viewModel.recognitionState.collectAsState()
    val allFoods by viewModel.allFoods.collectAsState()

    // ── 相机 launcher ──
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onPhotoTaken()
        }
    }

    // ── 权限 launcher ──
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = viewModel.createTempPhotoUri()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "需要相机权限才能拍照识别食物", Toast.LENGTH_SHORT).show()
        }
    }

    fun takePhoto() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val uri = viewModel.createTempPhotoUri()
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── 手动录入弹窗 ──
    var showManualDialog by remember { mutableStateOf(false) }

    // ── 监听 AI 识别结果，导航到确认页 ──
    LaunchedEffect(Unit) {
        viewModel.navigateToConfirm.collect { _ ->
            onNavigateToConfirm()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍽️ 饮食记录") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (recognitionState is RecognitionState.Recognizing) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text(" AI 识别中...", modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (recognitionState is RecognitionState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = (recognitionState as RecognitionState.Error).message,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = { takePhoto() },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "拍照") },
                    text = { Text("拍照识别") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TodayCalorieSummary(
                records = todayRecords,
                modifier = Modifier.padding(16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showManualDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("手动录入", modifier = Modifier.padding(start = 4.dp))
                }
            }

            if (todayRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "今天还没有饮食记录\n点击右下角拍照开始记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(todayRecords, key = { it.id }) { record ->
                        DietRecordCard(record)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // ── 手动录入弹窗 ──
    if (showManualDialog) {
        ManualInputDialog(
            allFoods = allFoods,
            onDismiss = { showManualDialog = false },
            onSave = { name, weight, calories, mealType ->
                viewModel.saveManualRecord(name, weight, calories, mealType)
                showManualDialog = false
            }
        )
    }
}

// ──────────────────────────────────────────────────────────
// 今日热量汇总
// ──────────────────────────────────────────────────────────
@Composable
private fun TodayCalorieSummary(records: List<DietRecord>, modifier: Modifier = Modifier) {
    val totalCal = records.sumOf { it.caloriesKcal }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("今日摄入", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("$totalCal", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("kcal", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        }
    }
}

// ──────────────────────────────────────────────────────────
// 单条记录卡片
// ──────────────────────────────────────────────────────────
@Composable
private fun DietRecordCard(record: DietRecord) {
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
                Text("${record.mealType} · ${record.foodName}",
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("${record.weightG}g", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${record.caloriesKcal} kcal", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ──────────────────────────────────────────────────────────
// 手动录入弹窗（含食物名称模糊搜索自动补全）
// ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualInputDialog(
    allFoods: List<FoodLibrary>,
    onDismiss: () -> Unit,
    onSave: (name: String, weightG: Int, caloriesKcal: Int, mealType: String) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var weightG by remember { mutableStateOf("100") }
    var caloriesKcal by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf("午餐") }
    var showSuggestions by remember { mutableStateOf(false) }
    val mealTypes = listOf("早餐", "午餐", "晚餐", "加餐")

    // 模糊匹配：按输入内容过滤食物库
    val suggestions = if (foodName.length >= 1) {
        allFoods.filter { it.name.contains(foodName, ignoreCase = true) }
            .take(6)
    } else emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动录入食物") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // ── 食物名称 + 自动补全下拉 ──
                Box {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = {
                            foodName = it
                            showSuggestions = it.isNotEmpty()
                        },
                        label = { Text("食物名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ScrollableDropdown(
                        expanded = showSuggestions && suggestions.isNotEmpty(),
                        onDismiss = { showSuggestions = false },
                        modifier = Modifier.fillMaxWidth(),
                        items = suggestions.map { food ->
                            DropdownItem(
                                key = "food_${food.id}",
                                content = {
                                    Column {
                                        Text(food.name, fontWeight = FontWeight.Medium)
                                        Text("${food.caloriesPer100g} kcal/100g",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    foodName = food.name
                                    caloriesKcal = food.caloriesPer100g.toString()
                                }
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 重量 ──
                OutlinedTextField(
                    value = weightG,
                    onValueChange = { weightG = it },
                    label = { Text("重量 (g)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 热量 ──
                OutlinedTextField(
                    value = caloriesKcal,
                    onValueChange = { caloriesKcal = it },
                    label = { Text("热量 (kcal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 餐别选择 ──
                Text("餐别", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mealTypes.forEach { type ->
                        FilterChip(
                            selected = mealType == type,
                            onClick = { mealType = type },
                            label = { Text(type) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = foodName.trim()
                    val weight = weightG.toIntOrNull() ?: 100
                    val cal = caloriesKcal.toIntOrNull() ?: 0
                    if (name.isNotEmpty() && cal > 0) {
                        onSave(name, weight, cal, mealType)
                    }
                },
                enabled = foodName.isNotBlank() && caloriesKcal.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
