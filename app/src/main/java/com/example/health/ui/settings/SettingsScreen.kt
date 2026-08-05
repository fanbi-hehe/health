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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 设置页 —— 框架占位，各功能入口后续接入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
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
            SettingsGroup(title = "模型服务配置") {
                SettingsRow(label = "API Base URL", hint = "点击配置")
                SettingsRow(label = "API Key", hint = "点击配置")
                SettingsRow(label = "文本模型", hint = "glm-4-flash")
                SettingsRow(label = "视觉模型", hint = "glm-4v-flash")
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup(title = "目标设定") {
                SettingsRow(label = "目标体重", hint = "点击设置")
                SettingsRow(label = "每日目标热量", hint = "点击设置")
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup(title = "自定义食物库") {
                SettingsRow(label = "食物管理", hint = "添加/编辑/删除")
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup(title = "数据管理") {
                SettingsRow(label = "导出所有数据（JSON）", hint = "备份")
                SettingsRow(label = "导入数据（JSON）", hint = "从备份恢复")
                SettingsRow(label = "清理旧照片", hint = "释放存储空间")
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup(title = "通知与个性化") {
                SettingsRow(label = "暴躁教练提醒", hint = "已启用")
                SettingsRow(label = "提醒时间", hint = "21:00")
                SettingsRow(label = "暴躁语录管理", hint = "查看/添加/删除")
            }
        }
    }
}

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
    hint: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: 各设置子页面入口 */ }
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
