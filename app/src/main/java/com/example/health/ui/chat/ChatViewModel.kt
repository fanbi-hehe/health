package com.example.health.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ChatMessage
import com.example.health.data.preference.AppPreferences
import com.example.health.data.repository.AiRepository
import com.example.health.domain.action.UserAction
import com.example.health.domain.action.UserActionExecutor
import com.example.health.domain.action.UserActionParser
import com.example.health.domain.context.UserContextBuilder
import com.example.health.domain.router.IntentQuery
import com.example.health.domain.router.IntentRouter
import com.example.health.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.chatMessageDao()
    private val prefs = AppPreferences(application)
    private val aiRepo = AiRepository(application)
    private val contextBuilder = UserContextBuilder(db, prefs)

    // ── 所有聊天消息 ──
    val messages: StateFlow<List<ChatMessage>> = dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 发送状态 ──
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // ── 错误 ──
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 系统操作结果（如 AI 教练写入训练/食物库），用于 UI 提示。 */
    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    fun clearError() { _error.value = null }

    fun clearActionFeedback() { _actionFeedback.value = null }

    /**
     * 发送文本消息（不含图片）。
     *
     * 流程：保存用户消息 → 意图路由 → 按需查询本地数据 → 组装系统提示
     * → 调用 AI → 保存回复。
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                // 1. 先取历史（不含当前消息，避免同一句发两次）
                val history = dao.getRecentMessages(10).reversed()

                // 2. 保存用户消息（UI 立即显示）
                val userMsg = ChatMessage(
                    role = "user", content = text.trim(),
                    imagePath = null, timestamp = System.currentTimeMillis()
                )
                dao.insert(userMsg)

                // 3. 意图路由 + 查询上下文（IO 线程）
                val systemPrompt = withContext(Dispatchers.IO) {
                    val knownExercises = contextBuilder.getKnownExerciseNames()
                    // AI 教练可执行动作：训练记录写入 / 食物库添加与修改（删除一律拦截）
                    val action = UserActionParser.parse(text.trim(), knownExercises)
                    val actionFeedback = if (action != UserAction.None) {
                        UserActionExecutor(db).execute(action)
                    } else ""
                    _actionFeedback.value = actionFeedback.takeIf { it.isNotBlank() }
                    buildSystemPromptWithContext(text.trim(), actionFeedback)
                }

                // 4. 调用 AI
                val result = aiRepo.chatCompletion(
                    userText = text.trim(),
                    imageFile = null,
                    history = history,
                    systemPrompt = systemPrompt
                )
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

                // 2. 先取历史（不含当前消息）
                val history = dao.getRecentMessages(10).reversed()

                // 3. 保存用户消息（含图片路径，UI 立即显示）
                val userMsg = ChatMessage(
                    role = "user",
                    content = text.trim().ifBlank { "[图片]" },
                    imagePath = imagePath,
                    timestamp = System.currentTimeMillis()
                )
                dao.insert(userMsg)

                // 4. 意图路由 + 查询上下文（图片消息通常与食物识别结合，也做路由）
                val systemPrompt = withContext(Dispatchers.IO) {
                    buildSystemPromptWithContext(text.trim())
                }

                // 5. 调用 AI（带图片）
                val result = aiRepo.chatCompletion(
                    userText = text.trim(),
                    imageFile = compressed,
                    history = history,
                    systemPrompt = systemPrompt
                )
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

    // ── 系统提示组装 ──

    /**
     * 根据用户输入文本，完成意图路由 → 数据查询 → 系统提示组装。
     * 在 IO 线程上运行。
     */
    private suspend fun buildSystemPromptWithContext(
        userText: String,
        actionFeedback: String = ""
    ): String {
        // 1. 获取已知动作名
        val knownExercises = contextBuilder.getKnownExerciseNames()

        // 2. 意图路由
        val intent = IntentRouter.resolve(userText, knownExercises)

        // 3. 获取用户档案（始终注入）
        val profileText = contextBuilder.buildProfileText()

        // 4. 根据意图查询上下文数据
        val contextData = contextBuilder.buildContextForIntent(intent)

        // 5. 组装系统提示
        return buildString {
            appendLine("你是一位专业的私人健康与体能管家（CSCS认证级别）。根据用户提供的数据和问题，给出个性化、具体、可行的建议。回答简洁有力，控制在200字以内。")
            appendLine()
            appendLine("## 用户档案")
            appendLine(profileText)

            if (contextData.isNotBlank()) {
                appendLine()
                appendLine("## 用户近期数据")
                appendLine(contextData)
            }

            // 已执行的本地操作结果（让 AI 基于事实回复）
            if (actionFeedback.isNotBlank()) {
                appendLine()
                appendLine("## 系统操作结果")
                appendLine(actionFeedback)
            }

            // 闲聊时不强制数据回答
            if (intent is IntentQuery.GeneralChat) {
                appendLine()
                appendLine("注意：用户当前是闲聊模式。如果问题与健康/训练无关，正常友好回答即可，不需要强行分析数据。")
            }
        }.trim()
    }
}
