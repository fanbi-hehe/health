package com.example.health.data.repository

import android.content.Context
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ChatMessage
import com.example.health.data.preference.AppPreferences
import com.example.health.data.remote.api.ApiService
import com.example.health.data.remote.dto.ChatRequest
import com.example.health.data.remote.dto.ChatResponse
import com.example.health.data.remote.dto.ContentPart
import com.example.health.data.remote.dto.FunctionCall
import com.example.health.data.remote.dto.FoodRecognitionResult
import com.example.health.data.remote.dto.ImageUrl
import com.example.health.data.remote.dto.Message
import com.example.health.data.remote.dto.RecognizedFood
import com.example.health.data.remote.dto.Tool
import com.example.health.data.remote.dto.ToolCall
import com.example.health.domain.action.ToolExecutor
import com.example.health.domain.action.TextToolCallParser
import com.example.health.domain.action.TavilySearch
import com.example.health.domain.plan.TrainingPlanGenerator
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import com.example.health.util.ImageCompressor
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class AiRepository(private val context: Context) {

    private val prefs = AppPreferences(context)
    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val api = retrofit.create(ApiService::class.java)

    // ────────── 视觉：食物识别 ──────────

    suspend fun recognizeFood(imageFile: File): Result<FoodRecognitionResult> {
        return try {
            val model = prefs.visionModel.first()
            val apiKey = prefs.visionApiKey.first()
            val baseUrl = prefs.visionApiBaseUrl.first()

            val base64 = ImageCompressor.fileToBase64(imageFile)
            val dataUrl = "data:image/jpeg;base64,$base64"

            val request = ChatRequest(
                model = model,
                messages = listOf(Message(role = "user", content = listOf(
                    ContentPart(type = "text", text =
                        // 与 v0.1 完全一致的识别 Prompt：只识别名称/重量/热量
                        "识别图中食物。估算每种食物的重量(g)和热量(kcal)。" +
                        "只返回纯JSON，不要任何解释。" +
                        "格式：{\"foods\":[{\"name\":\"食物名\",\"weight_g\":数值,\"calories_kcal\":数值}],\"total_calories\":数值}"
                    ),
                    ContentPart(type = "image_url", imageUrl = ImageUrl(url = dataUrl))
                )))
            )

            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val response = api.chatCompletion(url, mapOf("Authorization" to "Bearer $apiKey"), request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                Result.success(parseFoodJson(content))
            } else {
                Result.failure(Exception("API 请求失败: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ────────── 宏量营养素估算（识别完成后用语言模型补充） ──────────

    /**
     * 识别出食物列表后，调用文本模型估算每项食物的蛋白质/碳水/脂肪。
     * 任何失败都返回原列表（宏量为 0），不影响主流程。
     */
    suspend fun estimateMacros(foods: List<RecognizedFood>): List<RecognizedFood> {
        if (foods.isEmpty()) return foods
        return try {
            val model = prefs.textModel.first()
            val apiKey = prefs.textApiKey.first()
            val baseUrl = prefs.textApiBaseUrl.first()

            val foodLines = foods.joinToString("\n") {
                "- ${it.name} ${it.weightG}g 约 ${it.caloriesKcal}kcal"
            }
            val prompt = "以下是用户一餐中识别出的食物列表：\n$foodLines\n" +
                "请估算每项食物的蛋白质、碳水化合物、脂肪含量（克，按列表中的重量估算）。" +
                "只返回纯JSON数组，不要任何解释：" +
                "[{\"name\":\"食物名\",\"protein_g\":数值,\"carbs_g\":数值,\"fat_g\":数值}]" +
                "数量与顺序必须与列表一致，无法判断的项目填 0。"

            val request = ChatRequest(
                model = model,
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(ContentPart(type = "text", text = prompt))
                    )
                ),
                maxTokens = 1024
            )
            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val response = api.chatCompletion(
                url, mapOf("Authorization" to "Bearer $apiKey"), request
            )
            if (!response.isSuccessful) return foods
            val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
            mergeMacros(foods, content)
        } catch (_: Exception) {
            foods
        }
    }

    /** 解析语言模型返回的宏量数组，按顺序合并回食物列表。 */
    private fun mergeMacros(
        foods: List<RecognizedFood>,
        rawJson: String
    ): List<RecognizedFood> {
        return try {
            val cleaned = rawJson
                .replace("```json", "").replace("```", "").trim()
            val start = cleaned.indexOf('[')
            val end = cleaned.lastIndexOf(']')
            val jsonStr = if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
            val type = object : TypeToken<List<MacroEstimate>>() {}.type
            val estimates = gson.fromJson<List<MacroEstimate>>(jsonStr, type) ?: emptyList()

            foods.mapIndexed { index, food ->
                val est = estimates.getOrNull(index)
                if (est != null && est.name.isNotBlank()) {
                    food.copy(
                        proteinG = est.proteinG,
                        carbsG = est.carbsG,
                        fatG = est.fatG
                    )
                } else {
                    food
                }
            }
        } catch (_: Exception) {
            foods
        }
    }

    /** 语言模型宏量估算结果（JSON 数组元素）。 */
    private data class MacroEstimate(
        val name: String = "",
        @SerializedName("protein_g") val proteinG: Int = 0,
        @SerializedName("carbs_g") val carbsG: Int = 0,
        @SerializedName("fat_g") val fatG: Int = 0
    )

    // ────────── 文本/对话：AI 聊天 ──────────

    /**
     * @param userText     用户输入文本
     * @param imageFile    可选图片（如果用户上传了照片）
     * @param history      最近聊天记录（滑动窗口）
     * @param systemPrompt 自定义系统提示（当为空时使用默认提示）
     */
    suspend fun chatCompletion(
        userText: String,
        imageFile: File?,
        history: List<ChatMessage>,
        maxTokens: Int = 1024,
        systemPrompt: String = ""
    ): Result<String> {
        return try {
            val model = prefs.textModel.first()
            val apiKey = prefs.textApiKey.first()
            val baseUrl = prefs.textApiBaseUrl.first()

            // 构建消息列表
            val messages = buildChatMessages(userText, imageFile, history, systemPrompt)

            val request = ChatRequest(model = model, messages = messages, maxTokens = maxTokens)

            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val response = api.chatCompletion(url, mapOf("Authorization" to "Bearer $apiKey"), request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                Result.success(TextToolCallParser.stripToolCalls(content.trim()))
            } else {
                Result.failure(Exception("API 请求失败: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ────────── 带工具调用的对话（function calling） ──────────

    data class AiChatResult(
        val content: String,
        val toolFeedback: String? = null,
        val toolsSucceeded: Boolean = true
    )

    /**
     * 带工具调用的聊天：
     * 1. 请求时携带工具定义；
     * 2. 模型返回 tool_calls 则在本地白名单校验后执行；
     * 3. 把执行结果作为 tool 消息回传，再取最终回复。
     * 模型不支持 tools 时自动降级为普通对话。
     */
    suspend fun chatCompletionWithTools(
        userText: String,
        imageFile: File?,
        history: List<ChatMessage>,
        systemPrompt: String = "",
        tools: List<Tool> = emptyList()
    ): Result<AiChatResult> {
        return try {
            val model = prefs.textModel.first()
            val apiKey = prefs.textApiKey.first()
            val baseUrl = prefs.textApiBaseUrl.first()
            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val headers = mapOf("Authorization" to "Bearer $apiKey")

            val messages = buildChatMessages(userText, imageFile, history, systemPrompt)

            // 第一次请求：带工具
            var response = api.chatCompletion(
                url, headers,
                ChatRequest(model = model, messages = messages, maxTokens = 1024, tools = tools)
            )
            if (!response.isSuccessful) {
                // 模型可能不支持 tools：不带工具重试（普通对话）
                response = api.chatCompletion(url, headers, ChatRequest(model = model, messages = messages))
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("API 请求失败: ${response.code()} ${response.message()}")
                    )
                }
                return Result.success(
                    AiChatResult(
                        content = TextToolCallParser.stripToolCalls(extractContent(response.body())),
                        toolsSucceeded = false
                    )
                )
            }

            val firstBody = response.body()
            val toolCalls = firstBody?.choices?.firstOrNull()?.message?.toolCalls
            val firstContent = extractContent(firstBody)

            // ── 标准 JSON tool_calls ──
            if (!toolCalls.isNullOrEmpty()) {
                // 执行工具调用（白名单校验 + 参数校验）
                val executor = buildToolExecutor()
                val feedbacks = mutableListOf<String>()
                val seen = mutableSetOf<String>()
                messages.add(
                    Message(
                        role = "assistant",
                        content = listOf(
                            ContentPart(
                                type = "text",
                                text = firstBody?.choices?.firstOrNull()?.message?.content ?: ""
                            )
                        ),
                        toolCalls = toolCalls
                    )
                )
                toolCalls.forEach { call ->
                    val key = "${call.function?.name}|${call.function?.arguments}"
                    val feedback = if (seen.add(key)) {
                        executor.execute(
                            call.function?.name ?: "",
                            call.function?.arguments ?: ""
                        )
                    } else {
                        "该操作已在本轮执行过，跳过重复。"
                    }
                    feedbacks.add(feedback)
                    messages.add(
                        Message(
                            role = "tool",
                            content = listOf(ContentPart(type = "text", text = feedback)),
                            toolCallId = call.id
                        )
                    )
                }

                // 第二次请求：拿最终回复
                val finalResponse = api.chatCompletion(
                    url, headers, ChatRequest(model = model, messages = messages)
                )
                if (!finalResponse.isSuccessful) {
                    return Result.failure(
                        Exception("API 请求失败: ${finalResponse.code()} ${finalResponse.message()}")
                    )
                }
                return Result.success(
                    AiChatResult(
                        content = TextToolCallParser.stripToolCalls(extractContent(finalResponse.body())),
                        toolFeedback = feedbacks.firstOrNull(),
                        toolsSucceeded = true
                    )
                )
            }

            // ── 文本工具调用（DSML/XML 风格，模型把调用写进了回复内容） ──
            val textCalls = TextToolCallParser.extractToolCalls(firstContent)
            if (textCalls.isNotEmpty()) {
                val executor = buildToolExecutor()
                // 转换为标准 tool_calls（生成 id），让模型通过 assistant+tool 消息链看到执行结果
                val convertedCalls = textCalls.mapIndexed { i, call ->
                    ToolCall(
                        id = "call_${System.currentTimeMillis()}_$i",
                        type = "function",
                        function = FunctionCall(
                            name = call.name,
                            arguments = gson.toJson(call.arguments)
                        )
                    )
                }
                messages.add(
                    Message(
                        role = "assistant",
                        content = listOf(
                            ContentPart(
                                type = "text",
                                text = TextToolCallParser.stripToolCalls(firstContent)
                            )
                        ),
                        toolCalls = convertedCalls
                    )
                )
                // 执行工具并追加 role="tool" 结果消息（标准反馈链 + 响应级幂等）
                val seen = mutableSetOf<String>()
                val feedbacks = mutableListOf<String>()
                convertedCalls.forEach { call ->
                    val key = "${call.function?.name}|${call.function?.arguments}"
                    val feedback = if (seen.add(key)) {
                        executor.execute(
                            call.function?.name ?: "",
                            call.function?.arguments ?: ""
                        )
                    } else {
                        "该操作已在本轮执行过，跳过重复。"
                    }
                    feedbacks.add(feedback)
                    messages.add(
                        Message(
                            role = "tool",
                            content = listOf(ContentPart(type = "text", text = feedback)),
                            toolCallId = call.id
                        )
                    )
                }

                // 二次请求：携带完整消息链（模型明确这些工具已执行及结果）
                val finalResponse = api.chatCompletion(
                    url, headers, ChatRequest(model = model, messages = messages)
                )
                if (!finalResponse.isSuccessful) {
                    return Result.failure(
                        Exception("API 请求失败: ${finalResponse.code()} ${finalResponse.message()}")
                    )
                }
                return Result.success(
                    AiChatResult(
                        content = TextToolCallParser.stripToolCalls(extractContent(finalResponse.body())),
                        toolFeedback = feedbacks.firstOrNull(),
                        toolsSucceeded = true
                    )
                )
            }

            // ── 普通回复：剥离可能的残留标签后返回 ──
            Result.success(
                AiChatResult(content = TextToolCallParser.stripToolCalls(firstContent))
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 构建 OpenAI 兼容消息列表（system + 历史 + 当前消息）。 */
    private suspend fun buildChatMessages(
        userText: String,
        imageFile: File?,
        history: List<ChatMessage>,
        systemPrompt: String
    ): MutableList<Message> {
        val messages = mutableListOf<Message>()

        // System prompt（优先使用传入的自定义提示）
        val promptText = systemPrompt.ifBlank {
            "你是一位专业的私人健康与体能管家（CSCS认证级别）。" +
                "根据用户提供的数据和问题，给出个性化、具体、可行的建议。" +
                "回答简洁有力，控制在200字以内。"
        }
        messages.add(
            Message(
                role = "system",
                content = listOf(ContentPart(type = "text", text = promptText))
            )
        )

        // 历史消息（滑动窗口）
        history.forEach { msg ->
            val role = if (msg.role == "assistant") "assistant" else "user"
            val parts = mutableListOf<ContentPart>()
            if (msg.content.isNotBlank()) {
                parts.add(ContentPart(type = "text", text = msg.content))
            }
            if (parts.isNotEmpty()) {
                messages.add(Message(role = role, content = parts))
            }
        }

        // 当前用户消息
        val userParts = mutableListOf<ContentPart>()
        userParts.add(ContentPart(type = "text", text = userText))
        if (imageFile != null) {
            val base64 = ImageCompressor.fileToBase64(imageFile)
            userParts.add(
                ContentPart(
                    type = "image_url",
                    imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64")
                )
            )
        }
        messages.add(Message(role = "user", content = userParts))
        return messages
    }

    private fun extractContent(body: ChatResponse?): String =
        body?.choices?.firstOrNull()?.message?.content?.trim() ?: ""

    /** 统一的工具执行器：训练计划生成 + Tavily 联网搜索。 */
    private fun buildToolExecutor(): ToolExecutor = ToolExecutor(
        AppDatabase.getInstance(context),
        planGenerator = { custom ->
            TrainingPlanGenerator(context).generate(custom)
                .getOrElse { e -> "计划生成失败：${e.message ?: "未知错误"}" }
        },
        searchExecutor = { query -> TavilySearch(context).search(query) }
    )

    private fun parseFoodJson(raw: String): FoodRecognitionResult {
        return try {
            val cleaned = raw.replace("```json", "").replace("```", "").trim()
            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            val json = if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
            gson.fromJson(json, FoodRecognitionResult::class.java)
        } catch (_: Exception) { FoodRecognitionResult() }
    }
}
