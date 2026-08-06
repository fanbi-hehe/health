package com.example.health.data.repository

import android.content.Context
import com.example.health.data.local.entity.ChatMessage
import com.example.health.data.preference.AppPreferences
import com.example.health.data.remote.api.ApiService
import com.example.health.data.remote.dto.ChatRequest
import com.example.health.data.remote.dto.ContentPart
import com.example.health.data.remote.dto.FoodRecognitionResult
import com.example.health.data.remote.dto.ImageUrl
import com.example.health.data.remote.dto.Message
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
                        "识别图中食物。估算每种食物的重量(g)和热量(kcal)。" +
                        "同时估算宏量营养素（蛋白质/碳水化合物/脂肪，单位g，按估算重量计算）。" +
                        "只返回纯JSON，不要任何解释。" +
                        "格式：{\"foods\":[{\"name\":\"食物名\",\"weight_g\":数值,\"calories_kcal\":数值," +
                        "\"protein_g\":数值,\"carbs_g\":数值,\"fat_g\":数值}],\"total_calories\":数值}"
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
            val messages = mutableListOf<Message>()

            // System prompt（优先使用传入的自定义提示）
            val promptText = systemPrompt.ifBlank {
                "你是一位专业的私人健康与体能管家（CSCS认证级别）。" +
                "根据用户提供的数据和问题，给出个性化、具体、可行的建议。" +
                "回答简洁有力，控制在200字以内。"
            }
            messages.add(Message(role = "system", content = listOf(
                ContentPart(type = "text", text = promptText)
            )))

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
                userParts.add(ContentPart(type = "image_url",
                    imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64")))
            }
            messages.add(Message(role = "user", content = userParts))

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
