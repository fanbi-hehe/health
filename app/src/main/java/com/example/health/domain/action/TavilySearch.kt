package com.example.health.domain.action

import android.content.Context
import com.example.health.data.preference.AppPreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 通过 Tavily Search API 联网搜索，返回摘要文本给 AI 引用。
 * 未配置 API Key 时返回提示，不影响其他功能。
 */
class TavilySearch(private val context: Context) {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        val apiKey = AppPreferences(context).tavilyApiKey.first()
        if (apiKey.isBlank()) {
            return@withContext "搜索功能未配置：请在设置页填写 Tavily API Key。"
        }

        try {
            val bodyJson = gson.toJson(
                mapOf(
                    "api_key" to apiKey,
                    "query" to query,
                    "max_results" to 3,
                    "search_depth" to "basic"
                )
            )
            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "搜索失败：HTTP ${response.code}"
                }
                val body = response.body?.string() ?: return@withContext "搜索无结果"
                val parsed = gson.fromJson(body, TavilyResponse::class.java)
                if (parsed.results.isNullOrEmpty()) return@withContext "搜索无结果"

                parsed.results.take(3).joinToString("\n\n") { r ->
                    buildString {
                        append("【${r.title ?: "无标题"}】")
                        r.content?.takeIf { it.isNotBlank() }?.let { append("\n$it") }
                        r.url?.let { append("\n来源：$it") }
                    }
                }
            }
        } catch (e: Exception) {
            "搜索失败：${e.message ?: "未知错误"}"
        }
    }

    private data class TavilyResponse(
        val results: List<TavilyResult>? = null
    )

    private data class TavilyResult(
        val title: String? = null,
        val url: String? = null,
        val content: String? = null
    )
}
