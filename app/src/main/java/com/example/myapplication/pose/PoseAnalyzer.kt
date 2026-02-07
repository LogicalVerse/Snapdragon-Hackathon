package com.example.myapplication.pose

import android.util.Log
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Analyzes pose landmarks to count squat reps and provide form feedback
 * Ported from Python pose_test.py logic
 */
class PoseAnalyzer {
    
    companion object {
        private const val TAG = "PoseAnalyzer"
        
        // Angle thresholds for squat detection (from Python code)
        private const val DEPTH_ANGLE = 100f    // Knee angle at bottom of squat
        private const val STAND_ANGLE = 160f    // Knee angle when standing
        
        // Visibility threshold for landmarks
        private const val VISIBILITY_THRESHOLD = 0.5f
        
        // How many consecutive frames of full body visibility before counting starts
        private const val FULL_BODY_REQUIRED_FRAMES = 3
    }
    
    // State machine
    private var currentState = SquatState.UP
    private var reachedDepth = false
    private var repCount = 0
    
    // Full body visibility tracking
    private var fullBodyVisibleCount = 0
    private var isReady = false
    
    // Stats tracking
    private var minKneeAngleThisRep = 180f
    private val repDepthAngles = mutableListOf<Float>()
    private var startTimeMs: Long = System.currentTimeMillis()
    
    /**
     * Analyze a single frame of pose landmarks
     */
    fun analyzePose(landmarks: List<PoseLandmark>): AnalysisResult {
        if (landmarks.size < 33) {
            return AnalysisResult(
                feedback = "Pose not fully detected",
                isFullBodyVisible = false
            )
        }
        
        // Check full body visibility
        val fullBodyVisible = isFullBodyVisible(landmarks)
        
        if (fullBodyVisible) {
            fullBodyVisibleCount++
        } else {
            fullBodyVisibleCount = 0
            isReady = false
        }
        
        if (fullBodyVisibleCount >= FULL_BODY_REQUIRED_FRAMES) {
             if (!isReady) {
                 Log.d(TAG, "Full body detected. Starting squat count.")
             }
             isReady = true
         }
         
         // Validation messages for debug overlay
         val validationErrors = getVisibilityValidation(landmarks)
         val debugMessage = if (validationErrors.isEmpty()) "Full Body Visible" else "Missing: ${validationErrors.joinToString(", ")}"
         
         // FORCE ANALYSIS for debugging even if not ready
         // Use the smaller angle (more bent leg) for detection
         val leftKneeAngle = calculateKneeAngle(landmarks, isLeft = true)
         val rightKneeAngle = calculateKneeAngle(landmarks, isLeft = false)
         val kneeAngle = minOf(leftKneeAngle, rightKneeAngle)
         val side = if (leftKneeAngle < rightKneeAngle) "Left" else "Right"
         
         // Only run state machine if strictly ready OR for debug testing
         val effectiveKneeAngle = if (kneeAngle < 180) kneeAngle else 180f
         
         // Track minimum angle for this rep
         if (currentState == SquatState.DOWN || effectiveKneeAngle < minKneeAngleThisRep) {
             minKneeAngleThisRep = minOf(minKneeAngleThisRep, effectiveKneeAngle)
         }
         
         // State machine logic (from Python)
         // Allow state machine to run even if not fully ready to test logic? 
         // For now, let's keep strict "isReady" for counting, but visualize angles
         var feedback = ""
         
         if (isReady) {
            // Standard Logic
        when (currentState) {
            SquatState.UP -> {
                if (kneeAngle < DEPTH_ANGLE) {
                    currentState = SquatState.DOWN
                    reachedDepth = true
                    feedback = "Good depth! Now stand up."
                    Log.d(TAG, "Transition: UP -> DOWN (angle: $kneeAngle)")
                } else if (kneeAngle < STAND_ANGLE) {
                    feedback = "Go deeper..."
                } else {
                    feedback = "Ready - squat down"
                }
            }
            SquatState.DOWN -> {
                if (kneeAngle > STAND_ANGLE && reachedDepth) {
                    repCount++
                    repDepthAngles.add(minKneeAngleThisRep)
                    Log.d(TAG, "Rep completed! Count: $repCount, depth angle: $minKneeAngleThisRep")
                    
                    currentState = SquatState.UP
                    reachedDepth = false
                    minKneeAngleThisRep = 180f
                    feedback = "Rep $repCount complete!"
                } else if (kneeAngle > DEPTH_ANGLE) {
                    feedback = "Stand all the way up"
                } else {
                    feedback = "Holding at depth..."
                }
            }
        }
        
        } else {
            // Not ready feedback
             feedback = if (validationErrors.isNotEmpty()) "Show: ${validationErrors.first()}" else "Stand still..."
        }
        
        // Calculate depth percentage (180° = 0%, 90° = 100%)
        val depthPercentage = ((180f - effectiveKneeAngle) / 90f * 100f).coerceIn(0f, 100f)
        
        return AnalysisResult(
            kneeAngle = effectiveKneeAngle,
            repCount = repCount,
            currentState = currentState,
            isFullBodyVisible = isReady, // Report readiness state
            feedback = feedback,
            depthPercentage = depthPercentage,
            debugInfo = debugMessage,
            detectedSide = side
        )
    }
    
    /**
     * Check visibility of key landmarks and return list of missing ones
     */
    private fun getVisibilityValidation(landmarks: List<PoseLandmark>): List<String> {
        val missing = mutableListOf<String>()
        val keyIndices = mapOf(
            LandmarkIndex.LEFT_SHOULDER to "L.Shoulder",
            LandmarkIndex.RIGHT_SHOULDER to "R.Shoulder",
            LandmarkIndex.LEFT_HIP to "L.Hip",
            LandmarkIndex.RIGHT_HIP to "R.Hip",
            LandmarkIndex.LEFT_KNEE to "L.Knee",
            LandmarkIndex.RIGHT_KNEE to "R.Knee",
            LandmarkIndex.LEFT_ANKLE to "L.Ankle",
            LandmarkIndex.RIGHT_ANKLE to "R.Ankle"
        )
        
        for ((idx, name) in keyIndices) {
            if (idx >= landmarks.size) continue
            val lm = landmarks[idx]
            
            // Check boundaries
             val tol = 0.01f // Relaxed tolerance
            if (lm.x < tol || lm.x > (1f - tol) || lm.y < tol || lm.y > (1f - tol)) {
                missing.add(name)
            } else if (lm.visibility < 0.3f) { // Relaxed visibility threshold
                missing.add("$name(low conf)")
            }
        }
        return missing
    }
    
    /**
     * Get workout summary for pause screen
     */
    fun getWorkoutSummary(): WorkoutSummary {
        val durationSeconds = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
        
        val avgAngle = if (repDepthAngles.isNotEmpty()) {
            repDepthAngles.average().toFloat()
        } else 180f
        
        val bestAngle = repDepthAngles.minOrNull() ?: 180f
        
        // Calculate form score based on consistency of depth
        val formScore = calculateFormScore()
        
        return WorkoutSummary(
            totalReps = repCount,
            averageDepthAngle = avgAngle,
            bestDepthAngle = bestAngle,
            formScore = formScore,
            totalDurationSeconds = durationSeconds,
            repDepthAngles = repDepthAngles.toList()
        )
    }
    
    /**
     * Calculate form score based on depth consistency and achieving proper depth
     */
    private fun calculateFormScore(): Float {
        if (repDepthAngles.isEmpty()) return 0f
        
        // Base score: Did they reach good depth? (< 100° is good)
        val depthScore = repDepthAngles.count { it < DEPTH_ANGLE } / repDepthAngles.size.toFloat() * 50f
        
        // Consistency score: Low variance in depth angles
        val avgAngle = repDepthAngles.average()
        val variance = repDepthAngles.map { abs(it - avgAngle) }.average()
        val consistencyScore = (1 - (variance / 30f).coerceIn(0.0, 1.0)).toFloat() * 50f
        
        return (depthScore + consistencyScore).coerceIn(0f, 100f)
    }
    
    /**
     * Reset the analyzer for a new workout
     */
    fun reset() {
        currentState = SquatState.UP
        reachedDepth = false
        repCount = 0
        fullBodyVisibleCount = 0
        isReady = false
        minKneeAngleThisRep = 180f
        repDepthAngles.clear()
        startTimeMs = System.currentTimeMillis()
    }
    
    /**
     * Check if key body landmarks are visible (from Python is_full_body_visible)
     */
    private fun isFullBodyVisible(landmarks: List<PoseLandmark>): Boolean {
        // Check key landmarks: shoulders, hips, knees, wrists
        val keyIndices = listOf(
            LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.RIGHT_SHOULDER,
            LandmarkIndex.LEFT_HIP, LandmarkIndex.RIGHT_HIP,
            LandmarkIndex.LEFT_KNEE, LandmarkIndex.RIGHT_KNEE,
            LandmarkIndex.LEFT_WRIST, LandmarkIndex.RIGHT_WRIST,
            LandmarkIndex.NOSE
        )
        
        for (idx in keyIndices) {
            if (idx >= landmarks.size) return false
            
            val lm = landmarks[idx]
            
            // Check if landmark is within frame (with 3% margin)
            val tol = 0.03f
            if (lm.x < tol || lm.x > (1f - tol) || lm.y < tol || lm.y > (1f - tol)) {
                return false
            }
            
            // Check visibility
            if (lm.visibility < VISIBILITY_THRESHOLD) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Calculate knee angle (hip-knee-ankle/foot angle)
     * From Python angle_at_point function
     */
    private fun calculateKneeAngle(landmarks: List<PoseLandmark>, isLeft: Boolean): Float {
        val hipIdx = if (isLeft) LandmarkIndex.LEFT_HIP else LandmarkIndex.RIGHT_HIP
        val kneeIdx = if (isLeft) LandmarkIndex.LEFT_KNEE else LandmarkIndex.RIGHT_KNEE
        val ankleIdx = if (isLeft) LandmarkIndex.LEFT_ANKLE else LandmarkIndex.RIGHT_ANKLE
        
        if (hipIdx >= landmarks.size || kneeIdx >= landmarks.size || ankleIdx >= landmarks.size) {
            return 180f
        }
        
        val hip = landmarks[hipIdx]
        val knee = landmarks[kneeIdx]
        val ankle = landmarks[ankleIdx]
        
        return angleAtPoint(
            floatArrayOf(hip.x, hip.y),
            floatArrayOf(knee.x, knee.y),
            floatArrayOf(ankle.x, ankle.y)
        )
    }
    
    /**
     * Calculate angle at point B formed by points A-B-C
     * Direct port of Python angle_at_point function
     */
    private fun angleAtPoint(a: FloatArray, b: FloatArray, c: FloatArray): Float {
        val v1 = floatArrayOf(a[0] - b[0], a[1] - b[1])
        val v2 = floatArrayOf(c[0] - b[0], c[1] - b[1])
        
        val n1 = sqrt(v1[0] * v1[0] + v1[1] * v1[1])
        val n2 = sqrt(v2[0] * v2[0] + v2[1] * v2[1])
        
        if (n1 == 0f || n2 == 0f) return 0f
        
        var cosTheta = (v1[0] * v2[0] + v1[1] * v2[1]) / (n1 * n2)
        cosTheta = cosTheta.coerceIn(-1f, 1f)
        
        val theta = acos(cosTheta)
        return (theta * 180f / Math.PI).toFloat()
    }
}
