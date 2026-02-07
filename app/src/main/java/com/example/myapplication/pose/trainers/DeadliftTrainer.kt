package com.example.myapplication.pose.trainers

import com.example.myapplication.pose.*

/**
 * Deadlift exercise trainer.
 * Tracks hip hinge, back straightness, and bar path.
 */
class DeadliftTrainer(mode: WorkoutMode = WorkoutMode.BEGINNER) : ExerciseTrainer(
    exerciseId = "deadlift",
    exerciseName = "Deadlifts",
    mode = mode
) {
    private var currentPhase = ExercisePhase.START
    private var atBottom = false
    
    // Thresholds
    private val hipAngleDown = if (mode == WorkoutMode.BEGINNER) 100f else 90f
    private val hipAngleUp = if (mode == WorkoutMode.BEGINNER) 160f else 170f
    private val backAngleThreshold = if (mode == WorkoutMode.BEGINNER) 30f else 25f
    
    private val requiredLandmarks = listOf(
        LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.RIGHT_SHOULDER,
        LandmarkIndex.LEFT_HIP, LandmarkIndex.RIGHT_HIP,
        LandmarkIndex.LEFT_KNEE, LandmarkIndex.RIGHT_KNEE,
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
        
        if (isVisible) fullBodyVisibleCount++ else fullBodyVisibleCount = 0
        isReady = fullBodyVisibleCount >= framesRequired
        
        // Calculate hip angle (shoulder-hip-knee)
        val leftHipAngle = AngleUtils.calculateHipAngle(landmarks, true)
        val rightHipAngle = AngleUtils.calculateHipAngle(landmarks, false)
        val avgHipAngle = (leftHipAngle + rightHipAngle) / 2f
        
        // Check back straightness - shoulder-hip to vertical
        val lShoulder = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_SHOULDER)
        val lHip = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_HIP)
        val backAngle = AngleUtils.angleWithVertical(lHip, lShoulder)
        
        var feedbackType = FeedbackType.NONE
        var feedbackMessage = ""
        var isSevere = false
        
        if (isReady) {
            // State machine
            val wasAtBottom = atBottom
            
            if (avgHipAngle <= hipAngleDown) {
                atBottom = true
                currentPhase = ExercisePhase.HOLD
            } else if (avgHipAngle >= hipAngleUp) {
                if (wasAtBottom) {
                    // Completed a rep
                    val isCorrect = backAngle < backAngleThreshold + 10f
                    if (isCorrect) {
                        correctCount++
                        depthAngles.add(avgHipAngle)
                    } else {
                        incorrectCount++
                    }
                    // Record per-rep details
                    recordRep(isCorrect, avgHipAngle)
                }
                atBottom = false
                currentPhase = ExercisePhase.START
            } else {
                currentPhase = if (atBottom) ExercisePhase.CONCENTRIC else ExercisePhase.ECCENTRIC
            }
            
            // Feedback
            if (backAngle > backAngleThreshold) {
                feedbackType = FeedbackType.ROUND_BACK
                feedbackMessage = "Keep your back straight! Don't round."
                isSevere = true
            } else if (currentPhase == ExercisePhase.HOLD) {
                feedbackType = FeedbackType.GOOD_FORM
                feedbackMessage = "Good position! Drive through your legs."
            } else if (currentPhase == ExercisePhase.START) {
                feedbackType = FeedbackType.READY
                feedbackMessage = "Ready - hinge at hips!"
            } else {
                feedbackType = FeedbackType.NONE
                feedbackMessage = ""
            }
            
            recordFeedback(feedbackType)
        } else {
            feedbackType = FeedbackType.POSITION_BODY
            feedbackMessage = "Position full body in frame"
        }
        
        val depth = ((180f - avgHipAngle) / 90f * 100f).coerceIn(0f, 100f)
        
        return AnalysisResult(
            repCount = correctCount + incorrectCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            hipAngle = avgHipAngle,
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
        currentPhase = ExercisePhase.START
        atBottom = false
    }
}
