package com.example.health.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ChatMessage
import com.example.health.data.repository.AiRepository
import com.example.health.util.ImageCompressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).chatMessageDao()
    private val aiRepo = AiRepository(application)

    // ── 所有聊天消息 ──
    val messages: StateFlow<List<ChatMessage>> = dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 发送状态 ──
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // ── 错误 ──
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }

    /**
     * 发送文本消息（不含图片）。
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                // 1. 保存用户消息
                val userMsg = ChatMessage(
                    role = "user", content = text.trim(),
                    imagePath = null, timestamp = System.currentTimeMillis()
                )
                dao.insert(userMsg)

                // 2. 获取最近 10 条历史
                val history = dao.getRecentMessages(10).reversed()

                // 3. 调用 AI
                val result = aiRepo.chatCompletion(text.trim(), null, history)
                result.fold(
                    onSuccess = { reply ->
                        dao.insert(ChatMessage(role = "assistant", content = reply,
                            timestamp = System.currentTimeMillis()))
                    },
                    onFailure = { e ->
                        _error.value = e.message ?: "对话失败"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "未知错误"
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * 发送带图片的消息（拍照或相册选择）。
     */
    fun sendMessageWithImage(text: String, imageUri: Uri) {
        if (text.isBlank() && imageUri == Uri.EMPTY) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                val context = getApplication<Application>()
                // 1. 压缩图片
                val compressed = ImageCompressor.compress(context, imageUri)
                val imagePath = compressed.absolutePath

                // 2. 保存用户消息（含图片路径）
                val userMsg = ChatMessage(
                    role = "user",
                    content = text.trim().ifBlank { "[图片]" },
                    imagePath = imagePath,
                    timestamp = System.currentTimeMillis()
                )
                dao.insert(userMsg)

                // 3. 获取最近 10 条历史
                val history = dao.getRecentMessages(10).reversed()

                // 4. 调用 AI（带图片，使用文本模型配置；如果模型支持视觉，会识别图片内容）
                val result = aiRepo.chatCompletion(text.trim(), compressed, history)
                result.fold(
                    onSuccess = { reply ->
                        dao.insert(ChatMessage(role = "assistant", content = reply,
                            timestamp = System.currentTimeMillis()))
                    },
                    onFailure = { e ->
                        _error.value = e.message ?: "对话失败"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "未知错误"
            } finally {
                _isSending.value = false
            }
        }
    }
}
