package com.example.myapplication.pose.trainers

import com.example.myapplication.pose.*

/**
 * Row exercise trainer.
 * Tracks pull depth and back posture.
 */
class RowTrainer(mode: WorkoutMode = WorkoutMode.BEGINNER) : ExerciseTrainer(
    exerciseId = "rows",
    exerciseName = "Rows",
    mode = mode
) {
    private var atPeak = false
    private var lastElbowAngle = 180f
    
    // Thresholds
    private val elbowPullThreshold = if (mode == WorkoutMode.BEGINNER) 50f else 40f
    private val elbowExtendThreshold = if (mode == WorkoutMode.BEGINNER) 150f else 160f
    private val momentumThreshold = 15f // Max angle change per frame
    
    private val requiredLandmarks = listOf(
        LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.RIGHT_SHOULDER,
        LandmarkIndex.LEFT_ELBOW, LandmarkIndex.RIGHT_ELBOW,
        LandmarkIndex.LEFT_WRIST, LandmarkIndex.RIGHT_WRIST,
        LandmarkIndex.LEFT_HIP, LandmarkIndex.RIGHT_HIP
    )
    
    override fun analyze(landmarks: List<PoseLandmark>): AnalysisResult {
        if (landmarks.size < 33) {
            return AnalysisResult(
                feedbackType = FeedbackType.POSITION_BODY,
                feedback = "Incomplete pose data"
            )
        }
        
        val (isVisible, debugMsg) = checkBodyVisibility(landmarks, requiredLandmarks)
        
        if (isVisible) fullBodyVisibleCount++ else fullBodyVisibleCount = 0
        isReady = fullBodyVisibleCount >= framesRequired
        
        // Calculate elbow angles
        val leftElbow = AngleUtils.calculateElbowAngle(landmarks, true)
        val rightElbow = AngleUtils.calculateElbowAngle(landmarks, false)
        val avgElbowAngle = (leftElbow + rightElbow) / 2f
        
        // Check for momentum (too fast movement)
        val angleChange = kotlin.math.abs(avgElbowAngle - lastElbowAngle)
        val hasExcessMomentum = angleChange > momentumThreshold
        
        var feedbackType = FeedbackType.NONE
        var feedbackMessage = ""
        var isSevere = false
        
        if (isReady) {
            val wasAtPeak = atPeak
            
            if (avgElbowAngle <= elbowPullThreshold) {
                atPeak = true
            } else if (avgElbowAngle >= elbowExtendThreshold) {
                if (wasAtPeak) {
                    val isCorrect = !hasExcessMomentum
                    if (isCorrect) {
                        correctCount++
                        depthAngles.add(avgElbowAngle)
                    } else {
                        incorrectCount++
                    }
                    // Record per-rep details
                    recordRep(isCorrect, avgElbowAngle)
                }
                atPeak = false
            }
            
            // Feedback
            if (hasExcessMomentum) {
                feedbackType = FeedbackType.MOMENTUM
                feedbackMessage = "Control the weight - slow it down"
            } else if (atPeak) {
                feedbackType = FeedbackType.GOOD_FORM
                feedbackMessage = "Good squeeze! Now extend slowly."
            } else {
                feedbackType = FeedbackType.READY
                feedbackMessage = "Ready - pull with your back!"
            }
            
            recordFeedback(feedbackType)
            lastElbowAngle = avgElbowAngle
        } else {
            feedbackType = FeedbackType.POSITION_BODY
            feedbackMessage = "Position upper body in frame"
        }
        
        val depth = ((180f - avgElbowAngle) / 130f * 100f).coerceIn(0f, 100f)
        
        return AnalysisResult(
            repCount = correctCount + incorrectCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            elbowAngle = avgElbowAngle,
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
        atPeak = false
        lastElbowAngle = 180f
    }
}
