package com.example.myapplication.data

import androidx.compose.ui.graphics.Color

enum class FeedbackSeverity {
    SUCCESS, WARNING, CRITICAL, INFO
}

data class FeedbackMessage(
    val text: String,
    val severity: FeedbackSeverity = FeedbackSeverity.INFO,
    val metric: String? = null, // e.g., "Depth", "Speed", "Alignment"
    val metricValue: Float = 0.5f, // 0.0 to 1.0
    val timestamp: Long = System.currentTimeMillis()
)

data class FormQualityScore(
    val overallScore: Float = 0.85f, // 0.0 to 1.0
    val depth: Float = 0.9f,
    val speed: Float = 0.8f,
    val alignment: Float = 0.85f,
    val balance: Float = 0.75f
) {
    val label: String
        get() = when {
            overallScore >= 0.8f -> "Excellent"
            overallScore >= 0.6f -> "Good"
            else -> "Needs Work"
        }

    val scoreColor: Color
        get() = when {
            overallScore >= 0.8f -> Color(0xFF00E676) // Green
            overallScore >= 0.6f -> Color(0xFFFFC107) // Yellow
            else -> Color(0xFFFF3B30) // Red
        }
}

data class WorkoutState(
    val exerciseName: String = "Squats",
    val currentReps: Int = 0,
    val targetReps: Int = 15,
    val elapsedTimeSeconds: Int = 154, // 02:34
    val isRecording: Boolean = true,
    val isPaused: Boolean = false,
    val isConnected: Boolean = true,
    val formQuality: FormQualityScore = FormQualityScore(),
                val currentFeedback: FeedbackMessage = FeedbackMessage(
                    text = "Perfect form! Keep going!",
                    severity = FeedbackSeverity.SUCCESS
                ),
    val repHistory: List<Int> = emptyList(),
    val totalCalories: Float = 45.5f
) {
    val formattedTime: String
        get() {
            val minutes = elapsedTimeSeconds / 60
            val seconds = elapsedTimeSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val repProgress: Float
        get() = (currentReps.toFloat() / targetReps).coerceIn(0f, 1f)
}

// Sample feedback messages for various scenarios
object SampleFeedbackMessages {
    val perfectForm = FeedbackMessage(
        text = "Perfect form! Keep going!",
        severity = FeedbackSeverity.SUCCESS,
        metric = "Depth",
        metricValue = 0.95f
    )

    val goDeeper = FeedbackMessage(
        text = "Go deeper on your squat",
        severity = FeedbackSeverity.WARNING,
        metric = "Depth",
        metricValue = 0.65f
    )

    val keepBackStraight = FeedbackMessage(
        text = "Keep your back straight",
        severity = FeedbackSeverity.CRITICAL,
        metric = "Alignment",
        metricValue = 0.45f
    )

    val slowDown = FeedbackMessage(
        text = "Slow down your movement",
        severity = FeedbackSeverity.WARNING,
        metric = "Speed",
        metricValue = 0.3f
    )

    val excellent = FeedbackMessage(
        text = "Excellent form on that rep!",
        severity = FeedbackSeverity.SUCCESS,
        metric = null,
        metricValue = 0f
    )
}
