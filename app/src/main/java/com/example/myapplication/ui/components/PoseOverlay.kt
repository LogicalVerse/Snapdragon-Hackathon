package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.myapplication.pose.LandmarkIndex
import com.example.myapplication.pose.PoseLandmark
import com.example.myapplication.pose.SkeletonConnections
import com.example.myapplication.ui.theme.ElectricGreen

/**
 * Composable that draws pose skeleton overlay on camera preview
 */
@Composable
fun PoseOverlay(
    landmarks: List<PoseLandmark>,
    modifier: Modifier = Modifier,
    landmarkColor: Color = ElectricGreen,
    connectionColor: Color = ElectricGreen.copy(alpha = 0.8f),
    landmarkRadius: Float = 8f,
    connectionWidth: Float = 4f,
    minVisibility: Float = 0.5f,
    mirrorHorizontally: Boolean = true  // For front camera
) {
    if (landmarks.isEmpty()) return
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Helper to convert normalized coords to canvas coords
        fun toCanvasOffset(landmark: PoseLandmark): Offset {
            val x = if (mirrorHorizontally) {
                (1f - landmark.x) * width
            } else {
                landmark.x * width
            }
            val y = landmark.y * height
            return Offset(x, y)
        }
        
        // Draw connections first (so landmarks are on top)
        for ((startIdx, endIdx) in SkeletonConnections.connections) {
            if (startIdx < landmarks.size && endIdx < landmarks.size) {
                val start = landmarks[startIdx]
                val end = landmarks[endIdx]
                
                // Only draw if both landmarks are visible enough
                if (start.visibility >= minVisibility && end.visibility >= minVisibility) {
                    val startOffset = toCanvasOffset(start)
                    val endOffset = toCanvasOffset(end)
                    
                    drawLine(
                        color = connectionColor,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = connectionWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        // Draw landmarks
        for (landmark in landmarks) {
            if (landmark.visibility >= minVisibility) {
                val offset = toCanvasOffset(landmark)
                
                // Outer circle (border effect)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = landmarkRadius + 2f,
                    center = offset
                )
                
                // Inner circle
                drawCircle(
                    color = landmarkColor,
                    radius = landmarkRadius,
                    center = offset
                )
            }
        }
    }
}

/**
 * Simplified overlay showing only key body landmarks
 * Useful for cleaner visualization during workouts
 */
@Composable
fun SimplifiedPoseOverlay(
    landmarks: List<PoseLandmark>,
    modifier: Modifier = Modifier,
    color: Color = ElectricGreen,
    mirrorHorizontally: Boolean = true
) {
    // Key landmark indices for simplified view (full body)
    val keyLandmarks = listOf(
        LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.RIGHT_SHOULDER,
        LandmarkIndex.LEFT_ELBOW, LandmarkIndex.RIGHT_ELBOW,
        LandmarkIndex.LEFT_WRIST, LandmarkIndex.RIGHT_WRIST,
        LandmarkIndex.LEFT_HIP, LandmarkIndex.RIGHT_HIP,
        LandmarkIndex.LEFT_KNEE, LandmarkIndex.RIGHT_KNEE,
        LandmarkIndex.LEFT_ANKLE, LandmarkIndex.RIGHT_ANKLE
    )
    
    // Key connections for simplified view
    val keyConnections = listOf(
        Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.RIGHT_SHOULDER),  // Shoulder to shoulder
        Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_ELBOW),
        Pair(LandmarkIndex.LEFT_ELBOW, LandmarkIndex.LEFT_WRIST),  // Left arm
        Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_ELBOW),
        Pair(LandmarkIndex.RIGHT_ELBOW, LandmarkIndex.RIGHT_WRIST),  // Right arm
        Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_HIP),
        Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_HIP),  // Torso
        Pair(LandmarkIndex.LEFT_HIP, LandmarkIndex.RIGHT_HIP),  // Hip to hip
        Pair(LandmarkIndex.LEFT_HIP, LandmarkIndex.LEFT_KNEE),
        Pair(LandmarkIndex.LEFT_KNEE, LandmarkIndex.LEFT_ANKLE),  // Left leg
        Pair(LandmarkIndex.RIGHT_HIP, LandmarkIndex.RIGHT_KNEE),
        Pair(LandmarkIndex.RIGHT_KNEE, LandmarkIndex.RIGHT_ANKLE)   // Right leg
    )
    
    if (landmarks.isEmpty()) return
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        fun toCanvasOffset(landmark: PoseLandmark): Offset {
            val x = if (mirrorHorizontally) {
                (1f - landmark.x) * width
            } else {
                landmark.x * width
            }
            return Offset(x, landmark.y * height)
        }
        
        // Draw connections
        for ((startIdx, endIdx) in keyConnections) {
            if (startIdx < landmarks.size && endIdx < landmarks.size) {
                val start = landmarks[startIdx]
                val end = landmarks[endIdx]
                
                if (start.visibility >= 0.5f && end.visibility >= 0.5f) {
                    drawLine(
                        color = color.copy(alpha = 0.8f),
                        start = toCanvasOffset(start),
                        end = toCanvasOffset(end),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        // Draw key landmarks
        for (idx in keyLandmarks) {
            if (idx < landmarks.size && landmarks[idx].visibility >= 0.5f) {
                val offset = toCanvasOffset(landmarks[idx])
                
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = offset
                )
            }
        }
    }
}

/**
 * Overlay that replicates the EXACT visual style of the Python/OpenCV reference
 */
@Composable
fun PythonLikePoseOverlay(
    landmarks: List<PoseLandmark>,
    analysisResult: com.example.myapplication.pose.AnalysisResult?,
    frameWidth: Int,
    frameHeight: Int,
    frameCount: Long = 0,
    modifier: Modifier = Modifier
) {
    if (landmarks.isEmpty()) return
    
    // Python Colors (converted directly from BGR values in pose_test.py)
    val colorLine = Color(0xFFFFC8C8)   // (200, 200, 255) BGR -> Light Peach/Pink
    val colorJoint = Color(0xFF00FFFF)  // (0, 255, 255) BGR -> Yellow (R=255, G=255, B=0)
    
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Helper to convert normalized coords to canvas coords
        fun toCanvasOffset(landmark: PoseLandmark): Offset {
            // Mirror logic: 1 - x
            val x = (1f - landmark.x) * width
            val y = landmark.y * height
            return Offset(x, y)
        }

        // 1. Draw Skeleton (Matches Python draw connections)
        val connections = listOf(
            Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_ELBOW),
            Pair(LandmarkIndex.LEFT_ELBOW, LandmarkIndex.LEFT_WRIST),
            Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_HIP),
            Pair(LandmarkIndex.LEFT_HIP, LandmarkIndex.LEFT_KNEE),
            Pair(LandmarkIndex.LEFT_KNEE, LandmarkIndex.LEFT_ANKLE),
            Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_ELBOW),
            Pair(LandmarkIndex.RIGHT_ELBOW, LandmarkIndex.RIGHT_WRIST),
            Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_HIP),
            Pair(LandmarkIndex.RIGHT_HIP, LandmarkIndex.RIGHT_KNEE),
            Pair(LandmarkIndex.RIGHT_KNEE, LandmarkIndex.RIGHT_ANKLE),
            Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.RIGHT_SHOULDER),
            Pair(LandmarkIndex.LEFT_HIP, LandmarkIndex.RIGHT_HIP)
        )

        connections.forEach { (startIdx, endIdx) ->
            if (startIdx < landmarks.size && endIdx < landmarks.size) {
                val start = landmarks[startIdx]
                val end = landmarks[endIdx]
                
                if (start.visibility > 0.3f && end.visibility > 0.3f) {
                     drawLine(
                        color = colorLine,
                        start = toCanvasOffset(start),
                        end = toCanvasOffset(end),
                        strokeWidth = 3f * density,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        // Draw Joints
        val joints = listOf(
            LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_ELBOW, LandmarkIndex.LEFT_WRIST,
            LandmarkIndex.LEFT_HIP, LandmarkIndex.LEFT_KNEE, LandmarkIndex.LEFT_ANKLE,
            LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_ELBOW, LandmarkIndex.RIGHT_WRIST,
            LandmarkIndex.RIGHT_HIP, LandmarkIndex.RIGHT_KNEE, LandmarkIndex.RIGHT_ANKLE
        )
        
        joints.forEach { idx ->
            if (idx < landmarks.size && landmarks[idx].visibility > 0.3f) {
                drawCircle(
                    color = colorJoint,
                    radius = 5f * density,
                    center = toCanvasOffset(landmarks[idx])
                )
            }
        }

        // 2. Draw Text Overlay using Native Canvas via drawIntoCanvas
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            
            // Text Paint Setup
            val paint = android.graphics.Paint().apply {
                textSize = 24f * density // Scaled text size
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
                isAntiAlias = true
            }

            // Text positions matching Python (approximate relative to screen density)
            val lineSpacing = 35f * density
            var currentY = 50f * density 

            if (analysisResult != null) {
                if (analysisResult.isFullBodyVisible || analysisResult.kneeAngle < 179f) {
                    // Side: {side}
                    paint.color = android.graphics.Color.WHITE
                    nativeCanvas.drawText("Side: ${analysisResult.detectedSide}", 20f * density, currentY, paint)
                    currentY += lineSpacing

                    // Knee angle: {angle}
                    paint.color = android.graphics.Color.rgb(180, 255, 180) // Light Green
                    nativeCanvas.drawText("Knee angle: ${analysisResult.kneeAngle.toInt()}", 20f * density, currentY, paint)
                    currentY += lineSpacing

                    // Squats: {count}
                    paint.color = android.graphics.Color.rgb(255, 200, 50) // Golden/Orange
                    paint.textSize = 28f * density // Slightly larger
                    nativeCanvas.drawText("Squats: ${analysisResult.repCount}", 20f * density, currentY, paint)
                } else {
                     // Not ready / Not full body
                     paint.color = android.graphics.Color.YELLOW // (0, 255, 255) BGR
                     paint.textSize = 18f * density
                     nativeCanvas.drawText("Position full body to start", 20f * density, currentY + lineSpacing, paint)
                     
                     // Show Debug reasons
                     if (analysisResult.debugInfo.isNotEmpty()) {
                         paint.color = android.graphics.Color.WHITE
                         paint.textSize = 14f * density
                         nativeCanvas.drawText(analysisResult.debugInfo, 20f * density, currentY + lineSpacing * 2, paint)
                     }
                }
            } else {
                 paint.color = android.graphics.Color.RED
                 nativeCanvas.drawText("No Pose Detected", 20f * density, currentY, paint)
            }
        }
    }
}
