package com.example.myapplication.network

import android.content.Context
import android.util.Log
import com.example.myapplication.video.VideoFrameExtractor
import com.google.gson.GsonBuilder
import com.google.gson.JsonSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repository for Gemini AI form analysis.
 * Uses memory-efficient processing to prevent OOM crashes.
 */
class GeminiRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "GeminiRepository"
        private const val API_KEY = "AIzaSyBkAKYCgJ7I6veulBF3vxVIF21RHsXpkWM"
        
        private const val COMPARISON_PROMPT = """You are an expert fitness coach analyzing squat form.

Analyze the images I am sharing and give feedback ONLY on the pose shown in MY workout frames (the user's frames).

Focus on:
1. Depth - am I squatting deep enough?
2. Back position - is my spine neutral or rounded?
3. Knee tracking - are my knees tracking properly over my toes?
4. Overall movement quality

Keep your response SHORT and ENCOURAGING (2-3 sentences max). Start with what I'm doing well, then give ONE key improvement tip."""
    }
    
    // Lazy initialization to prevent crashes during construction
    private val frameExtractor by lazy {
        try {
            VideoFrameExtractor(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create VideoFrameExtractor", e)
            null
        }
    }
    
    // Custom Gson to handle sealed class serialization
    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Part::class.java, JsonSerializer<Part> { src, _, context ->
                when (src) {
                    is Part.TextPart -> context.serialize(src)
                    is Part.InlineDataPart -> context.serialize(src)
                }
            })
            .create()
    }
    
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)  // Increased timeout for large payloads
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GeminiApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    private val apiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }
    
    /**
     * Analyze user's squat form by comparing with professional reference.
     * Uses memory-efficient processing to prevent OOM.
     * 
     * @param videoUri URI of the user's workout video
     * @return AI-generated feedback text, or error Result if analysis failed
     */
    suspend fun analyzeSquatForm(videoUri: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Starting Gemini Analysis ===")
            Log.d(TAG, "Video URI: $videoUri")
            
            val extractor = frameExtractor
            if (extractor == null) {
                Log.e(TAG, "Frame extractor is null!")
                return@withContext Result.failure(Exception("Video processor unavailable"))
            }
            
            // Force GC before heavy operations
            System.gc()
            Log.d(TAG, "Memory before loading: ${getMemoryInfo()}")
            
            // Load professional reference frames
            Log.d(TAG, "Loading professional frames...")
            val professionalFrames = extractor.loadProfessionalFrames()
            if (professionalFrames.isEmpty()) {
                Log.e(TAG, "No professional frames loaded!")
                return@withContext Result.failure(Exception("Failed to load reference frames"))
            }
            Log.d(TAG, "Loaded ${professionalFrames.size} professional frames")
            
            // Force GC between operations
            System.gc()
            Log.d(TAG, "Memory after professional frames: ${getMemoryInfo()}")
            
            // Extract user's frames from video
            Log.d(TAG, "Extracting user frames...")
            val userFrames = extractor.extractFrames(videoUri, 6)
            if (userFrames.isEmpty()) {
                Log.e(TAG, "No user frames extracted!")
                return@withContext Result.failure(Exception("Failed to extract video frames"))
            }
            Log.d(TAG, "Extracted ${userFrames.size} user frames")
            
            // Force GC before building request
            System.gc()
            Log.d(TAG, "Memory after user frames: ${getMemoryInfo()}")
            
            // Calculate total payload size for logging
            val totalChars = professionalFrames.sumOf { it.length } + userFrames.sumOf { it.length }
            Log.d(TAG, "Total payload size: ~${totalChars / 1024}KB base64 data")
            
            // Build request parts
            val parts = mutableListOf<Part>()
            
            // Add the prompt
            parts.add(textPart(COMPARISON_PROMPT))
            
            // Add professional frames (first 6)
            professionalFrames.forEachIndexed { index, base64 ->
                Log.d(TAG, "Adding professional frame ${index + 1}: ${base64.length} chars")
                parts.add(imagePart(base64))
            }
            
            // Add user frames (last 6)
            userFrames.forEachIndexed { index, base64 ->
                Log.d(TAG, "Adding user frame ${index + 1}: ${base64.length} chars")
                parts.add(imagePart(base64))
            }
            
            Log.d(TAG, "Built request with ${parts.size} parts")
            Log.d(TAG, "Memory before API call: ${getMemoryInfo()}")
            
            // Make API request
            val request = GeminiRequest(
                contents = listOf(Content(parts = parts))
            )
            
            Log.d(TAG, "Sending request to Gemini API...")
            val response = apiService.generateContent(API_KEY, request)
            
            Log.d(TAG, "Response received: code=${response.code()}, success=${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                val feedbackText = responseBody?.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()
                    ?.text
                
                if (feedbackText != null) {
                    Log.d(TAG, "=== Analysis SUCCESS ===")
                    Log.d(TAG, "Feedback: $feedbackText")
                    return@withContext Result.success(feedbackText)
                } else {
                    Log.e(TAG, "Empty response body from Gemini API")
                    Log.e(TAG, "Response body: $responseBody")
                    return@withContext Result.failure(Exception("Empty AI response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "=== API ERROR ===")
                Log.e(TAG, "Code: ${response.code()}")
                Log.e(TAG, "Error: $errorBody")
                return@withContext Result.failure(Exception("API error ${response.code()}"))
            }
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "=== OUT OF MEMORY ===", e)
            System.gc()
            return@withContext Result.failure(Exception("Not enough memory for analysis"))
        } catch (e: Exception) {
            Log.e(TAG, "=== ANALYSIS FAILED ===", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get current memory usage info for debugging
     */
    private fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMB = runtime.maxMemory() / 1024 / 1024
        return "${usedMB}MB / ${maxMB}MB"
    }
}
