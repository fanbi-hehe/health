package com.example.health.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── 请求 ──

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val temperature: Double = 0.3
)

data class Message(
    val role: String,       // "user" | "system"
    val content: List<ContentPart>
)

data class ContentPart(
    val type: String,       // "text" | "image_url"
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    val url: String         // base64 data URL 或 HTTP URL
)

// ── 响应 ──

data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice>? = null
)

data class Choice(
    val index: Int? = null,
    val message: ResponseMessage? = null,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class ResponseMessage(
    val role: String? = null,
    val content: String? = null
)

// ── AI 识别结果（从 content JSON 解析） ──

data class FoodRecognitionResult(
    val foods: List<RecognizedFood> = emptyList(),
    @SerializedName("total_calories") val totalCalories: Int = 0
)

data class RecognizedFood(
    val name: String = "",
    @SerializedName("weight_g") val weightG: Int = 0,
    @SerializedName("calories_kcal") val caloriesKcal: Int = 0
)
