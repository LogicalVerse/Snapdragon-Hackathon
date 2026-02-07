package com.example.myapplication.pose.trainers

import com.example.myapplication.pose.*

/**
 * Push-up exercise trainer.
 * Tracks elbow angle and body alignment.
 */
class PushUpTrainer(mode: WorkoutMode = WorkoutMode.BEGINNER) : ExerciseTrainer(
    exerciseId = "pushup",
    exerciseName = "Push-ups",
    mode = mode
) {
    private var currentPhase = PushUpState.UP
    private var lastElbowAngle = 180f
    
    // Thresholds
    private val elbowDownThreshold = if (mode == WorkoutMode.BEGINNER) 100f else 90f
    private val elbowUpThreshold = if (mode == WorkoutMode.BEGINNER) 150f else 160f
    private val bodyAlignmentThreshold = 15f // Max angle deviation from straight line
    
    private val requiredLandmarks = listOf(
        LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.RIGHT_SHOULDER,
        LandmarkIndex.LEFT_ELBOW, LandmarkIndex.RIGHT_ELBOW,
        LandmarkIndex.LEFT_WRIST, LandmarkIndex.RIGHT_WRIST,
        LandmarkIndex.LEFT_HIP, LandmarkIndex.RIGHT_HIP,
        LandmarkIndex.LEFT_ANKLE, LandmarkIndex.RIGHT_ANKLE
    )
    
    override fun analyze(landmarks: List<PoseLandmark>): AnalysisResult {
        if (landmarks.size < 33) {
            return AnalysisResult(
                feedbackType = FeedbackType.POSITION_BODY,
                feedback = "Incomplete pose data"
            )
        }
        
        val (isVisible, debugMsg) = checkBodyVisibility(landmarks, requiredLandmarks)
        
        if (isVisible) {
            fullBodyVisibleCount++
        } else {
            fullBodyVisibleCount = 0
        }
        
        isReady = fullBodyVisibleCount >= framesRequired
        
        // Calculate elbow angles
        val leftElbow = AngleUtils.calculateElbowAngle(landmarks, true)
        val rightElbow = AngleUtils.calculateElbowAngle(landmarks, false)
        val avgElbowAngle = (leftElbow + rightElbow) / 2f
        
        // Body alignment: shoulder-hip-ankle should be relatively straight
        val lShoulder = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_SHOULDER)
        val lHip = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_HIP)
        val lAnkle = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_ANKLE)
        val bodyAngle = AngleUtils.angleAtPoint(lShoulder, lHip, lAnkle)
        val bodyDeviationFromStraight = kotlin.math.abs(180f - bodyAngle)
        
        var feedbackType = FeedbackType.NONE
        var feedbackMessage = ""
        var isSevere = false
        
        if (isReady) {
            // State machine
            val newPhase = when {
                avgElbowAngle <= elbowDownThreshold -> PushUpState.DOWN
                avgElbowAngle >= elbowUpThreshold -> PushUpState.UP
                else -> PushUpState.TRANSITION
            }
            
            // Count rep when going from DOWN back to UP
            if (currentPhase == PushUpState.DOWN && newPhase == PushUpState.UP) {
                val isCorrect = bodyDeviationFromStraight < 20f
                if (isCorrect) {
                    correctCount++
                    if (avgElbowAngle < 180f) {
                        depthAngles.add(avgElbowAngle)
                    }
                } else {
                    incorrectCount++
                }
                // Record per-rep details
                recordRep(isCorrect, avgElbowAngle)
            }
            
            currentPhase = newPhase
            
            // Feedback
            if (bodyDeviationFromStraight > 25f) {
                if (lHip.second < lShoulder.second && lHip.second < lAnkle.second) {
                    feedbackType = FeedbackType.HIPS_TOO_HIGH
                    feedbackMessage = "Lower your hips - keep body straight"
                } else {
                    feedbackType = FeedbackType.HIPS_TOO_LOW
                    feedbackMessage = "Raise your hips - don't sag"
                }
            } else if (newPhase == PushUpState.UP && avgElbowAngle < elbowUpThreshold) {
                feedbackType = FeedbackType.ARMS_NOT_LOCKED
                feedbackMessage = "Fully extend your arms at the top"
            } else if (newPhase == PushUpState.DOWN) {
                feedbackType = FeedbackType.GOOD_FORM
                feedbackMessage = "Good depth! Push up!"
            } else if (newPhase == PushUpState.UP) {
                feedbackType = FeedbackType.READY
                feedbackMessage = "Ready - go down!"
            }
            
            recordFeedback(feedbackType)
            lastElbowAngle = avgElbowAngle
        } else {
            feedbackType = FeedbackType.POSITION_BODY
            feedbackMessage = "Position full body in frame"
        }
        
        val depth = ((180f - avgElbowAngle) / 90f * 100f).coerceIn(0f, 100f)
        
        return AnalysisResult(
            repCount = correctCount + incorrectCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            elbowAngle = avgElbowAngle,
            hipAngle = bodyAngle,
            feedbackType = feedbackType,
            feedback = feedbackMessage,
            isSevereFeedback = isSevere,
            isFullBodyVisible = isVisible,
            isReady = isReady,
            depthPercentage = depth,
            debugInfo = debugMsg
        )
    }
    
    override fun reset(newMode: WorkoutMode?) {
        super.reset(newMode)
        currentPhase = PushUpState.UP
        lastElbowAngle = 180f
    }
}
