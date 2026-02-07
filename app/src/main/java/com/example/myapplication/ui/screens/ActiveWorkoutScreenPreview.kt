package com.example.myapplication.ui.screens

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.data.FeedbackSeverity
import com.example.myapplication.data.FormQualityScore
import com.example.myapplication.data.SampleFeedbackMessages
import com.example.myapplication.data.WorkoutState
import com.example.myapplication.ui.theme.FormlyTheme
import com.example.myapplication.ui.theme.DeepDark
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=1440px,height=3088px,dpi=512",
    name = "Galaxy S25 Ultra - Active Workout"
)
@Composable
fun ActiveWorkoutScreenPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            ActiveWorkoutScreen(
                initialWorkoutState = WorkoutState(
                    exerciseName = "Squats",
                    currentReps = 7,
                    targetReps = 15,
                    elapsedTimeSeconds = 154,
                    isRecording = true,
                    isConnected = true,
                    formQuality = FormQualityScore(
                        overallScore = 0.85f,
                        depth = 0.9f,
                        speed = 0.8f,
                        alignment = 0.85f,
                        balance = 0.75f
                    ),
                    currentFeedback = SampleFeedbackMessages.perfectForm
                )
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=1440px,height=3088px,dpi=512",
    name = "Galaxy S25 Ultra - Poor Form"
)
@Composable
fun ActiveWorkoutScreenPoorFormPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            ActiveWorkoutScreen(
                initialWorkoutState = WorkoutState(
                    exerciseName = "Squats",
                    currentReps = 3,
                    targetReps = 15,
                    elapsedTimeSeconds = 45,
                    isRecording = true,
                    isConnected = true,
                    formQuality = FormQualityScore(
                        overallScore = 0.45f,
                        depth = 0.4f,
                        speed = 0.3f,
                        alignment = 0.5f,
                        balance = 0.55f
                    ),
                    currentFeedback = SampleFeedbackMessages.keepBackStraight
                )
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=1440px,height=3088px,dpi=512",
    name = "Galaxy S25 Ultra - Paused"
)
@Composable
fun ActiveWorkoutScreenPausedPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            ActiveWorkoutScreen(
                initialWorkoutState = WorkoutState(
                    exerciseName = "Push-ups",
                    currentReps = 10,
                    targetReps = 15,
                    elapsedTimeSeconds = 240,
                    isRecording = false,
                    isPaused = true,
                    isConnected = true,
                    formQuality = FormQualityScore(
                        overallScore = 0.72f,
                        depth = 0.75f,
                        speed = 0.7f,
                        alignment = 0.7f,
                        balance = 0.68f
                    ),
                    currentFeedback = SampleFeedbackMessages.slowDown
                )
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1440px,height=250px,dpi=512"
)
@Composable
fun CompactCameraPreviewPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            CompactCameraPreview(
                exerciseName = "Squats",
                elapsedTime = "02:34",
                isRecording = true,
                isConnected = true
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1440px,height=500px,dpi=512"
)
@Composable
fun RepCounterPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            RepCounter(
                currentReps = 7,
                targetReps = 15,
                progress = 0.47f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1440px,height=600px,dpi=512"
)
@Composable
fun FormQualityIndicatorPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            FormQualityIndicator(
                formQuality = FormQualityScore(
                    overallScore = 0.85f,
                    depth = 0.9f,
                    speed = 0.8f,
                    alignment = 0.85f,
                    balance = 0.75f
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1440px,height=350px,dpi=512"
)
@Composable
fun FeedbackCardExcellentPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            FeedbackCard(
                feedback = SampleFeedbackMessages.perfectForm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1440px,height=350px,dpi=512"
)
@Composable
fun FeedbackCardWarningPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            FeedbackCard(
                feedback = SampleFeedbackMessages.goDeeper,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1440px,height=300px,dpi=512"
)
@Composable
fun BottomControlPanelPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            BottomControlPanel(
                isPaused = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }
    }
}


