package com.example.myapplication.pose

/**
 * Represents a single pose landmark with position and visibility
 */
data class PoseLandmark(
    val x: Float,      // Normalized x coordinate (0-1)
    val y: Float,      // Normalized y coordinate (0-1)
    val z: Float,      // Depth relative to hips
    val visibility: Float  // Likelihood landmark is visible (0-1)
)

/**
 * MediaPipe Pose Landmarker indices (33 landmarks)
 */
object LandmarkIndex {
    const val NOSE = 0
    const val LEFT_EYE_INNER = 1
    const val LEFT_EYE = 2
    const val LEFT_EYE_OUTER = 3
    const val RIGHT_EYE_INNER = 4
    const val RIGHT_EYE = 5
    const val RIGHT_EYE_OUTER = 6
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val MOUTH_LEFT = 9
    const val MOUTH_RIGHT = 10
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_PINKY = 17
    const val RIGHT_PINKY = 18
    const val LEFT_INDEX = 19
    const val RIGHT_INDEX = 20
    const val LEFT_THUMB = 21
    const val RIGHT_THUMB = 22
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32
    
    const val TOTAL_LANDMARKS = 33
}

/**
 * Skeleton connections for drawing pose (MediaPipe standard)
 */
object SkeletonConnections {
    val connections = listOf(
        // Face
        Pair(LandmarkIndex.NOSE, LandmarkIndex.LEFT_EYE_INNER),
        Pair(LandmarkIndex.LEFT_EYE_INNER, LandmarkIndex.LEFT_EYE),
        Pair(LandmarkIndex.LEFT_EYE, LandmarkIndex.LEFT_EYE_OUTER),
        Pair(LandmarkIndex.LEFT_EYE_OUTER, LandmarkIndex.LEFT_EAR),
        Pair(LandmarkIndex.NOSE, LandmarkIndex.RIGHT_EYE_INNER),
        Pair(LandmarkIndex.RIGHT_EYE_INNER, LandmarkIndex.RIGHT_EYE),
        Pair(LandmarkIndex.RIGHT_EYE, LandmarkIndex.RIGHT_EYE_OUTER),
        Pair(LandmarkIndex.RIGHT_EYE_OUTER, LandmarkIndex.RIGHT_EAR),
        Pair(LandmarkIndex.MOUTH_LEFT, LandmarkIndex.MOUTH_RIGHT),
        
        // Torso
        Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.RIGHT_SHOULDER),
        Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_HIP),
        Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_HIP),
        Pair(LandmarkIndex.LEFT_HIP, LandmarkIndex.RIGHT_HIP),
        
        // Left arm
        Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_ELBOW),
        Pair(LandmarkIndex.LEFT_ELBOW, LandmarkIndex.LEFT_WRIST),
        Pair(LandmarkIndex.LEFT_WRIST, LandmarkIndex.LEFT_THUMB),
        Pair(LandmarkIndex.LEFT_WRIST, LandmarkIndex.LEFT_PINKY),
        Pair(LandmarkIndex.LEFT_WRIST, LandmarkIndex.LEFT_INDEX),
        Pair(LandmarkIndex.LEFT_PINKY, LandmarkIndex.LEFT_INDEX),
        
        // Right arm
        Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_ELBOW),
        Pair(LandmarkIndex.RIGHT_ELBOW, LandmarkIndex.RIGHT_WRIST),
        Pair(LandmarkIndex.RIGHT_WRIST, LandmarkIndex.RIGHT_THUMB),
        Pair(LandmarkIndex.RIGHT_WRIST, LandmarkIndex.RIGHT_PINKY),
        Pair(LandmarkIndex.RIGHT_WRIST, LandmarkIndex.RIGHT_INDEX),
        Pair(LandmarkIndex.RIGHT_PINKY, LandmarkIndex.RIGHT_INDEX),
        
        // Left leg
        Pair(LandmarkIndex.LEFT_HIP, LandmarkIndex.LEFT_KNEE),
        Pair(LandmarkIndex.LEFT_KNEE, LandmarkIndex.LEFT_ANKLE),
        Pair(LandmarkIndex.LEFT_ANKLE, LandmarkIndex.LEFT_HEEL),
        Pair(LandmarkIndex.LEFT_ANKLE, LandmarkIndex.LEFT_FOOT_INDEX),
        Pair(LandmarkIndex.LEFT_HEEL, LandmarkIndex.LEFT_FOOT_INDEX),
        
        // Right leg
        Pair(LandmarkIndex.RIGHT_HIP, LandmarkIndex.RIGHT_KNEE),
        Pair(LandmarkIndex.RIGHT_KNEE, LandmarkIndex.RIGHT_ANKLE),
        Pair(LandmarkIndex.RIGHT_ANKLE, LandmarkIndex.RIGHT_HEEL),
        Pair(LandmarkIndex.RIGHT_ANKLE, LandmarkIndex.RIGHT_FOOT_INDEX),
        Pair(LandmarkIndex.RIGHT_HEEL, LandmarkIndex.RIGHT_FOOT_INDEX)
    )
}
