package com.example.health.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.example.health.data.local.entity.FoodLibrary

/**
 * 食物库管理页：
 * - 列表展示内置 + 自定义食物（自定义优先）
 * - 自定义食物支持添加 / 编辑 / 删除，内置食物只读
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLibraryScreen(
    onBack: () -> Unit = {},
    viewModel: FoodLibraryViewModel = viewModel()
) {
    val allFoods by viewModel.allFoods.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<FoodLibrary?>(null) }
    var deletingFood by remember { mutableStateOf<FoodLibrary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食物库") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加食物") }
            )
        }
    ) { innerPadding ->
        if (allFoods.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无食物数据\n点击右下角添加自定义食物",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "共 ${allFoods.size} 种 · 自定义 ${allFoods.count { it.isCustom }} 种（内置只读）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(allFoods, key = { it.id }) { food ->
                    FoodLibraryRow(
                        food = food,
                        onClick = { if (food.isCustom) editingFood = food },
                        onDelete = { deletingFood = food }
                    )
                }
            }
        }
    }

    // ── 添加 ──
    if (showAddDialog) {
        FoodEditDialog(
            title = "添加自定义食物",
            initialName = "",
            initialCalories = "",
            initialProtein = "",
            onDismiss = { showAddDialog = false },
            onSave = { name, cal, protein ->
                viewModel.addFood(name, cal, protein)
                showAddDialog = false
            }
        )
    }

    // ── 编辑 ──
    editingFood?.let { food ->
        FoodEditDialog(
            title = "编辑食物",
            initialName = food.name,
            initialCalories = food.caloriesPer100g.toString(),
            initialProtein = if (food.proteinPer100g > 0) food.proteinPer100g.toString() else "",
            onDismiss = { editingFood = null },
            onSave = { name, cal, protein ->
                viewModel.updateFood(
                    food.copy(
                        name = name.trim(),
                        caloriesPer100g = cal,
                        proteinPer100g = protein
                    )
                )
                editingFood = null
            }
        )
    }

    // ── 删除确认 ──
    deletingFood?.let { food ->
        AlertDialog(
            onDismissRequest = { deletingFood = null },
            title = { Text("删除食物") },
            text = { Text("确定删除「${food.name}」吗？删除后手动录入将不再自动补全它。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFood(food)
                    deletingFood = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingFood = null }) { Text("取消") }
            }
        )
    }
}

// ── 单行食物条目 ──
@Composable
private fun FoodLibraryRow(
    food: FoodLibrary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = food.isCustom, onClick = onClick),
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
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${food.caloriesPer100g} kcal/100g" +
                        if (food.proteinPer100g > 0) " · 蛋白 ${"%.1f".format(food.proteinPer100g)}g" else "" +
                        if (food.isCustom) " · 自定义" else " · 内置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (food.isCustom) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── 添加/编辑对话框 ──
@Composable
private fun FoodEditDialog(
    title: String,
    initialName: String,
    initialCalories: String,
    initialProtein: String,
    onDismiss: () -> Unit,
    onSave: (name: String, caloriesPer100g: Int, proteinPer100g: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var calories by remember { mutableStateOf(initialCalories) }
    var protein by remember { mutableStateOf(initialProtein) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("食物名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = calories,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.all { it.isDigit() }) calories = v
                    },
                    label = { Text("每100g热量 (kcal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = protein,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.toDoubleOrNull() != null) protein = v
                    },
                    label = { Text("蛋白质（g/100g，可选）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cal = calories.toIntOrNull()
                    val proteinValue = protein.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    if (name.isNotBlank() && cal != null && cal > 0) {
                        onSave(name.trim(), cal, proteinValue)
                    }
                },
                enabled = name.isNotBlank() && (calories.toIntOrNull() ?: 0) > 0
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
