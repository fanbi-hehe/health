package com.example.health.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.health.data.preference.AppPreferences

/**
 * 设置页 —— 模型服务配置、目标设定、食物库、数据管理、通知。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val textModel by viewModel.textModel.collectAsState()
    val visionModel by viewModel.visionModel.collectAsState()
    val targetWeight by viewModel.targetWeightKg.collectAsState()
    val targetCalories by viewModel.targetDailyCalories.collectAsState()
    val coachEnabled by viewModel.coachNotificationEnabled.collectAsState()
    val coachHour by viewModel.coachReminderHour.collectAsState()
    val coachMinute by viewModel.coachReminderMinute.collectAsState()

    // ── 对话框状态 ──
    var showApiUrlDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showTextModelDialog by remember { mutableStateOf(false) }
    var showVisionModelDialog by remember { mutableStateOf(false) }
    var showTargetWeightDialog by remember { mutableStateOf(false) }
    var showTargetCaloriesDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
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
            // ── 模型服务配置 ──
            SettingsGroup(title = "模型服务配置") {
                SettingsRow(
                    label = "API Base URL",
                    hint = apiBaseUrl,
                    onClick = { showApiUrlDialog = true }
                )
                SettingsRow(
                    label = "API Key",
                    hint = if (apiKey.isNotEmpty()) "●●●●●●●● (已设置)" else "点击配置",
                    onClick = { showApiKeyDialog = true }
                )
                SettingsRow(
                    label = "文本模型",
                    hint = textModel,
                    onClick = { showTextModelDialog = true }
                )
                SettingsRow(
                    label = "视觉模型",
                    hint = visionModel,
                    onClick = { showVisionModelDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 目标设定 ──
            SettingsGroup(title = "目标设定") {
                SettingsRow(
                    label = "目标体重",
                    hint = "${targetWeight} kg",
                    onClick = { showTargetWeightDialog = true }
                )
                SettingsRow(
                    label = "每日目标热量",
                    hint = "$targetCalories kcal",
                    onClick = { showTargetCaloriesDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 自定义食物库 ──
            SettingsGroup(title = "自定义食物库") {
                SettingsRow(label = "食物管理", hint = "添加/编辑/删除")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 数据管理 ──
            SettingsGroup(title = "数据管理") {
                SettingsRow(label = "导出所有数据（JSON）", hint = "备份")
                SettingsRow(label = "导入数据（JSON）", hint = "从备份恢复")
                SettingsRow(label = "清理旧照片", hint = "释放存储空间")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 通知与个性化 ──
            SettingsGroup(title = "通知与个性化") {
                SettingsSwitchRow(
                    label = "暴躁教练提醒",
                    checked = coachEnabled,
                    onToggle = { viewModel.setCoachNotificationEnabled(it) }
                )
                SettingsRow(
                    label = "提醒时间",
                    hint = "${coachHour}:${coachMinute.toString().padStart(2, '0')}",
                    onClick = { showReminderTimeDialog = true }
                )
                SettingsRow(label = "暴躁语录管理", hint = "查看/添加/删除")
            }
        }
    }

    // ── 编辑对话框 ──
    if (showApiUrlDialog) {
        EditTextDialog(
            title = "API Base URL",
            initialValue = apiBaseUrl,
            placeholder = AppPreferences.DEFAULT_API_BASE_URL,
            onDismiss = { showApiUrlDialog = false },
            onConfirm = {
                viewModel.setApiBaseUrl(it)
                showApiUrlDialog = false
            }
        )
    }

    if (showApiKeyDialog) {
        EditTextDialog(
            title = "API Key",
            initialValue = apiKey,
            placeholder = "输入 API Key",
            isPassword = true,
            onDismiss = { showApiKeyDialog = false },
            onConfirm = {
                viewModel.setApiKey(it)
                showApiKeyDialog = false
            }
        )
    }

    if (showTextModelDialog) {
        EditTextDialog(
            title = "文本模型",
            initialValue = textModel,
            placeholder = AppPreferences.DEFAULT_TEXT_MODEL,
            onDismiss = { showTextModelDialog = false },
            onConfirm = {
                viewModel.setTextModel(it)
                showTextModelDialog = false
            }
        )
    }

    if (showVisionModelDialog) {
        EditTextDialog(
            title = "视觉模型",
            initialValue = visionModel,
            placeholder = AppPreferences.DEFAULT_VISION_MODEL,
            onDismiss = { showVisionModelDialog = false },
            onConfirm = {
                viewModel.setVisionModel(it)
                showVisionModelDialog = false
            }
        )
    }

    if (showTargetWeightDialog) {
        EditNumberDialog(
            title = "目标体重 (kg)",
            initialValue = targetWeight.toString(),
            onDismiss = { showTargetWeightDialog = false },
            onConfirm = {
                it.toDoubleOrNull()?.let { w -> viewModel.setTargetWeightKg(w) }
                showTargetWeightDialog = false
            }
        )
    }

    if (showTargetCaloriesDialog) {
        EditNumberDialog(
            title = "每日目标热量 (kcal)",
            initialValue = targetCalories.toString(),
            onDismiss = { showTargetCaloriesDialog = false },
            onConfirm = {
                it.toIntOrNull()?.let { c -> viewModel.setTargetDailyCalories(c) }
                showTargetCaloriesDialog = false
            }
        )
    }

    if (showReminderTimeDialog) {
        TimePickerDialog(
            initialHour = coachHour,
            initialMinute = coachMinute,
            onDismiss = { showReminderTimeDialog = false },
            onConfirm = { h, m ->
                viewModel.setCoachReminderTime(h, m)
                showReminderTimeDialog = false
            }
        )
    }
}

// ──────────────────────────────────────────────────────────
// 可复用组件
// ──────────────────────────────────────────────────────────

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    hint: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onToggle)
    }
    HorizontalDivider()
}

// ──────────────────────────────────────────────────────────
// 编辑弹窗
// ──────────────────────────────────────────────────────────

@Composable
private fun EditTextDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    isPassword: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                visualTransformation = if (isPassword && !showPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                trailingIcon = if (isPassword) {
                    {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "隐藏" else "显示")
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditNumberDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var hourStr by remember { mutableStateOf(initialHour.toString()) }
    var minuteStr by remember { mutableStateOf(initialMinute.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置提醒时间") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = hourStr,
                    onValueChange = { hourStr = it },
                    label = { Text("时") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = " : ",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                OutlinedTextField(
                    value = minuteStr,
                    onValueChange = { minuteStr = it },
                    label = { Text("分") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hourStr.toIntOrNull() ?: initialHour
                val m = minuteStr.toIntOrNull() ?: initialMinute
                onConfirm(h.coerceIn(0, 23), m.coerceIn(0, 59))
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
