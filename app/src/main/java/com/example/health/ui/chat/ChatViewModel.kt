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
import com.example.health.domain.action.ToolDefinitions
import com.example.health.domain.context.UserContextBuilder
import com.example.health.domain.router.IntentQuery
import com.example.health.domain.router.IntentRouter
import com.example.health.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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

                // 3. 每日重置检测 + 历史压缩 + 构建全量上下文（IO 线程）
                val systemPrompt = withContext(Dispatchers.IO) {
                    val todayStr = LocalDate.now().toString()
                    if (prefs.lastChatDate.first() != todayStr) {
                        prefs.setLastChatDate(todayStr)
                    }
                    maybeCompactHistory()
                    buildSystemPromptWithContext()
                }

                // 4. 调用 AI（function calling：模型识别动作 → 本地白名单执行）
                val result = aiRepo.chatCompletionWithTools(
                    userText = text.trim(),
                    imageFile = null,
                    history = history,
                    systemPrompt = systemPrompt,
                    tools = ToolDefinitions.coachTools
                )
                result.fold(
                    onSuccess = { r ->
                        // 工具执行反馈 → Toast（写没写一眼可见）
                        r.toolFeedback?.let { _actionFeedback.value = it }
                        if (!r.toolsSucceeded) {
                            // 模型不支持 tools 时的降级：本地正则兜底
                            val fallbackFeedback = withContext(Dispatchers.IO) {
                                val knownExercises = contextBuilder.getKnownExerciseNames()
                                val action = UserActionParser.parse(text.trim(), knownExercises)
                                if (action != UserAction.None) UserActionExecutor(db).execute(action)
                                else null
                            }
                            fallbackFeedback?.let { _actionFeedback.value = it }
                        }
                        dao.insert(ChatMessage(role = "assistant", content = r.content,
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
                    val todayStr = LocalDate.now().toString()
                    if (prefs.lastChatDate.first() != todayStr) {
                        prefs.setLastChatDate(todayStr)
                    }
                    maybeCompactHistory()
                    buildSystemPromptWithContext()
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
    private suspend fun buildSystemPromptWithContext(actionFeedback: String = ""): String {
        // 1. 用户档案
        val profileText = contextBuilder.buildProfileText()

        // 2. 全量数据注入（今日/本周/本月，带日期时间）
        val fullContext = contextBuilder.buildFullContext()

        // 3. 历史滚动摘要 + 每日重置提示
        val summary = prefs.chatSummary.first()
        val newDay = isNewDay(prefs.lastChatDate.first())

        // 4. 组装系统提示
        return buildString {
            appendLine("你是一位专业的私人健康与体能管家（CSCS认证级别）。根据用户提供的数据和问题，给出个性化、具体、可行的建议。回答简洁有力，控制在200字以内。")
            appendLine()
            appendLine("## 用户档案")
            appendLine(profileText)

            if (summary.isNotBlank()) {
                appendLine()
                appendLine("## 历史摘要")
                appendLine(summary)
            }

            if (newDay) {
                appendLine()
                appendLine("注意：今天是 ${LocalDate.now()}（${weekLabelNow()}），这是新的一天。请以今天的数据为准，不要使用旧日期数据回答今天的问题。")
            }

            if (fullContext.isNotBlank()) {
                appendLine()
                appendLine("## 用户数据")
                appendLine(fullContext)
            }

            // 已执行的本地操作结果（让 AI 基于事实回复）
            if (actionFeedback.isNotBlank()) {
                appendLine()
                appendLine("## 系统操作结果")
                appendLine(actionFeedback)
            }

        }.trim()
    }

    // ── 滚动摘要（auto-compact） ──

    /** 历史 token 达到阈值时：先让 AI 压缩为摘要 → 清空历史，之后只带摘要 + 最近消息。 */
    private suspend fun maybeCompactHistory() {
        val summary = prefs.chatSummary.first()
        val history = dao.getAllMessagesOnce()
        if (shouldCompact(summary, history)) {
            compactHistory(summary, history)
        }
    }

    private suspend fun compactHistory(oldSummary: String, history: List<ChatMessage>) {
        if (history.isEmpty()) return
        val historyText = history.joinToString("\n") { "${it.role}：${it.content}" }
        val prompt = buildString {
            append("请把以下对话压缩成一段不超过500字的中文摘要，保留：用户目标、关键数据结论、最近状态、未完成事项。")
            append("\n\n")
            if (oldSummary.isNotBlank()) {
                append("旧摘要：\n$oldSummary\n\n")
            }
            append("对话记录：\n$historyText")
        }
        val result = aiRepo.chatCompletion(prompt, null, emptyList(), maxTokens = 700)
        result.fold(
            onSuccess = { summary ->
                prefs.setChatSummary(summary.trim())
                dao.deleteAll()
            },
            onFailure = { /* 压缩失败本次跳过，下次再试 */ }
        )
    }

    private fun weekLabelNow(): String =
        LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE).replace("星期", "周")

    companion object {
        /** 历史压缩阈值（token；字符按 1/4 估算，默认 8000） */
        internal const val MAX_HISTORY_TOKENS = 8000

        internal fun estimateTokens(text: String): Int = text.length / 4

        internal fun isNewDay(lastDate: String): Boolean =
            lastDate != LocalDate.now().toString()

        internal fun shouldCompact(summary: String, history: List<ChatMessage>): Boolean {
            val totalChars = summary.length + history.sumOf { it.content.length }
            return totalChars / 4 > MAX_HISTORY_TOKENS
        }
    }
}
