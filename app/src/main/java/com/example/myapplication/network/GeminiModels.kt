package com.example.myapplication.network

import com.google.gson.annotations.SerializedName

/**
 * Gemini API request/response models for multimodal content generation.
 */

// Request models
data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

sealed class Part {
    data class TextPart(
        val text: String
    ) : Part()
    
    data class InlineDataPart(
        @SerializedName("inline_data")
        val inlineData: InlineData
    ) : Part()
}

data class InlineData(
    @SerializedName("mime_type")
    val mimeType: String,
    val data: String  // Base64 encoded image
)

// Response models
data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: ContentResponse?
)

data class ContentResponse(
    val parts: List<PartResponse>?
)

data class PartResponse(
    val text: String?
)

/**
 * Helper to create a text part
 */
fun textPart(text: String): Part = Part.TextPart(text)

/**
 * Helper to create an image part from base64 data
 */
fun imagePart(base64Data: String, mimeType: String = "image/jpeg"): Part = 
    Part.InlineDataPart(InlineData(mimeType, base64Data))
