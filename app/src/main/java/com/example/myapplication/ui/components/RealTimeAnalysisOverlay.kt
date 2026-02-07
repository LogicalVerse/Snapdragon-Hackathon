package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.pose.AnalysisResult
import com.example.myapplication.pose.FeedbackType
import com.example.myapplication.pose.SquatState
import com.example.myapplication.ui.theme.*

/**
 * Real-time analysis overlay showing rep count, angles, and feedback during workout
 */
@Composable
fun RealTimeAnalysisOverlay(
    analysisResult: AnalysisResult?,
    exerciseName: String = "Workout",
    modifier: Modifier = Modifier
) {
    if (analysisResult == null) return
    
    Box(modifier = modifier.fillMaxSize()) {
        // Top overlay - Rep counter and angles
        TopAnalysisBar(
            analysisResult = analysisResult,
            exerciseName = exerciseName,
            modifier = Modifier.align(Alignment.TopStart)
        )
        
        // Center feedback message (when there's notable feedback)
        AnimatedVisibility(
            visible = analysisResult.feedbackType != FeedbackType.NONE && 
                     analysisResult.feedbackType != FeedbackType.READY,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            FeedbackBanner(analysisResult)
        }
        
        // Bottom overlay - State indicator and depth
        BottomAnalysisBar(
            analysisResult = analysisResult,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TopAnalysisBar(
    analysisResult: AnalysisResult,
    exerciseName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.4f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Exercise name
        Text(
            text = exerciseName,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Rep counter
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${analysisResult.repCount}",
                color = ElectricGreen,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " reps",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Correct/Incorrect breakdown
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Correct",
                    tint = BrightGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " ${analysisResult.correctCount}",
                    color = BrightGreen,
                    fontSize = 14.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Incorrect",
                    tint = AlertRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " ${analysisResult.incorrectCount}",
                    color = AlertRed,
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Angle display
        if (analysisResult.isFullBodyVisible || analysisResult.kneeAngle < 175f) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AngleChip(label = "Knee", angle = analysisResult.kneeAngle)
                if (analysisResult.hipAngle < 175f) {
                    AngleChip(label = "Hip", angle = analysisResult.hipAngle)
                }
            }
            
            // Side indicator
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Side: ${analysisResult.detectedSide}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AngleChip(label: String, angle: Float) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SubtleGray.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        Text(
            text = "${angle.toInt()}°",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FeedbackBanner(analysisResult: AnalysisResult) {
    val backgroundColor = when {
        analysisResult.isSevereFeedback -> AlertRed
        analysisResult.feedbackType == FeedbackType.GOOD_FORM ||
        analysisResult.feedbackType == FeedbackType.REP_COMPLETE -> BrightGreen
        else -> Amber
    }
    
    val icon = when {
        analysisResult.isSevereFeedback -> Icons.Default.Warning
        analysisResult.feedbackType == FeedbackType.GOOD_FORM ||
        analysisResult.feedbackType == FeedbackType.REP_COMPLETE -> Icons.Default.Check
        else -> Icons.Default.Warning
    }
    
    Row(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = analysisResult.feedback,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BottomAnalysisBar(
    analysisResult: AnalysisResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.6f)
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // State indicator
        StateIndicator(state = analysisResult.currentState)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Depth progress bar
        DepthProgressBar(
            depthPercentage = analysisResult.depthPercentage,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(8.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Depth: ${analysisResult.depthPercentage.toInt()}%",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StateIndicator(state: SquatState) {
    val (text, color) = when (state) {
        SquatState.S1_NORMAL -> "Ready" to Color.White
        SquatState.S2_TRANSITION -> "Going..." to Amber
        SquatState.S3_PASS -> "At Depth!" to BrightGreen
        SquatState.UP -> "Up" to Color.White
        SquatState.DOWN -> "Down" to Amber
    }
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DepthProgressBar(
    depthPercentage: Float,
    modifier: Modifier = Modifier
) {
    val progress = (depthPercentage / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        progress >= 0.8f -> BrightGreen
        progress >= 0.5f -> Amber
        else -> Color.White.copy(alpha = 0.5f)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(RoundedCornerShape(4.dp))
                .background(progressColor)
        )
    }
}
