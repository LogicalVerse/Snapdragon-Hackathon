package com.example.myapplication.pose.trainers

import com.example.myapplication.pose.*
import kotlin.math.abs

/**
 * Threshold configuration for squat state detection and feedback.
 * Based on LearnOpenCV AI Fitness Trainer.
 */
data class SquatThresholds(
    // State thresholds (knee-vertical angle in degrees)
    val stateS1Max: Float = 32f,      // Max angle to be in S1 (Standing)
    val stateS2Min: Float = 35f,      // Min angle for S2 (Transition)
    val stateS2Max: Float = 65f,      // Max angle for S2
    val stateS3Min: Float = 75f,      // Min angle for S3 (Full depth)
    val stateS3Max: Float = 95f,      // Max angle for S3
    
    // Feedback thresholds
    val hipVerticalMin: Float = 20f,      // Below this = "lean forward"
    val hipVerticalMax: Float = 45f,      // Above this = "lean back"
    val lowerHipsMin: Float = 50f,        // Hip-knee-vertical for "lower hips"
    val lowerHipsMax: Float = 80f,
    val kneeOverToes: Float = 30f,        // Knee-ankle-vertical threshold
    val deepSquat: Float = 95f,           // Beyond this = too deep
    
    // View detection
    val offsetThresh: Float = 45f         // Frontal view warning
) {
    companion object {
        val BEGINNER = SquatThresholds(
            stateS1Max = 35f,
            stateS2Min = 38f,
            stateS2Max = 68f,
            stateS3Min = 72f,
            stateS3Max = 98f,
            hipVerticalMin = 18f,
            hipVerticalMax = 48f,
            lowerHipsMin = 48f,
            lowerHipsMax = 82f,
            kneeOverToes = 35f,
            deepSquat = 98f
        )
        
        val PRO = SquatThresholds(
            stateS1Max = 32f,
            stateS2Min = 35f,
            stateS2Max = 65f,
            stateS3Min = 75f,
            stateS3Max = 95f,
            hipVerticalMin = 20f,
            hipVerticalMax = 45f,
            lowerHipsMin = 50f,
            lowerHipsMax = 80f,
            kneeOverToes = 30f,
            deepSquat = 95f
        )
    }
}

/**
 * Squat exercise trainer implementing robust 3-state machine from LearnOpenCV.
 * 
 * State Machine Logic:
 * - S1 (Normal): Standing position, knee-vertical angle ≤ 32°
 * - S2 (Transition): Going down or coming up, angle 35°-65°
 * - S3 (Pass): Full squat depth, angle 75°-95°
 * 
 * Rep Counting:
 * - Maintains state_sequence list during squat
 * - Correct rep: sequence contains S3 (reached full depth)
 * - Incorrect rep: went to S2 but never reached S3
 * - Rep counted ONLY when returning to S1
 * 
 * NO position constraints - works as long as landmarks are detected.
 */
class SquatTrainer(mode: WorkoutMode = WorkoutMode.BEGINNER) : ExerciseTrainer(
    exerciseId = "squats",
    exerciseName = "Squats",
    mode = mode
) {
    private var thresholds = if (mode == WorkoutMode.BEGINNER) SquatThresholds.BEGINNER else SquatThresholds.PRO
    
    // State machine
    private var currentState = SquatState.S1_NORMAL
    private var prevState = SquatState.S1_NORMAL
    private val stateSequence = mutableListOf<String>()
    
    // Tracking
    private var minKneeAngleThisRep = 180f
    private var lastKneeVertical = 0f
    private var hasSevereFeedbackThisRep = false
    
    override fun analyze(landmarks: List<PoseLandmark>): AnalysisResult {
        if (landmarks.size < 33) {
            return createResult(
                feedbackType = FeedbackType.POSITION_BODY,
                feedback = "Position yourself in frame"
            )
        }
        
        // Get landmark coordinates - NO strict visibility checks
        val nose = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.NOSE)
        val lShoulder = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_SHOULDER)
        val rShoulder = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.RIGHT_SHOULDER)
        val lHip = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_HIP)
        val rHip = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.RIGHT_HIP)
        val lKnee = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_KNEE)
        val rKnee = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.RIGHT_KNEE)
        val lAnkle = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_ANKLE)
        val rAnkle = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.RIGHT_ANKLE)
        
        // Check frontal view (optional warning, not blocking)
        val offsetAngle = AngleUtils.offsetAngle(nose, lShoulder, rShoulder)
        val isFrontal = offsetAngle > thresholds.offsetThresh
        
        // Determine which side is more visible (use the one with lower knee angle = more bent)
        val leftKneeAngle = AngleUtils.calculateKneeAngle(landmarks, true)
        val rightKneeAngle = AngleUtils.calculateKneeAngle(landmarks, false)
        
        val (side, shoulder, hip, knee, ankle, kneeAngle) = if (leftKneeAngle < rightKneeAngle) {
            SideLandmarks("Left", lShoulder, lHip, lKnee, lAnkle, leftKneeAngle)
        } else {
            SideLandmarks("Right", rShoulder, rHip, rKnee, rAnkle, rightKneeAngle)
        }
        
        // Calculate angles with vertical (core of the analysis)
        val hipVertical = AngleUtils.angleWithVertical(shoulder, hip)
        val kneeVertical = AngleUtils.angleWithVertical(hip, knee)
        val ankleVertical = AngleUtils.angleWithVertical(knee, ankle)
        
        // Track minimum knee angle for depth recording
        if (kneeAngle < minKneeAngleThisRep) {
            minKneeAngleThisRep = kneeAngle
        }
        
        // Check inactivity
        val currentTimeMs = System.currentTimeMillis()
        val angleChange = abs(kneeVertical - lastKneeVertical)
        
        if (angleChange > 2f) {  // Reduced threshold for sensitivity
            lastActiveTimeMs = currentTimeMs
            activeSeconds += 0.033f
        }
        
        val inactivitySeconds = (currentTimeMs - lastActiveTimeMs) / 1000f
        
        // Reset counters on prolonged inactivity
        if (inactivitySeconds > inactiveThresholdSeconds) {
            correctCount = 0
            incorrectCount = 0
            stateSequence.clear()
            lastActiveTimeMs = currentTimeMs
        }
        
        lastKneeVertical = kneeVertical
        
        // STATE MACHINE - Core logic from LearnOpenCV
        val newState = determineState(kneeVertical)
        val isGoingDown = kneeVertical > lastKneeVertical
        
        // State transition logic
        if (newState != currentState) {
            prevState = currentState
            currentState = newState
            
            // Add to sequence (max 3 states: [s2, s3, s2])
            when (newState) {
                SquatState.S2_TRANSITION -> {
                    // Only add if sequence is empty or last was S3 (coming up)
                    if (stateSequence.isEmpty() || stateSequence.lastOrNull() == SquatState.S3_PASS.name) {
                        if (stateSequence.size < 3) {
                            stateSequence.add(newState.name)
                        }
                    }
                }
                SquatState.S3_PASS -> {
                    // Only add if we came from S2
                    if (stateSequence.lastOrNull() == SquatState.S2_TRANSITION.name) {
                        if (stateSequence.size < 3) {
                            stateSequence.add(newState.name)
                        }
                    }
                }
                SquatState.S1_NORMAL -> {
                    // REP COUNTED HERE - returned to standing
                    updateCounters()
                }
                else -> { /* UP/DOWN legacy states - ignore */ }
            }
        }
        
        // Determine feedback (only for S2 and S3 as per article)
        var feedbackType = FeedbackType.NONE
        var feedbackMessage = ""
        var isSevere = false
        
        when (currentState) {
            SquatState.S1_NORMAL -> {
                feedbackType = FeedbackType.READY
                feedbackMessage = "Ready - squat down!"
            }
            SquatState.S2_TRANSITION, SquatState.S3_PASS -> {
                val (ft, msg, severe) = determineFeedback(hipVertical, kneeVertical, ankleVertical, isGoingDown)
                feedbackType = ft
                feedbackMessage = msg
                isSevere = severe
                
                if (severe) {
                    hasSevereFeedbackThisRep = true
                }
                
                recordFeedback(feedbackType)
            }
            else -> {
                feedbackType = FeedbackType.READY
                feedbackMessage = "Position yourself"
            }
        }
        
        // Override with frontal warning if applicable
        if (isFrontal) {
            feedbackType = FeedbackType.FRONTAL_WARNING
            feedbackMessage = "Turn to side view"
        }
        
        // Calculate depth percentage
        val depthPercentage = ((180f - kneeAngle) / 90f * 100f).coerceIn(0f, 100f)
        
        return AnalysisResult(
            repCount = correctCount + incorrectCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            currentState = currentState,
            stateSequence = stateSequence.toList(),
            kneeAngle = kneeAngle,
            hipAngle = AngleUtils.calculateHipAngle(landmarks, side == "Left"),
            hipVerticalAngle = hipVertical,
            kneeVerticalAngle = kneeVertical,
            ankleVerticalAngle = ankleVertical,
            feedbackType = feedbackType,
            feedback = feedbackMessage,
            isSevereFeedback = isSevere,
            isFullBodyVisible = true, // No strict visibility constraints
            isReady = true,
            detectedSide = side,
            offsetAngle = offsetAngle,
            isFrontalView = isFrontal,
            depthPercentage = depthPercentage,
            inactivitySeconds = inactivitySeconds,
            debugInfo = "State: ${currentState.name}, Seq: $stateSequence"
        )
    }
    
    /**
     * Determine state based on knee-vertical angle.
     * Uses thresholds from LearnOpenCV article.
     */
    private fun determineState(kneeVerticalAngle: Float): SquatState {
        return when {
            kneeVerticalAngle <= thresholds.stateS1Max -> SquatState.S1_NORMAL
            kneeVerticalAngle in thresholds.stateS2Min..thresholds.stateS2Max -> SquatState.S2_TRANSITION
            kneeVerticalAngle >= thresholds.stateS3Min && kneeVerticalAngle <= thresholds.stateS3Max -> SquatState.S3_PASS
            kneeVerticalAngle > thresholds.stateS3Max -> SquatState.S3_PASS // Cap at S3 for deep squats
            else -> currentState // In gap between thresholds, maintain current state
        }
    }
    
    /**
     * Determine feedback based on angles.
     * Feedback only computed for S2 and S3 states.
     */
    private fun determineFeedback(
        hipVertical: Float,
        kneeVertical: Float,
        ankleVertical: Float,
        isGoingDown: Boolean
    ): Triple<FeedbackType, String, Boolean> {
        
        // SEVERE ISSUES (contribute to incorrect count)
        
        // Feedback 4: Knee falling over toes
        if (ankleVertical > thresholds.kneeOverToes) {
            return Triple(FeedbackType.KNEE_OVER_TOES, "Knees over toes! Push hips back", true)
        }
        
        // Feedback 5: Too deep (only in S3)
        if (currentState == SquatState.S3_PASS && kneeVertical > thresholds.deepSquat) {
            return Triple(FeedbackType.DEEP_SQUAT, "Too deep! Stop at parallel", true)
        }
        
        // NON-SEVERE FEEDBACK
        
        // Feedback 1: Need to lean forward more
        if (hipVertical < thresholds.hipVerticalMin) {
            return Triple(FeedbackType.BEND_FORWARD, "Lean forward slightly", false)
        }
        
        // Feedback 2: Leaning back too much
        if (hipVertical > thresholds.hipVerticalMax) {
            return Triple(FeedbackType.BEND_BACKWARDS, "Straighten your back", false)
        }
        
        // Feedback 3: Need to go deeper (only when going down in S2)
        if (isGoingDown && currentState == SquatState.S2_TRANSITION) {
            if (kneeVertical in thresholds.lowerHipsMin..thresholds.lowerHipsMax) {
                return Triple(FeedbackType.LOWER_HIPS, "Go deeper!", false)
            }
        }
        
        // Good form feedback
        return when (currentState) {
            SquatState.S3_PASS -> Triple(FeedbackType.GOOD_FORM, "Good depth! Stand up", false)
            else -> Triple(FeedbackType.NONE, "", false)
        }
    }
    
    /**
     * Update rep counters when returning to S1.
     * 
     * From LearnOpenCV:
     * - state_sequence should be [S2, S3, S2] for correct rep
     * - If S3 is present, rep reached proper depth
     * - If only S2 transitions (never reached S3), incorrect
     */
    private fun updateCounters() {
        if (stateSequence.isEmpty()) {
            // No movement detected, don't count
            return
        }
        
        val hasReachedDepth = stateSequence.contains(SquatState.S3_PASS.name)
        val isCorrect = hasReachedDepth && !hasSevereFeedbackThisRep
        
        if (isCorrect) {
            // Correct rep: reached depth without severe form issues
            correctCount++
            if (minKneeAngleThisRep < 180f) {
                depthAngles.add(minKneeAngleThisRep)
            }
        } else if (stateSequence.isNotEmpty()) {
            // Incorrect rep: either didn't reach depth or had severe feedback
            incorrectCount++
        }
        
        // Record rep details for summary
        recordRep(isCorrect, minKneeAngleThisRep)
        
        // Reset for next rep
        stateSequence.clear()
        minKneeAngleThisRep = 180f
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
            currentState = currentState,
            feedbackType = feedbackType,
            feedback = feedback
        )
    }
    
    override fun reset(newMode: WorkoutMode?) {
        super.reset(newMode)
        if (newMode != null) {
            thresholds = if (newMode == WorkoutMode.BEGINNER) SquatThresholds.BEGINNER else SquatThresholds.PRO
        }
        currentState = SquatState.S1_NORMAL
        prevState = SquatState.S1_NORMAL
        stateSequence.clear()
        minKneeAngleThisRep = 180f
        lastKneeVertical = 0f
        hasSevereFeedbackThisRep = false
    }
    
    private data class SideLandmarks(
        val side: String,
        val shoulder: Pair<Float, Float>,
        val hip: Pair<Float, Float>,
        val knee: Pair<Float, Float>,
        val ankle: Pair<Float, Float>,
        val kneeAngle: Float
    )
}
