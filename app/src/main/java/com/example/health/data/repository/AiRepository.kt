package com.example.health.data.repository

import android.content.Context
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

    // Retrofit 实例缓存 —— @Url 会覆盖 baseUrl，所以这里用 dummy
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val api = retrofit.create(ApiService::class.java)

    /**
     * 调用视觉模型识别食物。
     * @return 识别结果，失败返回 null
     */
    suspend fun recognizeFood(imageFile: File): Result<FoodRecognitionResult> {
        return try {
            val model = prefs.visionModel.first()
            val apiKey = prefs.apiKey.first()
            val baseUrl = prefs.apiBaseUrl.first()

            val base64 = ImageCompressor.fileToBase64(imageFile)
            val dataUrl = "data:image/jpeg;base64,$base64"

            val request = ChatRequest(
                model = model,
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            ContentPart(
                                type = "text",
                                text = "识别图中食物。估算每种食物的重量(g)和热量(kcal)。" +
                                       "只返回纯JSON，不要任何解释。" +
                                       "格式：{\"foods\":[{\"name\":\"食物名\",\"weight_g\":数值,\"calories_kcal\":数值}],\"total_calories\":数值}"
                            ),
                            ContentPart(
                                type = "image_url",
                                imageUrl = ImageUrl(url = dataUrl)
                            )
                        )
                    )
                )
            )

            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val response = api.chatCompletion(
                url = url,
                request = request,
                headers = mapOf("Authorization" to "Bearer $apiKey")
            )

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                val result = parseFoodJson(content)
                Result.success(result)
            } else {
                Result.failure(Exception("API 请求失败: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解析 AI 返回的 JSON —— 容错：提取第一个 { 到最后一个 } 之间的内容。
     */
    private fun parseFoodJson(raw: String): FoodRecognitionResult {
        return try {
            val cleaned = raw
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            val json = if (start >= 0 && end > start) {
                cleaned.substring(start, end + 1)
            } else {
                cleaned
            }

            gson.fromJson(json, FoodRecognitionResult::class.java)
        } catch (e: Exception) {
            // 解析失败，返回空结果
            FoodRecognitionResult()
        }
    }
}
