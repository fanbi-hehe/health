package com.example.health.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── 请求 ──

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val temperature: Double = 0.3,
    val tools: List<Tool>? = null
)

// ── 工具调用（function calling） ──

data class Tool(
    val type: String = "function",
    val function: FunctionSpec
)

data class FunctionSpec(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

data class ToolCall(
    val id: String? = null,
    val type: String? = null,
    val function: FunctionCall? = null
)

data class FunctionCall(
    val name: String? = null,
    val arguments: String? = null
)

data class Message(
    val role: String,       // "user" | "system"
    val content: List<ContentPart>,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null
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
    val content: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null
)

// ── AI 识别结果（从 content JSON 解析） ──

data class FoodRecognitionResult(
    val foods: List<RecognizedFood> = emptyList(),
    @SerializedName("total_calories") val totalCalories: Int = 0
)

data class RecognizedFood(
    val name: String = "",
    @SerializedName("weight_g") val weightG: Int = 0,
    @SerializedName("calories_kcal") val caloriesKcal: Int = 0,
    @SerializedName("protein_g") val proteinG: Int = 0,
    @SerializedName("carbs_g") val carbsG: Int = 0,
    @SerializedName("fat_g") val fatG: Int = 0
)
