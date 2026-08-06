package com.example.health.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToFoodLibrary: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    // ── 视觉模型 ──
    val visionUrl by viewModel.visionApiBaseUrl.collectAsState()
    val visionKey by viewModel.visionApiKey.collectAsState()
    val visionModel by viewModel.visionModel.collectAsState()
    // ── 文本模型 ──
    val textUrl by viewModel.textApiBaseUrl.collectAsState()
    val textKey by viewModel.textApiKey.collectAsState()
    val textModel by viewModel.textModel.collectAsState()
    // ── 目标 ──
    val targetWeight by viewModel.targetWeightKg.collectAsState()
    val targetCalories by viewModel.targetDailyCalories.collectAsState()
    // ── 通知 ──
    val coachEnabled by viewModel.coachNotificationEnabled.collectAsState()
    val coachHour by viewModel.coachReminderHour.collectAsState()
    val coachMinute by viewModel.coachReminderMinute.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()

    // ── 导入弹窗 ──
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showQuotesDialog by remember { mutableStateOf(false) }

    // ── 对话框状态 ──
    var showVisionUrl by remember { mutableStateOf(false) }
    var showVisionKey by remember { mutableStateOf(false) }
    var showVisionModel by remember { mutableStateOf(false) }
    var showTextUrl by remember { mutableStateOf(false) }
    var showTextKey by remember { mutableStateOf(false) }
    var showTextModel by remember { mutableStateOf(false) }
    var showTargetWeight by remember { mutableStateOf(false) }
    var showTargetCalories by remember { mutableStateOf(false) }
    var showReminderTime by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            // ── 视觉模型配置 ──
            SettingsGroup("视觉模型（食物识别）") {
                SettingsRow("API Base URL", visionUrl) { showVisionUrl = true }
                SettingsRow("API Key", if (visionKey.isNotEmpty()) "●●●● (已设置)" else "点击配置") { showVisionKey = true }
                SettingsRow("模型名称", visionModel) { showVisionModel = true }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 文本模型配置 ──
            SettingsGroup("文本模型（AI 对话）") {
                SettingsRow("API Base URL", textUrl) { showTextUrl = true }
                SettingsRow("API Key", if (textKey.isNotEmpty()) "●●●● (已设置)" else "点击配置") { showTextKey = true }
                SettingsRow("模型名称", textModel) { showTextModel = true }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 目标设定 ──
            SettingsGroup("目标设定") {
                SettingsRow("目标体重", "${targetWeight} kg") { showTargetWeight = true }
                SettingsRow("每日目标热量", "$targetCalories kcal") { showTargetCalories = true }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 自定义食物库 ──
            SettingsGroup("自定义食物库") {
                SettingsRow("食物管理", "添加/编辑/删除") { onNavigateToFoodLibrary() }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 数据管理 ──
            SettingsGroup("数据管理") {
                SettingsRow("导出所有数据（JSON）", "备份") { viewModel.exportAllData() }
                SettingsRow("导入数据（JSON）", "从备份恢复") { showImportDialog = true }
                SettingsRow("清理旧照片", "释放存储空间") { viewModel.clearOldPhotos() }
            }
            backupStatus?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.contains("成功") || it.contains("已清理")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 通知 ──
            SettingsGroup("通知与个性化") {
                SettingsSwitchRow("暴躁教练提醒", coachEnabled) { viewModel.setCoachNotificationEnabled(it) }
                SettingsRow("提醒时间", "${coachHour}:${coachMinute.toString().padStart(2, '0')}") {
                    showReminderTime = true
                }
                SettingsRow("暴躁语录管理", "查看/添加/删除") { showQuotesDialog = true }
            }
        }
    }

    // ── 对话框 ──
    if (showVisionUrl) EditTextDialog("视觉 API Base URL", visionUrl, AppPreferences.DEFAULT_VISION_BASE_URL,
        onDismiss = { showVisionUrl = false }, onConfirm = { viewModel.setVisionApiBaseUrl(it); showVisionUrl = false })
    if (showVisionKey) EditTextDialog("视觉 API Key", visionKey, "输入 API Key", isPassword = true,
        onDismiss = { showVisionKey = false }, onConfirm = { viewModel.setVisionApiKey(it); showVisionKey = false })
    if (showVisionModel) EditTextDialog("视觉模型", visionModel, AppPreferences.DEFAULT_VISION_MODEL,
        onDismiss = { showVisionModel = false }, onConfirm = { viewModel.setVisionModel(it); showVisionModel = false })
    if (showTextUrl) EditTextDialog("文本 API Base URL", textUrl, AppPreferences.DEFAULT_TEXT_BASE_URL,
        onDismiss = { showTextUrl = false }, onConfirm = { viewModel.setTextApiBaseUrl(it); showTextUrl = false })
    if (showTextKey) EditTextDialog("文本 API Key", textKey, "输入 API Key", isPassword = true,
        onDismiss = { showTextKey = false }, onConfirm = { viewModel.setTextApiKey(it); showTextKey = false })
    if (showTextModel) EditTextDialog("文本模型", textModel, AppPreferences.DEFAULT_TEXT_MODEL,
        onDismiss = { showTextModel = false }, onConfirm = { viewModel.setTextModel(it); showTextModel = false })
    if (showTargetWeight) EditNumberDialog("目标体重 (kg)", targetWeight.toString(),
        onDismiss = { showTargetWeight = false },
        onConfirm = { it.toDoubleOrNull()?.let { w -> viewModel.setTargetWeightKg(w) }; showTargetWeight = false })
    if (showTargetCalories) EditNumberDialog("每日目标热量 (kcal)", targetCalories.toString(),
        onDismiss = { showTargetCalories = false },
        onConfirm = { it.toIntOrNull()?.let { c -> viewModel.setTargetDailyCalories(c) }; showTargetCalories = false })
    if (showReminderTime) TimePickerDialog(coachHour, coachMinute,
        onDismiss = { showReminderTime = false },
        onConfirm = { h, m -> viewModel.setCoachReminderTime(h, m); showReminderTime = false })
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("从备份恢复") },
            text = {
                OutlinedTextField(importText, { importText = it }, label = { Text("粘贴 JSON 数据") },
                    modifier = Modifier.fillMaxWidth().height(200.dp), maxLines = 20)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setImportJson(importText); viewModel.importData(); showImportDialog = false
                }) { Text("导入") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("取消") } }
        )
    }
    if (showQuotesDialog) QuotesManageDialog(viewModel, onDismiss = { showQuotesDialog = false })
}

@Composable
private fun QuotesManageDialog(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val quotes by viewModel.quotes.collectAsState()
    var newQuote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("暴躁语录管理") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                if (quotes.isEmpty()) {
                    Text("暂无自定义语录，使用内置默认语录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                quotes.forEachIndexed { index, quote ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(quote, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { viewModel.deleteCoachQuote(index) },
                            modifier = Modifier.size(32.dp)) {
                            Text("✕", color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(newQuote, { newQuote = it },
                        label = { Text("新语录") }, singleLine = true,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        if (newQuote.isNotBlank()) {
                            viewModel.addCoachQuote(newQuote.trim())
                            newQuote = ""
                        }
                    }) { Text("＋", style = MaterialTheme.typography.titleMedium) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        dismissButton = null
    )
}

// ──────────────────────────────────────────────────────────
// Reusable components (same as before)
// ──────────────────────────────────────────────────────────
@Composable private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) { content() }
    }
}
@Composable private fun SettingsRow(label: String, hint: String, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
}
@Composable private fun SettingsSwitchRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
    HorizontalDivider()
}
@Composable private fun EditTextDialog(title: String, initialValue: String, placeholder: String,
    isPassword: Boolean = false, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    var showPw by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        OutlinedTextField(value = value, onValueChange = { value = it }, placeholder = { Text(placeholder) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPassword && !showPw) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {{ TextButton(onClick = { showPw = !showPw }) { Text(if (showPw) "隐藏" else "显示") } }} else null)
    }, confirmButton = { TextButton(onClick = { onConfirm(value.trim()) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
@Composable private fun EditNumberDialog(title: String, initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        OutlinedTextField(value, { value = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
    }, confirmButton = { TextButton(onClick = { onConfirm(value.trim()) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
@Composable private fun TimePickerDialog(initialHour: Int, initialMinute: Int, onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit) {
    var h by remember { mutableStateOf(initialHour.toString()) }
    var m by remember { mutableStateOf(initialMinute.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("设置提醒时间") }, text = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(h, { h = it }, label = { Text("时") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            Text(" : ", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 8.dp))
            OutlinedTextField(m, { m = it }, label = { Text("分") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
    }, confirmButton = {
        TextButton(onClick = { onConfirm((h.toIntOrNull() ?: initialHour).coerceIn(0, 23),
            (m.toIntOrNull() ?: initialMinute).coerceIn(0, 59)) }) { Text("确定") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
