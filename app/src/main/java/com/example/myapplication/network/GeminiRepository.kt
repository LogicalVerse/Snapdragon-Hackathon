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
        
        /**
         * Build dynamic prompt based on what reference frames are available.
         * This ensures the prompt matches the actual images being sent.
         */
        private fun buildPrompt(
            hasGoodFormFrames: Boolean,
            hasBadFormFrames: Boolean,
            goodCount: Int,
            badCount: Int,
            userCount: Int
        ): String {
            val totalImages = goodCount + badCount + userCount
            
            return buildString {
                appendLine("You are an expert fitness coach analyzing squat form.")
                appendLine()
                appendLine("I am sending you $totalImages images:")
                
                var imageIndex = 1
                if (hasGoodFormFrames) {
                    appendLine("- Images $imageIndex-${imageIndex + goodCount - 1}: GOOD FORM REFERENCE (correct technique)")
                    imageIndex += goodCount
                }
                if (hasBadFormFrames) {
                    appendLine("- Images $imageIndex-${imageIndex + badCount - 1}: BAD FORM REFERENCE (mistakes to avoid)")
                    imageIndex += badCount
                }
                appendLine("- Images $imageIndex-${imageIndex + userCount - 1}: MY WORKOUT FRAMES")
                appendLine()
                
                appendLine("Analyze my workout and provide feedback:")
                appendLine()
                appendLine("Response format (keep SHORT - 2-3 sentences):")
                appendLine("✓ Good: [What I'm doing well]")
                appendLine("⚠ Fix: [Main issue to correct, if any]")
                appendLine("💡 Tip: [One specific improvement]")
                appendLine()
                appendLine("Be encouraging but honest. If form looks good, say so!")
            }
        }
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
            
            // Load good form reference frames
            Log.d(TAG, "Loading good form frames...")
            val goodFormFrames = extractor.loadGoodFormFrames()
            if (goodFormFrames.isEmpty()) {
                Log.w(TAG, "No good form frames loaded, falling back to professional frames")
            }
            Log.d(TAG, "Loaded ${goodFormFrames.size} good form frames")
            
            // Force GC between operations
            System.gc()
            
            // Load bad form reference frames
            Log.d(TAG, "Loading bad form frames...")
            val badFormFrames = extractor.loadBadFormFrames()
            Log.d(TAG, "Loaded ${badFormFrames.size} bad form frames")
            
            // Force GC between operations
            System.gc()
            Log.d(TAG, "Memory after reference frames: ${getMemoryInfo()}")
            
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
            val totalChars = goodFormFrames.sumOf { it.length } + badFormFrames.sumOf { it.length } + userFrames.sumOf { it.length }
            Log.d(TAG, "Total payload size: ~${totalChars / 1024}KB base64 data")
            
            // Build request parts
            val parts = mutableListOf<Part>()
            
            // Build dynamic prompt based on what frames we have
            val prompt = buildPrompt(
                hasGoodFormFrames = goodFormFrames.isNotEmpty(),
                hasBadFormFrames = badFormFrames.isNotEmpty(),
                goodCount = goodFormFrames.size,
                badCount = badFormFrames.size,
                userCount = userFrames.size
            )
            Log.d(TAG, "Generated prompt: $prompt")
            
            // Add the prompt first
            parts.add(textPart(prompt))
            
            // Add good form frames (GROUP 1 - first 6)
            goodFormFrames.forEachIndexed { index, base64 ->
                Log.d(TAG, "Adding good form frame ${index + 1}: ${base64.length} chars")
                parts.add(imagePart(base64))
            }
            
            // Add bad form frames (GROUP 2 - next 6)
            badFormFrames.forEachIndexed { index, base64 ->
                Log.d(TAG, "Adding bad form frame ${index + 1}: ${base64.length} chars")
                parts.add(imagePart(base64))
            }
            
            // Add user frames (GROUP 3 - last 6)
            userFrames.forEachIndexed { index, base64 ->
                Log.d(TAG, "Adding user frame ${index + 1}: ${base64.length} chars")
                parts.add(imagePart(base64))
            }
            
            Log.d(TAG, "Built request with ${parts.size} parts (${goodFormFrames.size} good + ${badFormFrames.size} bad + ${userFrames.size} user)")
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
