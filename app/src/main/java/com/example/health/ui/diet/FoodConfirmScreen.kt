package com.example.health.ui.diet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import coil.compose.rememberAsyncImagePainter
import com.example.health.data.remote.dto.RecognizedFood
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodConfirmScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    viewModel: DietViewModel = viewModel()
) {
    val photoPath by viewModel.currentPhotoPath.collectAsState()
    val allFoods by viewModel.allFoods.collectAsState()
    val lastResult by viewModel.lastRecognitionResult.collectAsState()
    val context = LocalContext.current

    // 从 ViewModel 获取的识别结果
    var editableFoods by remember { mutableStateOf<List<MutableFoodItem>>(emptyList()) }
    var mealType by remember { mutableStateOf(defaultMealType()) }
    var initialized by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }

    // 初始化：从 ViewModel 读取识别结果
    if (!initialized && lastResult != null) {
        editableFoods = lastResult!!.foods.map { food ->
            MutableFoodItem(
                name = food.name,
                weightGStr = food.weightG.toString(),
                caloriesKcalStr = food.caloriesKcal.toString()
            )
        }
        initialized = true
    }

    val mealTypes = listOf("早餐", "午餐", "晚餐", "加餐")

    val totalCalories = editableFoods.sumOf { it.caloriesKcal }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("确认食物") },
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
            // ── 图片预览 ──
            photoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(file),
                            contentDescription = "食物照片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── 食物列表 ──
            if (editableFoods.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AI 未能识别到食物",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请点击下方按钮手动添加",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                editableFoods.forEachIndexed { index, item ->
                    FoodEditCard(
                        index = index,
                        item = item,
                        onUpdate = { updated -> editableFoods = editableFoods.toMutableList().also { it[index] = updated } },
                        onDelete = { editableFoods = editableFoods.toMutableList().also { it.removeAt(index) } }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── 手动添加食物 ──
            OutlinedButton(
                onClick = {
                    editableFoods = editableFoods + MutableFoodItem(
                        name = "",
                        weightGStr = "100",
                        caloriesKcalStr = "0"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("手动添加食物", modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 总热量 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "总热量",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$totalCalories kcal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 餐别选择 ──
            Text(
                text = "选择餐别",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                mealTypes.forEach { type ->
                    FilterChip(
                        selected = mealType == type,
                        onClick = { mealType = type },
                        label = { Text(type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 保存 / 存为模板 ──
            Button(
                onClick = {
                    viewModel.saveFoodRecords(validFoods(editableFoods), mealType, photoPath)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = editableFoods.any { it.name.isNotBlank() }
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Text("保存记录", modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showSaveTemplateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = editableFoods.any { it.name.isNotBlank() }
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                Text("存为模板", modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── 存为模板弹窗 ──
    if (showSaveTemplateDialog) {
        var templateName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("存为餐食模板") },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("模板名称（如：我的早餐）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveMealTemplate(templateName, validFoods(editableFoods))
                        Toast.makeText(context, "模板已保存", Toast.LENGTH_SHORT).show()
                        showSaveTemplateDialog = false
                    },
                    enabled = templateName.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) { Text("取消") }
            }
        )
    }
}

/** 过滤空名称并转换为识别结果结构。 */
private fun validFoods(items: List<MutableFoodItem>): List<RecognizedFood> {
    return items
        .filter { it.name.isNotBlank() }
        .map {
            RecognizedFood(
                name = it.name,
                weightG = it.weightG,
                caloriesKcal = it.caloriesKcal
            )
        }
}

// ── 单个食物编辑卡片 ──
@Composable
private fun FoodEditCard(
    index: Int,
    item: MutableFoodItem,
    onUpdate: (MutableFoodItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "食物 ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.caloriesKcal} kcal",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 名称
            OutlinedTextField(
                value = item.name,
                onValueChange = { onUpdate(item.copy(name = it)) },
                label = { Text("食物名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 重量
            OutlinedTextField(
                value = item.weightGStr,
                onValueChange = { v ->
                    // 允许空字符串，只允许数字或空
                    if (v.isEmpty() || v.all { it.isDigit() }) {
                        onUpdate(item.copy(weightGStr = v))
                    }
                },
                label = { Text("重量 (g)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // 快捷调整按钮（独立一行）
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                listOf(-50, -10, +10, +50).forEach { delta ->
                    OutlinedButton(
                        onClick = {
                            val currentWeight = item.weightG  // computed from string
                            val newWeight = (currentWeight + delta).coerceAtLeast(1)
                            val density = if (currentWeight > 0) item.caloriesKcal.toDouble() / currentWeight else 0.0
                            val newCal = (newWeight * density).toInt()
                            onUpdate(item.copy(
                                weightGStr = newWeight.toString(),
                                caloriesKcalStr = newCal.toString()
                            ))
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

            Spacer(modifier = Modifier.height(8.dp))

            // 热量
            OutlinedTextField(
                value = item.caloriesKcalStr,
                onValueChange = { v ->
                    // 允许空字符串，只允许数字或空
                    if (v.isEmpty() || v.all { it.isDigit() }) {
                        onUpdate(item.copy(caloriesKcalStr = v))
                    }
                },
                label = { Text("热量 (kcal)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // 删除按钮
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── 可变食物项（编辑用，中间值存为字符串以支持清空输入框） ──
data class MutableFoodItem(
    val name: String,
    val weightGStr: String,
    val caloriesKcalStr: String
) {
    val weightG: Int get() = weightGStr.toIntOrNull() ?: 0
    val caloriesKcal: Int get() = caloriesKcalStr.toIntOrNull() ?: 0
}

fun defaultMealType(): String = when (java.time.LocalTime.now().hour) {
    in 5..10 -> "早餐"
    in 10..14 -> "午餐"
    in 14..20 -> "晚餐"
    else -> "加餐"
}
