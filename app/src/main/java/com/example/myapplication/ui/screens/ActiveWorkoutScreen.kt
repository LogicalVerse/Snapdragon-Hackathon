package com.example.myapplication.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.camera.CameraManager
import com.example.myapplication.data.FeedbackMessage
import com.example.myapplication.data.FeedbackSeverity
import com.example.myapplication.data.FormQualityScore
import com.example.myapplication.data.WorkoutState
import com.example.myapplication.ui.theme.AlertRed
import com.example.myapplication.ui.theme.Amber
import com.example.myapplication.ui.theme.CoolBlue
import com.example.myapplication.ui.theme.DeepDark
import com.example.myapplication.ui.theme.ElectricGreen
import com.example.myapplication.ui.theme.ElevatedDark
import com.example.myapplication.ui.theme.MediumGray
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.Spacing
import com.example.myapplication.ui.theme.SubtleGray
import com.example.myapplication.pose.AnalysisResult
import com.example.myapplication.pose.PoseLandmark
import com.example.myapplication.pose.WorkoutSummary
import com.example.myapplication.ui.components.PythonLikePoseOverlay
import com.example.myapplication.ui.components.SimplifiedPoseOverlay

/**
 * Active Workout Screen - Real-time form feedback during exercise
 */
@Composable
fun ActiveWorkoutScreen(
    initialWorkoutState: WorkoutState = WorkoutState(),
    onStopPressed: () -> Unit = {},
    onFinishPressed: () -> Unit = {},
    onPausePressed: () -> Unit = {},
    onResumePressed: () -> Unit = {}
) {
    var workoutState by remember { mutableStateOf(initialWorkoutState) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showPausedOverlay by remember { mutableStateOf(false) }
    var workoutSummary by remember { mutableStateOf(WorkoutSummary()) }
    var currentLandmarks by remember { mutableStateOf<List<PoseLandmark>>(emptyList()) }
    var currentAnalysisResult by remember { mutableStateOf<AnalysisResult?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember { CameraManager(context) }

    // Create PreviewView for CameraX
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Start camera, recording, and pose estimation on mount
    LaunchedEffect(Unit) {
        cameraManager.startCamera(lifecycleOwner, previewView, useFrontCamera = true)
        // Start pose estimation for analysis
        cameraManager.startPoseEstimation()
        // Connect analysis callback
        cameraManager.onAnalysisResult = { result ->
            currentAnalysisResult = result
            workoutState = workoutState.copy(
                currentReps = result.repCount,
                currentFeedback = FeedbackMessage(
                    text = result.feedback,
                    severity = when {
                        result.feedback.contains("Perfect") || result.feedback.contains("complete") -> FeedbackSeverity.SUCCESS
                        result.feedback.contains("Go deeper") || result.feedback.contains("Stand") -> FeedbackSeverity.WARNING
                        result.feedback.contains("Position") -> FeedbackSeverity.INFO
                        else -> FeedbackSeverity.INFO
                    },
                    metric = "Depth",
                    metricValue = result.depthPercentage / 100f
                )
            )
        }
        
        // Connect pose detection callback for visualization
        cameraManager.onPoseDetected = { landmarks ->
            currentLandmarks = landmarks
        }
        
        // Auto-start recording
        cameraManager.startRecording()
        workoutState = workoutState.copy(isRecording = true)
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.stopRecording()
            cameraManager.shutdown()
        }
    }

    fun handleFinish() {
        cameraManager.stopRecording()
        onFinishPressed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Compact Camera Preview
            CompactCameraPreview(
                exerciseName = workoutState.exerciseName,
                elapsedTime = workoutState.formattedTime,
                isRecording = workoutState.isRecording,
                isConnected = workoutState.isConnected,
                previewView = previewView,
                landmarks = currentLandmarks,
                analysisResult = currentAnalysisResult,
                onStopPressed = { showExitConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Incresed height for better visibility
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            )

            // Main Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    vertical = Spacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl)
            ) {
                // Rep Counter
                item {
                    RepCounter(
                        currentReps = workoutState.currentReps,
                        targetReps = workoutState.targetReps,
                        progress = workoutState.repProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Form Quality Indicator
                item {
                    FormQualityIndicator(
                        formQuality = workoutState.formQuality,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Real-Time Feedback Card
                item {
                    FeedbackCard(
                        feedback = workoutState.currentFeedback,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
            }

            // Bottom Controls
            BottomControlPanel(
                isPaused = workoutState.isPaused,
                onPauseResumeClick = {
                    workoutState = if (workoutState.isPaused) {
                        onResumePressed()
                        workoutState.copy(isPaused = false)
                    } else {
                        // Capture workout summary when pausing
                        workoutSummary = cameraManager.getWorkoutSummary()
                        onPausePressed()
                        workoutState.copy(isPaused = true)
                    }
                    showPausedOverlay = workoutState.isPaused
                },
                onFinishClick = { showExitConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }

        // Paused Overlay with Workout Summary
        if (showPausedOverlay && workoutState.isPaused) {
            PausedOverlay(
                summary = workoutSummary,
                onResume = {
                    workoutState = workoutState.copy(isPaused = false)
                    showPausedOverlay = false
                    onResumePressed()
                },
                onEndWorkout = { showExitConfirmation = true },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Exit Confirmation Dialog
        if (showExitConfirmation) {
            ExitConfirmationDialog(
                onDismiss = { showExitConfirmation = false },
                onConfirm = { handleFinish() }
            )
        }
    }
}

/**
 * Compact camera preview at top of screen
 */
@Composable
fun CompactCameraPreview(
    exerciseName: String,
    elapsedTime: String,
    isRecording: Boolean,
    isConnected: Boolean,
    previewView: PreviewView? = null,
    landmarks: List<PoseLandmark> = emptyList(),
    analysisResult: AnalysisResult? = null,
    onStopPressed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Camera Preview Box
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SubtleGray)
                    .shadow(2.dp, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.TopEnd
            ) {
                if (previewView != null) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Python-Style Reference Overlay
                PythonLikePoseOverlay(
                    landmarks = landmarks,
                    analysisResult = analysisResult,
                    frameWidth = 640, // Nominal width from logic
                    frameHeight = 480,
                    modifier = Modifier.fillMaxSize()
                )

                // Recording Indicator
                if (isRecording) {
                    val pulsing by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                0.5f at 0
                                1f at 500
                                0.5f at 1000
                            }
                        )
                    )

                    Box(
                        modifier = Modifier
                            .padding(Spacing.sm)
                            .size(12.dp)
                            .background(
                                color = AlertRed.copy(alpha = pulsing),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Workout Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MediumGray,
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = elapsedTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediumGray
                    )

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (isConnected) ElectricGreen else AlertRed,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Stop Button
            IconButton(
                onClick = onStopPressed,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = AlertRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
                        tint = AlertRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Large rep counter with animation
 */
@Composable
fun RepCounter(
    currentReps: Int,
    targetReps: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedReps by animateIntAsState(
        targetValue = currentReps,
        animationSpec = spring(stiffness = 100f)
    )

    val repScale by animateFloatAsState(
        targetValue = if (currentReps > 0 && currentReps % 1 == 0) 1.1f else 1f,
        animationSpec = spring(stiffness = 400f)
    )

    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "REPS",
            style = MaterialTheme.typography.labelLarge,
            color = MediumGray,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Glow background
            if (progress > 0) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ElectricGreen.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                radius = with(LocalDensity.current) { 80.dp.toPx() }
                            )
                        )
                )
            }

            // Animated number
            Text(
                text = animatedReps.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontSize = 120.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ElectricGreen,
                modifier = Modifier.apply {
                    if (repScale != 1f) {
                        this.then(
                            Modifier
                                .padding(bottom = Spacing.lg)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = "of $targetReps",
            style = MaterialTheme.typography.bodyMedium,
            color = MediumGray
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // Progress indicator
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = ElectricGreen,
            trackColor = SubtleGray
        )
    }
}

/**
 * Form quality indicator with circular progress ring
 */
@Composable
fun FormQualityIndicator(
    formQuality: FormQualityScore,
    modifier: Modifier = Modifier
) {
    val progressColor by animateColorAsState(
        targetValue = formQuality.scoreColor,
        animationSpec = spring(stiffness = 100f)
    )

    val animatedProgress by animateFloatAsState(
        targetValue = formQuality.overallScore,
        animationSpec = spring(stiffness = 80f)
    )

    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FORM QUALITY",
            style = MaterialTheme.typography.labelLarge,
            color = MediumGray,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Circular progress indicator
        Box(
            modifier = Modifier
                .size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(8.dp, shape = CircleShape),
                color = progressColor,
                trackColor = SubtleGray,
                strokeWidth = 16.dp,
                strokeCap = StrokeCap.Round
            )

            // Inner text with score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = progressColor
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = formQuality.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = progressColor
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // Metrics breakdown
        MetricsBreakdown(formQuality)
    }
}

/**
 * Breakdown of individual form metrics
 */
@Composable
fun MetricsBreakdown(
    formQuality: FormQualityScore,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetricItem(label = "Depth", value = formQuality.depth)
        MetricItem(label = "Speed", value = formQuality.speed)
        MetricItem(label = "Align", value = formQuality.alignment)
        MetricItem(label = "Balance", value = formQuality.balance)
    }
}

/**
 * Individual metric display
 */
@Composable
fun MetricItem(
    label: String,
    value: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            progress = value,
            modifier = Modifier.size(40.dp),
            color = ElectricGreen,
            trackColor = SubtleGray,
            strokeWidth = 3.dp
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MediumGray
        )

        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = OffWhite
        )
    }
}

/**
 * Real-time AI feedback card
 */
@Composable
fun FeedbackCard(
    feedback: FeedbackMessage,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (feedback.severity) {
        FeedbackSeverity.SUCCESS -> ElectricGreen.copy(alpha = 0.1f)
        FeedbackSeverity.WARNING -> Amber.copy(alpha = 0.1f)
        FeedbackSeverity.CRITICAL -> AlertRed.copy(alpha = 0.1f)
        FeedbackSeverity.INFO -> CoolBlue.copy(alpha = 0.1f)
    }

    val iconColor = when (feedback.severity) {
        FeedbackSeverity.SUCCESS -> ElectricGreen
        FeedbackSeverity.WARNING -> Amber
        FeedbackSeverity.CRITICAL -> AlertRed
        FeedbackSeverity.INFO -> CoolBlue
    }

    val icon = when (feedback.severity) {
        FeedbackSeverity.SUCCESS -> Icons.Default.Check
        FeedbackSeverity.WARNING -> Icons.Default.Warning
        FeedbackSeverity.CRITICAL -> Icons.Default.Close
        FeedbackSeverity.INFO -> Icons.Default.Info
    }

    Card(
        modifier = modifier
            .animateContentSize()
            .shadow(4.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Severity Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )

            // Feedback Message
            Text(
                text = feedback.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OffWhite,
                textAlign = TextAlign.Center
            )

            // Metric indicator
            if (feedback.metric != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = feedback.metric,
                        style = MaterialTheme.typography.labelMedium,
                        color = MediumGray
                    )

                    LinearProgressIndicator(
                        progress = feedback.metricValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = iconColor,
                        trackColor = SubtleGray
                    )

                    Text(
                        text = "${(feedback.metricValue * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray
                    )
                }
            }
        }
    }
}

/**
 * Bottom control panel with pause and finish buttons
 */
@Composable
fun BottomControlPanel(
    isPaused: Boolean,
    onPauseResumeClick: () -> Unit = {},
    onFinishClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(8.dp),
        color = ElevatedDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause/Resume Button
            Button(
                onClick = onPauseResumeClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SubtleGray
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Close,
                        contentDescription = null,
                        tint = OffWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = Spacing.sm)
                    )

                    Text(
                        text = if (isPaused) "Resume" else "Pause",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
                }
            }

            // Finish Button
            Button(
                onClick = onFinishClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertRed
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = OffWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = Spacing.sm)
                    )

                    Text(
                        text = "Finish",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
                }
            }
        }
    }
}

/**
 * Paused workout overlay
 */
@Composable
fun PausedOverlay(
    summary: WorkoutSummary,
    onResume: () -> Unit = {},
    onEndWorkout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(12.dp, shape = RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = ElevatedDark
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                Text(
                    text = "WORKOUT PAUSED",
                    style = MaterialTheme.typography.labelLarge,
                    color = MediumGray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Reps
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = summary.totalReps.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = OffWhite
                        )
                        Text(
                            text = "Reps",
                            style = MaterialTheme.typography.labelMedium,
                            color = MediumGray
                        )
                    }
                    
                    // Best Depth
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summary.bestDepthPercentage.toInt()}%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricGreen
                        )
                        Text(
                            text = "Best Depth",
                            style = MaterialTheme.typography.labelMedium,
                            color = MediumGray
                        )
                    }
                }
                
                // Form Score
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Form Score",
                                style = MaterialTheme.typography.labelMedium,
                                color = MediumGray
                            )
                            Text(
                                text = summary.formLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    summary.formScore >= 80 -> ElectricGreen
                                    summary.formScore >= 60 -> Amber
                                    else -> AlertRed
                                }
                            )
                        }
                        
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = summary.formScore / 100f,
                                modifier = Modifier.size(50.dp),
                                color = when {
                                    summary.formScore >= 80 -> ElectricGreen
                                    summary.formScore >= 60 -> Amber
                                    else -> AlertRed
                                },
                                trackColor = SubtleGray,
                                strokeCap = StrokeCap.Round
                            )
                            Text(
                                text = summary.formScore.toInt().toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OffWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Action Buttons
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DeepDark
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "RESUME",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepDark
                    )
                }

                TextButton(
                    onClick = onEndWorkout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "End Workout",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AlertRed
                    )
                }
            }
        }
    }
}

/**
 * Exit confirmation dialog
 */
@Composable
fun ExitConfirmationDialog(
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "End Workout?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Your progress will be saved and you can review it later.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue", color = ElectricGreen)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertRed
                )
            ) {
                Text("End Workout", color = OffWhite)
            }
        }
    )
}
