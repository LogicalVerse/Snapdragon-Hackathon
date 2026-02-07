package com.example.myapplication.pose

/**
 * State machine states for squat detection
 */
enum class SquatState {
    UP,      // Standing position (knee angle > threshold)
    DOWN     // Squat position (knee angle < threshold)
}

/**
 * Real-time analysis result for each frame
 */
data class AnalysisResult(
    val kneeAngle: Float = 180f,           // Current knee angle in degrees
    val repCount: Int = 0,                  // Total reps completed
    val currentState: SquatState = SquatState.UP,
    val isFullBodyVisible: Boolean = false,
    val feedback: String = "Position yourself in frame",
    val depthPercentage: Float = 0f,         // 0 = standing, 100 = full depth
    val debugInfo: String = "",             // Added for debugging visibility issues
    val detectedSide: String = "Right"      // Added for Python overlay matching
)

/**
 * Summary of the entire workout session
 */
data class WorkoutSummary(
    val totalReps: Int = 0,
    val averageDepthAngle: Float = 180f,    // Average knee angle at bottom of each squat
    val bestDepthAngle: Float = 180f,       // Lowest knee angle achieved (deeper = better)
    val formScore: Float = 0f,              // 0-100 score based on consistency
    val totalDurationSeconds: Int = 0,
    val repDepthAngles: List<Float> = emptyList()  // Angle at bottom of each rep
) {
    // Convert angle to depth percentage (180° = 0%, 90° = 100%)
    val averageDepthPercentage: Float
        get() = ((180f - averageDepthAngle) / 90f * 100f).coerceIn(0f, 100f)
    
    val bestDepthPercentage: Float
        get() = ((180f - bestDepthAngle) / 90f * 100f).coerceIn(0f, 100f)
    
    val formLabel: String
        get() = when {
            formScore >= 80f -> "Excellent"
            formScore >= 60f -> "Good"
            formScore >= 40f -> "Fair"
            else -> "Needs Work"
        }
}
