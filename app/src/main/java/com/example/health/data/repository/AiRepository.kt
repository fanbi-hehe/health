package com.example.health.data.repository

import android.content.Context
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ChatMessage
import com.example.health.data.preference.AppPreferences
import com.example.health.data.remote.api.ApiService
import com.example.health.data.remote.dto.ChatRequest
import com.example.health.data.remote.dto.ChatResponse
import com.example.health.data.remote.dto.ContentPart
import com.example.health.data.remote.dto.FoodRecognitionResult
import com.example.health.data.remote.dto.ImageUrl
import com.example.health.data.remote.dto.Message
import com.example.health.data.remote.dto.RecognizedFood
import com.example.health.data.remote.dto.Tool
import com.example.health.domain.action.ToolExecutor
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

    /** 单张图片识别（兼容旧调用）。 */
    suspend fun recognizeFood(imageFile: File): Result<FoodRecognitionResult> {
        return recognizeFood(listOf(imageFile))
    }

    /**
     * 多张图片识别：
     * 1. 优先一次请求带多图（部分模型支持）；
     * 2. 模型不支持时自动降级为逐张识别并合并结果。
     */
    suspend fun recognizeFood(imageFiles: List<File>): Result<FoodRecognitionResult> {
        if (imageFiles.isEmpty()) return Result.failure(Exception("没有可识别的图片"))
        return try {
            val model = prefs.visionModel.first()
            val apiKey = prefs.visionApiKey.first()
            val baseUrl = prefs.visionApiBaseUrl.first()

            // 1. 多图一次请求
            val multiResult = requestFoodRecognition(model, apiKey, baseUrl, imageFiles)
            if (multiResult != null) {
                return Result.success(multiResult)
            }

            // 2. 降级：逐张识别并合并
            val allFoods = mutableListOf<RecognizedFood>()
            var total = 0
            var anyFailed = false
            for (file in imageFiles) {
                val single = requestFoodRecognition(model, apiKey, baseUrl, listOf(file))
                if (single == null) {
                    anyFailed = true
                    continue
                }
                allFoods += single.foods
                total += single.totalCalories
            }
            if (allFoods.isEmpty()) {
                return Result.failure(
                    Exception(if (anyFailed) "图片识别失败，请检查 API 配置后重试" else "未识别到食物")
                )
            }
            Result.success(FoodRecognitionResult(foods = allFoods, totalCalories = total))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 单次识别请求；失败返回 null（供上层降级）。 */
    private suspend fun requestFoodRecognition(
        model: String,
        apiKey: String,
        baseUrl: String,
        imageFiles: List<File>
    ): FoodRecognitionResult? {
        val parts = mutableListOf<ContentPart>()
        val isMulti = imageFiles.size > 1
        parts.add(
            ContentPart(
                type = "text",
                text =
                    "识别图片中的食物。${if (isMulti) "以下是同一餐的多张照片，请合并识别所有食物；某张没有食物可忽略。" else ""}" +
                    "估算每种食物的重量(g)和热量(kcal)。" +
                    "同时估算宏量营养素（蛋白质/碳水化合物/脂肪，单位g，按估算重量计算）。" +
                    "只返回纯JSON，不要任何解释。" +
                    "格式：{\"foods\":[{\"name\":\"食物名\",\"weight_g\":数值,\"calories_kcal\":数值," +
                    "\"protein_g\":数值,\"carbs_g\":数值,\"fat_g\":数值}],\"total_calories\":数值}"
            )
        )
        imageFiles.forEach { file ->
            val base64 = ImageCompressor.fileToBase64(file)
            parts.add(
                ContentPart(
                    type = "image_url",
                    imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64")
                )
            )
        }

        val request = ChatRequest(
            model = model,
            messages = listOf(Message(role = "user", content = parts))
        )
        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val response = api.chatCompletion(
            url, mapOf("Authorization" to "Bearer $apiKey"), request
        )
        if (!response.isSuccessful) return null
        val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
        return parseFoodJson(content)
    }

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
                Result.success(content.trim())
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
                    AiChatResult(content = extractContent(response.body()), toolsSucceeded = false)
                )
            }

            val firstBody = response.body()
            val toolCalls = firstBody?.choices?.firstOrNull()?.message?.toolCalls
            if (toolCalls.isNullOrEmpty()) {
                return Result.success(AiChatResult(content = extractContent(firstBody)))
            }

            // 执行工具调用（白名单校验 + 参数校验）
            val executor = ToolExecutor(AppDatabase.getInstance(context))
            val feedbacks = mutableListOf<String>()
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
                val feedback = executor.execute(
                    call.function?.name ?: "",
                    call.function?.arguments ?: ""
                )
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
            Result.success(
                AiChatResult(
                    content = extractContent(finalResponse.body()),
                    toolFeedback = feedbacks.firstOrNull(),
                    toolsSucceeded = true
                )
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
