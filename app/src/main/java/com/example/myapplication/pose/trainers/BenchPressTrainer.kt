package com.example.myapplication.pose.trainers

import com.example.myapplication.pose.*

/**
 * Bench press exercise trainer.
 * Tracks elbow depth and lockout.
 */
class BenchPressTrainer(mode: WorkoutMode = WorkoutMode.BEGINNER) : ExerciseTrainer(
    exerciseId = "bench_press",
    exerciseName = "Bench Press",
    mode = mode
) {
    private var atBottom = false
    
    // Thresholds  
    private val elbowDownThreshold = if (mode == WorkoutMode.BEGINNER) 70f else 60f
    private val elbowUpThreshold = if (mode == WorkoutMode.BEGINNER) 150f else 160f
    private val elbowFlareThreshold = 85f // Angle at shoulder for elbow flare
    
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
        
        // Calculate elbow flare (angle at shoulder: elbow-shoulder-hip)
        val lShoulder = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_SHOULDER)
        val lElbow = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_ELBOW)
        val lHip = AngleUtils.getLandmarkCoords(landmarks, LandmarkIndex.LEFT_HIP)
        val elbowFlare = AngleUtils.angleAtPoint(lElbow, lShoulder, lHip)
        
        var feedbackType = FeedbackType.NONE
        var feedbackMessage = ""
        var isSevere = false
        
        if (isReady) {
            val wasAtBottom = atBottom
            
            if (avgElbowAngle <= elbowDownThreshold) {
                atBottom = true
            } else if (avgElbowAngle >= elbowUpThreshold) {
                if (wasAtBottom) {
                    val isCorrect = elbowFlare <= elbowFlareThreshold
                    if (isCorrect) {
                        correctCount++
                    } else {
                        incorrectCount++
                    }
                    depthAngles.add(avgElbowAngle)
                    // Record per-rep details
                    recordRep(isCorrect, avgElbowAngle)
                }
                atBottom = false
            }
            
            // Feedback
            if (elbowFlare > elbowFlareThreshold) {
                feedbackType = FeedbackType.ELBOWS_TOO_WIDE
                feedbackMessage = "Tuck your elbows - 45° angle"
            } else if (!atBottom && avgElbowAngle < elbowUpThreshold) {
                feedbackType = FeedbackType.INCOMPLETE_LOCKOUT
                feedbackMessage = "Lock out your arms fully"
            } else if (atBottom) {
                feedbackType = FeedbackType.GOOD_FORM
                feedbackMessage = "Good depth! Push up!"
            } else {
                feedbackType = FeedbackType.READY
                feedbackMessage = "Ready - lower the bar!"
            }
            
            recordFeedback(feedbackType)
        } else {
            feedbackType = FeedbackType.POSITION_BODY
            feedbackMessage = "Position upper body in frame"
        }
        
        val depth = ((180f - avgElbowAngle) / 120f * 100f).coerceIn(0f, 100f)
        
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
        atBottom = false
    }
}
