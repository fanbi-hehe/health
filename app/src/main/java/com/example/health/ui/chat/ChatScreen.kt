package com.example.health.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.health.data.local.entity.ChatMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success && cameraUri != null) attachedImageUri = cameraUri }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) attachedImageUri = uri }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraUri = FileProvider.getUriForFile(context,
                "${context.packageName}.fileprovider",
                File(context.cacheDir, "chat_${System.currentTimeMillis()}.jpg"))
            cameraLauncher.launch(cameraUri!!)
        } else {
            Toast.makeText(context, "需要相机权限", Toast.LENGTH_SHORT).show()
        }
    }

    fun takePhoto() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraUri = FileProvider.getUriForFile(context,
                "${context.packageName}.fileprovider",
                File(context.cacheDir, "chat_${System.currentTimeMillis()}.jpg"))
            cameraLauncher.launch(cameraUri!!)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun send() {
        val text = inputText.trim()
        if (text.isEmpty() && attachedImageUri == null) return
        if (attachedImageUri != null) {
            viewModel.sendMessageWithImage(text, attachedImageUri!!)
        } else {
            viewModel.sendMessage(text)
        }
        inputText = ""
        attachedImageUri = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💬 AI 对话") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // 消息列表（weight(1f) 自动填满剩余空间，键盘弹起时自动压缩）
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("开始对话吧！\n可以问我饮食建议、训练计划或上传食物照片",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(messages, key = { it.id }) { msg -> MessageBubble(msg) }
                if (isSending) {
                    item {
                        Row(modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text(" AI 思考中...", modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // 输入区域（图片预览 + 输入栏）—— imePadding 只作用于此，消息列表不动
            Column(
                modifier = Modifier.fillMaxWidth().imePadding()
            ) {
                attachedImageUri?.let { uri ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = uri, contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { attachedImageUri = null }) {
                            Icon(Icons.Default.Close, "移除图片", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, "相册")
                }
                IconButton(onClick = { takePhoto() }) {
                    Icon(Icons.Default.CameraAlt, "拍照")
                }
                OutlinedTextField(
                    value = inputText, onValueChange = { inputText = it },
                    placeholder = { Text("输入消息...") },
                    modifier = Modifier.weight(1f), maxLines = 4
                )
                IconButton(onClick = { send() }, enabled = !isSending) {
                    Icon(Icons.AutoMirrored.Filled.Send, "发送",
                        tint = if (isSending) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary)
                }
            } // close input Row
        } // close input Column (imePadding)
        } // close outer Column
    } // close Scaffold
} // close ChatScreen

// ──────────────────────────────────────────────────────────
// 消息气泡
// ──────────────────────────────────────────────────────────
@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("AI", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            message.imagePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    AsyncImage(model = file, contentDescription = null,
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            if (message.content.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(text = message.content, modifier = Modifier.padding(12.dp),
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(timeStr, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp))
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Text("我", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
