package com.example.myapplication.pose

import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Utility functions for calculating angles from pose landmarks.
 * Ported from Python angle_utils.py
 */
object AngleUtils {
    
    /**
     * Calculate angle at point B formed by points A-B-C.
     * Used for joint angles like hip-knee-ankle (knee angle) or shoulder-elbow-wrist (elbow angle).
     *
     * @param a First point (x, y)
     * @param b Vertex point (x, y) - angle is measured here
     * @param c Third point (x, y)
     * @return Angle in degrees (0-180)
     */
    fun angleAtPoint(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Float {
        val v1x = a.first - b.first
        val v1y = a.second - b.second
        val v2x = c.first - b.first
        val v2y = c.second - b.second
        
        val n1 = sqrt(v1x * v1x + v1y * v1y)
        val n2 = sqrt(v2x * v2x + v2y * v2y)
        
        if (n1 == 0f || n2 == 0f) return 0f
        
        val dotProduct = v1x * v2x + v1y * v2y
        var cosTheta = dotProduct / (n1 * n2)
        cosTheta = cosTheta.coerceIn(-1f, 1f)
        
        val theta = acos(cosTheta)
        return (theta * 180f / Math.PI).toFloat()
    }
    
    /**
     * Calculate angle between line (p1 to p2) and vertical axis.
     * Used for posture analysis (shoulder-hip, hip-knee, knee-ankle with vertical).
     *
     * @param p1 Upper point (x, y) - e.g., shoulder or hip
     * @param p2 Lower point (x, y) - e.g., hip or knee
     * @return Angle in degrees (0 = perfectly vertical, 90 = horizontal)
     */
    fun angleWithVertical(p1: Pair<Float, Float>, p2: Pair<Float, Float>): Float {
        // Vector from p1 to p2
        val lineX = p2.first - p1.first
        val lineY = p2.second - p1.second
        
        // Vertical vector pointing down (y increases down in image coords)
        val verticalX = 0f
        val verticalY = 1f
        
        val lineNorm = sqrt(lineX * lineX + lineY * lineY)
        if (lineNorm == 0f) return 0f
        
        val dotProduct = lineX * verticalX + lineY * verticalY
        var cosTheta = dotProduct / lineNorm
        cosTheta = cosTheta.coerceIn(-1f, 1f)
        
        val theta = acos(cosTheta)
        return (theta * 180f / Math.PI).toFloat()
    }
    
    /**
     * Calculate offset angle to detect if person is facing the camera (frontal view).
     * Uses nose position relative to shoulder midpoint.
     *
     * @param nose Nose coordinates (x, y)
     * @param leftShoulder Left shoulder (x, y)
     * @param rightShoulder Right shoulder (x, y)
     * @return Offset angle in degrees. Low values = side view, High values = frontal view
     */
    fun offsetAngle(
        nose: Pair<Float, Float>,
        leftShoulder: Pair<Float, Float>,
        rightShoulder: Pair<Float, Float>
    ): Float {
        // Use angle at nose formed by left shoulder - nose - right shoulder
        return angleAtPoint(leftShoulder, nose, rightShoulder)
    }
    
    /**
     * Get coordinates from a landmark as a Pair.
     */
    fun getLandmarkCoords(landmarks: List<PoseLandmark>, index: Int): Pair<Float, Float> {
        if (index >= landmarks.size) return Pair(0f, 0f)
        val lm = landmarks[index]
        return Pair(lm.x, lm.y)
    }
    
    /**
     * Calculate knee angle (hip-knee-ankle)
     */
    fun calculateKneeAngle(landmarks: List<PoseLandmark>, isLeft: Boolean): Float {
        val hipIdx = if (isLeft) LandmarkIndex.LEFT_HIP else LandmarkIndex.RIGHT_HIP
        val kneeIdx = if (isLeft) LandmarkIndex.LEFT_KNEE else LandmarkIndex.RIGHT_KNEE
        val ankleIdx = if (isLeft) LandmarkIndex.LEFT_ANKLE else LandmarkIndex.RIGHT_ANKLE
        
        if (hipIdx >= landmarks.size || kneeIdx >= landmarks.size || ankleIdx >= landmarks.size) {
            return 180f
        }
        
        val hip = getLandmarkCoords(landmarks, hipIdx)
        val knee = getLandmarkCoords(landmarks, kneeIdx)
        val ankle = getLandmarkCoords(landmarks, ankleIdx)
        
        return angleAtPoint(hip, knee, ankle)
    }
    
    /**
     * Calculate elbow angle (shoulder-elbow-wrist)
     */
    fun calculateElbowAngle(landmarks: List<PoseLandmark>, isLeft: Boolean): Float {
        val shoulderIdx = if (isLeft) LandmarkIndex.LEFT_SHOULDER else LandmarkIndex.RIGHT_SHOULDER
        val elbowIdx = if (isLeft) LandmarkIndex.LEFT_ELBOW else LandmarkIndex.RIGHT_ELBOW
        val wristIdx = if (isLeft) LandmarkIndex.LEFT_WRIST else LandmarkIndex.RIGHT_WRIST
        
        if (shoulderIdx >= landmarks.size || elbowIdx >= landmarks.size || wristIdx >= landmarks.size) {
            return 180f
        }
        
        val shoulder = getLandmarkCoords(landmarks, shoulderIdx)
        val elbow = getLandmarkCoords(landmarks, elbowIdx)
        val wrist = getLandmarkCoords(landmarks, wristIdx)
        
        return angleAtPoint(shoulder, elbow, wrist)
    }
    
    /**
     * Calculate hip angle (shoulder-hip-knee)
     */
    fun calculateHipAngle(landmarks: List<PoseLandmark>, isLeft: Boolean): Float {
        val shoulderIdx = if (isLeft) LandmarkIndex.LEFT_SHOULDER else LandmarkIndex.RIGHT_SHOULDER
        val hipIdx = if (isLeft) LandmarkIndex.LEFT_HIP else LandmarkIndex.RIGHT_HIP
        val kneeIdx = if (isLeft) LandmarkIndex.LEFT_KNEE else LandmarkIndex.RIGHT_KNEE
        
        if (shoulderIdx >= landmarks.size || hipIdx >= landmarks.size || kneeIdx >= landmarks.size) {
            return 180f
        }
        
        val shoulder = getLandmarkCoords(landmarks, shoulderIdx)
        val hip = getLandmarkCoords(landmarks, hipIdx)
        val knee = getLandmarkCoords(landmarks, kneeIdx)
        
        return angleAtPoint(shoulder, hip, knee)
    }
}
