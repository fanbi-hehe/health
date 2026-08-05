package com.example.health.data.remote.api

import com.example.health.data.remote.dto.ChatRequest
import com.example.health.data.remote.dto.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * 动态 URL（用户在设置中配置 API Base URL）—— 不使用 baseUrl 常量。
 */
interface ApiService {

    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
