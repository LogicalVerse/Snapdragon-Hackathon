package com.example.myapplication.pose.trainers

import com.example.myapplication.pose.*
import kotlin.math.abs

/**
 * Bicep Curl state machine states
 */
enum class CurlState(val displayName: String) {
    EXTENDED("Arms Extended"),    // Arm straight (elbow > 150°)
    CURLING("Curling"),           // Mid-curl (50° - 150°)
    CONTRACTED("Contracted")      // Full curl (elbow < 50°)
}

/**
 * Bicep Curl exercise trainer.
 * Tracks elbow angle through curl motion.
 */
class BicepCurlTrainer(mode: WorkoutMode = WorkoutMode.BEGINNER) : ExerciseTrainer(
    exerciseId = "bicep_curl",
    exerciseName = "Bicep Curls",
    mode = mode
) {
    // State thresholds
    private val extendedMinAngle = if (mode == WorkoutMode.BEGINNER) 145f else 150f
    private val contractedMaxAngle = if (mode == WorkoutMode.BEGINNER) 55f else 50f
    
    // State machine
    private var currentState = CurlState.EXTENDED
    private var prevState = CurlState.EXTENDED
    private val stateSequence = mutableListOf<String>()
    
    // Tracking
    private var minElbowAngleThisRep = 180f
    private var lastElbowAngle = 180f
    private var hasSevereFeedbackThisRep = false
    private var repStartTimeMs = System.currentTimeMillis()
    
    // Movement velocity tracking (for momentum detection)
    private var angleHistory = mutableListOf<Float>()
    private val historySize = 5
    
    override fun analyze(landmarks: List<PoseLandmark>): AnalysisResult {
        if (landmarks.size < 33) {
            return createResult(
                feedbackType = FeedbackType.POSITION_BODY,
                feedback = "Position yourself in frame"
            )
        }
        
        // Calculate elbow angles for both arms
        val leftElbowAngle = AngleUtils.calculateElbowAngle(landmarks, true)
        val rightElbowAngle = AngleUtils.calculateElbowAngle(landmarks, false)
        
        // Use the arm with smaller angle (more curled)
        val (side, elbowAngle) = if (leftElbowAngle < rightElbowAngle) {
            "Left" to leftElbowAngle
        } else {
            "Right" to rightElbowAngle
        }
        
        // Track angle history for velocity detection
        angleHistory.add(elbowAngle)
        if (angleHistory.size > historySize) {
            angleHistory.removeAt(0)
        }
        
        // Track minimum angle for depth
        if (elbowAngle < minElbowAngleThisRep) {
            minElbowAngleThisRep = elbowAngle
        }
        
        // Check for movement (inactivity detection)
        val currentTimeMs = System.currentTimeMillis()
        val angleChange = abs(elbowAngle - lastElbowAngle)
        
        if (angleChange > 3f) {
            lastActiveTimeMs = currentTimeMs
            activeSeconds += 0.033f
        }
        
        val inactivitySeconds = (currentTimeMs - lastActiveTimeMs) / 1000f
        
        if (inactivitySeconds > inactiveThresholdSeconds) {
            correctCount = 0
            incorrectCount = 0
            stateSequence.clear()
            lastActiveTimeMs = currentTimeMs
        }
        
        lastElbowAngle = elbowAngle
        
        // STATE MACHINE
        val newState = determineState(elbowAngle)
        
        // State transition logic
        if (newState != currentState) {
            prevState = currentState
            currentState = newState
            
            when (newState) {
                CurlState.CURLING -> {
                    if (stateSequence.isEmpty() || stateSequence.lastOrNull() == CurlState.CONTRACTED.name) {
                        if (stateSequence.size < 3) {
                            stateSequence.add(newState.name)
                        }
                    }
                }
                CurlState.CONTRACTED -> {
                    if (stateSequence.lastOrNull() == CurlState.CURLING.name) {
                        if (stateSequence.size < 3) {
                            stateSequence.add(newState.name)
                        }
                    }
                }
                CurlState.EXTENDED -> {
                    // REP COUNTED HERE
                    updateCounters()
                }
            }
        }
        
        // Determine feedback
        var feedbackType = FeedbackType.NONE
        var feedbackMessage = ""
        var isSevere = false
        
        when (currentState) {
            CurlState.EXTENDED -> {
                feedbackType = FeedbackType.READY
                feedbackMessage = "Ready - curl up!"
            }
            CurlState.CURLING, CurlState.CONTRACTED -> {
                val (ft, msg, severe) = determineFeedback(elbowAngle)
                feedbackType = ft
                feedbackMessage = msg
                isSevere = severe
                
                if (severe) {
                    hasSevereFeedbackThisRep = true
                }
                
                if (feedbackType != FeedbackType.NONE && feedbackType != FeedbackType.GOOD_FORM) {
                    currentRepFeedback.add(feedbackType)
                }
                
                recordFeedback(feedbackType)
            }
        }
        
        // Calculate depth percentage (0-100, 100 = full curl)
        val depthPercentage = ((180f - elbowAngle) / 130f * 100f).coerceIn(0f, 100f)
        
        return AnalysisResult(
            repCount = correctCount + incorrectCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            currentState = SquatState.UP, // Using generic state for UI
            stateSequence = stateSequence.toList(),
            elbowAngle = elbowAngle,
            feedbackType = feedbackType,
            feedback = feedbackMessage,
            isSevereFeedback = isSevere,
            isFullBodyVisible = true,
            isReady = true,
            detectedSide = side,
            depthPercentage = depthPercentage,
            inactivitySeconds = inactivitySeconds,
            debugInfo = "State: ${currentState.name}, Elbow: ${elbowAngle.toInt()}°"
        )
    }
    
    private fun determineState(elbowAngle: Float): CurlState {
        return when {
            elbowAngle >= extendedMinAngle -> CurlState.EXTENDED
            elbowAngle <= contractedMaxAngle -> CurlState.CONTRACTED
            else -> CurlState.CURLING
        }
    }
    
    private fun determineFeedback(elbowAngle: Float): Triple<FeedbackType, String, Boolean> {
        // Check for momentum (too fast movement)
        if (angleHistory.size >= historySize) {
            val velocity = abs(angleHistory.last() - angleHistory.first()) / historySize
            if (velocity > 15f) {
                return Triple(FeedbackType.MOMENTUM, "Control the movement", false)
            }
        }
        
        // Check if not reaching full contraction
        if (currentState == CurlState.CURLING && elbowAngle in 55f..80f) {
            return Triple(FeedbackType.LOWER_HIPS, "Curl higher!", false)
        }
        
        // Good form feedback
        return when (currentState) {
            CurlState.CONTRACTED -> Triple(FeedbackType.GOOD_FORM, "Good! Now extend", false)
            else -> Triple(FeedbackType.NONE, "", false)
        }
    }
    
    private fun updateCounters() {
        if (stateSequence.isEmpty()) return
        
        val hasContracted = stateSequence.contains(CurlState.CONTRACTED.name)
        
        if (hasContracted && !hasSevereFeedbackThisRep) {
            correctCount++
            if (minElbowAngleThisRep < 180f) {
                depthAngles.add(minElbowAngleThisRep)
            }
            recordRep(true, minElbowAngleThisRep)
        } else if (stateSequence.isNotEmpty()) {
            incorrectCount++
            recordRep(false, minElbowAngleThisRep)
        }
        
        stateSequence.clear()
        minElbowAngleThisRep = 180f
        hasSevereFeedbackThisRep = false
    }
    
    private fun createResult(
        feedbackType: FeedbackType = FeedbackType.NONE,
        feedback: String = ""
    ): AnalysisResult {
        return AnalysisResult(
            repCount = correctCount + incorrectCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            feedbackType = feedbackType,
            feedback = feedback
        )
    }
    
    override fun reset(newMode: WorkoutMode?) {
        super.reset(newMode)
        currentState = CurlState.EXTENDED
        prevState = CurlState.EXTENDED
        stateSequence.clear()
        minElbowAngleThisRep = 180f
        lastElbowAngle = 180f
        hasSevereFeedbackThisRep = false
        angleHistory.clear()
    }
}
