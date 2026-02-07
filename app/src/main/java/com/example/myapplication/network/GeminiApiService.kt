package com.example.myapplication.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Gemini API.
 */
interface GeminiApiService {
    
    @POST("v1beta/models/gemini-3-flash-preview:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
    
    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
    }
}
