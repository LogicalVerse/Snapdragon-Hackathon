package com.example.myapplication.pose

/**
 * Squat state machine states (3-state model from LearnOpenCV)
 */
enum class SquatState(val displayName: String) {
    S1_NORMAL("Standing"),        // Standing - knee-vertical angle ≤ 32°
    S2_TRANSITION("Transitioning"), // Going down/up - 35°-65°
    S3_PASS("At Depth"),          // Full squat depth - 75°-95°
    UP("Ready"),                  // Legacy state for backwards compatibility
    DOWN("Squatting")             // Legacy state for backwards compatibility
}

/**
 * Push-up state machine states
 */
enum class PushUpState(val displayName: String) {
    UP("Arms Extended"),          // Arms fully extended
    TRANSITION("Moving"),         // Going down or up
    DOWN("At Bottom")             // Chest near ground
}

/**
 * Generic exercise state for trainers that use simple up/down detection
 */
enum class ExercisePhase(val displayName: String) {
    START("Ready"),
    ECCENTRIC("Going Down"),      // Lowering phase
    HOLD("Hold"),                 // At peak contraction
    CONCENTRIC("Coming Up")       // Raising phase
}

/**
 * Workout difficulty modes affecting thresholds
 */
enum class WorkoutMode(val displayName: String) {
    BEGINNER("Beginner"),
    PRO("Pro")
}

/**
 * Feedback types with display names and severity
 */
enum class FeedbackType(val displayName: String, val isSevere: Boolean = false) {
    NONE(""),
    READY("Ready"),
    
    // Squat feedback
    BEND_FORWARD("Lean forward slightly", false),
    BEND_BACKWARDS("Straighten your back", false),
    LOWER_HIPS("Go deeper!", false),
    KNEE_OVER_TOES("Knees over toes!", true),
    DEEP_SQUAT("Too deep!", true),
    
    // Push-up feedback
    HIPS_TOO_HIGH("Lower your hips", false),
    HIPS_TOO_LOW("Raise your hips", false),
    ARMS_NOT_LOCKED("Fully extend arms", false),
    ELBOWS_FLARED("Keep elbows closer", false),
    
    // Deadlift feedback
    ROUND_BACK("Keep back straight!", true),
    HIPS_TOO_EARLY("Hips rising too fast", false),
    BAR_AWAY("Keep bar close", false),
    
    // Bench press feedback
    ELBOWS_TOO_WIDE("Tuck elbows more", false),
    INCOMPLETE_LOCKOUT("Lock out fully", false),
    
    // Row feedback
    MOMENTUM("Control the weight", false),
    
    // General feedback
    FRONTAL_WARNING("Turn to side view", false),
    POSITION_BODY("Position full body", false),
    GOOD_FORM("Good form!", false),
    REP_COMPLETE("Rep complete!", false);
    
    companion object {
        fun fromString(value: String): FeedbackType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

/**
 * Analysis result returned by exercise trainers
 */
data class AnalysisResult(
    // Rep counting
    val repCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    
    // Current state
    val currentState: SquatState = SquatState.UP,
    val stateSequence: List<String> = emptyList(),
    
    // Angles (in degrees)
    val kneeAngle: Float = 180f,
    val hipAngle: Float = 180f,
    val elbowAngle: Float = 180f,
    val hipVerticalAngle: Float = 0f,
    val kneeVerticalAngle: Float = 0f,
    val ankleVerticalAngle: Float = 0f,
    
    // Feedback
    val feedbackType: FeedbackType = FeedbackType.NONE,
    val feedback: String = "",
    val isSevereFeedback: Boolean = false,
    
    // Visibility and readiness
    val isFullBodyVisible: Boolean = false,
    val isReady: Boolean = false,
    
    // Side detection
    val detectedSide: String = "Right",
    val offsetAngle: Float = 0f,
    val isFrontalView: Boolean = false,
    
    // Depth tracking
    val depthPercentage: Float = 0f,
    
    // Inactivity
    val inactivitySeconds: Float = 0f,
    
    // Debug
    val debugInfo: String = ""
)

/**
 * Individual rep information for detailed analysis
 */
data class RepInfo(
    val repNumber: Int,
    val isCorrect: Boolean,
    val depthAngle: Float,
    val depthPercentage: Float,
    val feedbackReceived: List<FeedbackType>,
    val durationMs: Long
)

/**
 * Workout summary for pause/end screen
 */
data class WorkoutSummary(
    val exerciseId: String = "squats",
    val exerciseName: String = "Squats",
    
    // Rep counts
    val totalReps: Int = 0,
    val correctReps: Int = 0,
    val incorrectReps: Int = 0,
    
    // Accuracy
    val accuracyPercentage: Float = 0f,
    
    // Depth/angle analysis
    val averageDepthAngle: Float = 180f,
    val bestDepthAngle: Float = 180f,
    val depthAngles: List<Float> = emptyList(),
    
    // Form score (0-100)
    val formScore: Float = 0f,
    val formLabel: String = "N/A",
    
    // Feedback breakdown
    val feedbackCounts: Map<String, Int> = emptyMap(),
    val mostCommonIssue: String? = null,
    
    // Timing
    val totalDurationSeconds: Int = 0,
    val activeTimeSeconds: Int = 0,
    
    // Per-rep details
    val repDetails: List<RepInfo> = emptyList(),
    
    // Mode
    val mode: WorkoutMode = WorkoutMode.BEGINNER
) {
    val formattedDuration: String
        get() {
            val minutes = totalDurationSeconds / 60
            val seconds = totalDurationSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    
    val mostCommonIssueDisplay: String
        get() = mostCommonIssue?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "None"
    
    val bestDepthPercentage: Float
        get() = ((180f - bestDepthAngle) / 90f * 100f).coerceIn(0f, 100f)
    
    val averageDepthPercentage: Float
        get() = ((180f - averageDepthAngle) / 90f * 100f).coerceIn(0f, 100f)
}

/**
 * Abstract base class for all exercise trainers
 */
abstract class ExerciseTrainer(
    val exerciseId: String,
    val exerciseName: String,
    protected var mode: WorkoutMode = WorkoutMode.BEGINNER
) {
    // Common tracking
    protected var correctCount = 0
    protected var incorrectCount = 0
    protected var startTimeMs = System.currentTimeMillis()
    protected var lastActiveTimeMs = System.currentTimeMillis()
    protected var activeSeconds = 0f
    protected var fullBodyVisibleCount = 0
    protected var isReady = false
    protected val feedbackHistory = mutableMapOf<String, Int>()
    protected val depthAngles = mutableListOf<Float>()
    
    // Per-rep tracking
    protected val repInfoList = mutableListOf<RepInfo>()
    protected var currentRepStartMs = System.currentTimeMillis()
    protected val currentRepFeedback = mutableListOf<FeedbackType>()
    
    // Visibility thresholds
    protected val visibilityThreshold = 0.5f
    protected val boundaryMargin = 0.03f
    protected val framesRequired = 3
    protected val inactiveThresholdSeconds = 15f
    
    /**
     * Analyze a single frame of pose landmarks.
     * Must be implemented by each exercise trainer.
     */
    abstract fun analyze(landmarks: List<PoseLandmark>): AnalysisResult
    
    /**
     * Get workout summary for pause/end screen.
     */
    open fun getSummary(): WorkoutSummary {
        val totalReps = correctCount + incorrectCount
        val accuracy = if (totalReps > 0) (correctCount.toFloat() / totalReps) * 100f else 0f
        
        val avgDepth = if (depthAngles.isNotEmpty()) depthAngles.average().toFloat() else 180f
        val bestDepth = depthAngles.minOrNull() ?: 180f
        
        val formScore = calculateFormScore()
        val formLabel = when {
            formScore >= 80f -> "Excellent"
            formScore >= 60f -> "Good"
            formScore >= 40f -> "Fair"
            else -> "Needs Work"
        }
        
        val mostCommon = feedbackHistory.maxByOrNull { it.value }?.key
        val duration = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
        
        return WorkoutSummary(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            totalReps = totalReps,
            correctReps = correctCount,
            incorrectReps = incorrectCount,
            accuracyPercentage = accuracy,
            averageDepthAngle = avgDepth,
            bestDepthAngle = bestDepth,
            depthAngles = depthAngles.toList(),
            formScore = formScore,
            formLabel = formLabel,
            feedbackCounts = feedbackHistory.toMap(),
            mostCommonIssue = mostCommon,
            totalDurationSeconds = duration,
            activeTimeSeconds = activeSeconds.toInt(),
            repDetails = repInfoList.toList(),
            mode = mode
        )
    }
    
    /**
     * Record a completed rep with details.
     */
    protected fun recordRep(isCorrect: Boolean, depthAngle: Float) {
        val repNumber = repInfoList.size + 1
        val durationMs = System.currentTimeMillis() - currentRepStartMs
        val depthPercentage = ((180f - depthAngle) / 90f * 100f).coerceIn(0f, 100f)
        
        repInfoList.add(RepInfo(
            repNumber = repNumber,
            isCorrect = isCorrect,
            depthAngle = depthAngle,
            depthPercentage = depthPercentage,
            feedbackReceived = currentRepFeedback.toList(),
            durationMs = durationMs
        ))
        
        currentRepFeedback.clear()
        currentRepStartMs = System.currentTimeMillis()
    }
    
    /**
     * Reset trainer state for new workout.
     */
    open fun reset(newMode: WorkoutMode? = null) {
        if (newMode != null) mode = newMode
        correctCount = 0
        incorrectCount = 0
        startTimeMs = System.currentTimeMillis()
        lastActiveTimeMs = System.currentTimeMillis()
        activeSeconds = 0f
        fullBodyVisibleCount = 0
        isReady = false
        feedbackHistory.clear()
        depthAngles.clear()
    }
    
    /**
     * Calculate form score (0-100). Can be overridden.
     */
    protected open fun calculateFormScore(): Float {
        val total = correctCount + incorrectCount
        if (total == 0) return 0f
        
        // Accuracy component (50%)
        val accuracyScore = (correctCount.toFloat() / total) * 50f
        
        // Consistency component (25%)
        val consistencyScore = if (depthAngles.size > 1) {
            val avg = depthAngles.average()
            val variance = depthAngles.map { kotlin.math.abs(it - avg) }.average()
            maxOf(0f, 25f - variance.toFloat())
        } else if (depthAngles.size == 1) 25f else 0f
        
        // Severe feedback penalty (25%)
        val severeTypes = listOf("KNEE_OVER_TOES", "DEEP_SQUAT", "ROUND_BACK")
        var severePenalty = 0f
        severeTypes.forEach { type ->
            severePenalty += (feedbackHistory[type] ?: 0) * 2
        }
        val feedbackScore = maxOf(0f, 25f - severePenalty)
        
        return minOf(100f, accuracyScore + consistencyScore + feedbackScore)
    }
    
    /**
     * Check if key body landmarks are visible.
     */
    protected fun checkBodyVisibility(
        landmarks: List<PoseLandmark>,
        requiredIndices: List<Int>
    ): Pair<Boolean, String> {
        val missing = mutableListOf<String>()
        
        for (idx in requiredIndices) {
            if (idx >= landmarks.size) {
                missing.add(getLandmarkName(idx))
                continue
            }
            
            val lm = landmarks[idx]
            val tol = boundaryMargin
            
            if (lm.x < tol || lm.x > (1f - tol) || lm.y < tol || lm.y > (1f - tol)) {
                missing.add("${getLandmarkName(idx)}(edge)")
            } else if (lm.visibility < visibilityThreshold) {
                missing.add("${getLandmarkName(idx)}(vis)")
            }
        }
        
        return if (missing.isEmpty()) {
            Pair(true, "Full body visible")
        } else {
            Pair(false, "Missing: ${missing.take(3).joinToString(", ")}")
        }
    }
    
    /**
     * Record feedback occurrence for summary.
     */
    protected fun recordFeedback(feedbackType: FeedbackType) {
        if (feedbackType != FeedbackType.NONE && feedbackType != FeedbackType.READY) {
            val key = feedbackType.name
            feedbackHistory[key] = (feedbackHistory[key] ?: 0) + 1
        }
    }
    
    /**
     * Get landmark name for debug messages.
     */
    private fun getLandmarkName(idx: Int): String = when (idx) {
        LandmarkIndex.NOSE -> "Nose"
        LandmarkIndex.LEFT_SHOULDER -> "L.Shoulder"
        LandmarkIndex.RIGHT_SHOULDER -> "R.Shoulder"
        LandmarkIndex.LEFT_HIP -> "L.Hip"
        LandmarkIndex.RIGHT_HIP -> "R.Hip"
        LandmarkIndex.LEFT_KNEE -> "L.Knee"
        LandmarkIndex.RIGHT_KNEE -> "R.Knee"
        LandmarkIndex.LEFT_ANKLE -> "L.Ankle"
        LandmarkIndex.RIGHT_ANKLE -> "R.Ankle"
        LandmarkIndex.LEFT_ELBOW -> "L.Elbow"
        LandmarkIndex.RIGHT_ELBOW -> "R.Elbow"
        LandmarkIndex.LEFT_WRIST -> "L.Wrist"
        LandmarkIndex.RIGHT_WRIST -> "R.Wrist"
        else -> "Point$idx"
    }
}
