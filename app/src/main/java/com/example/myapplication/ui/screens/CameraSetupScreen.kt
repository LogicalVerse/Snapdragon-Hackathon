package com.example.myapplication.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import com.example.myapplication.camera.CameraManager
import com.example.myapplication.data.PositioningGuide
import com.example.myapplication.ui.theme.AlertRed
import com.example.myapplication.ui.theme.Amber
import com.example.myapplication.ui.theme.CornerRadius
import com.example.myapplication.ui.theme.DeepDark
import com.example.myapplication.ui.theme.ElectricGreen
import com.example.myapplication.ui.theme.ElevatedDark
import com.example.myapplication.ui.theme.LightGray
import com.example.myapplication.ui.theme.MediumGray
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.Spacing
import com.example.myapplication.ui.theme.SubtleGray
import com.example.myapplication.ui.components.SimplifiedPoseOverlay
import com.example.myapplication.ui.components.RealTimeAnalysisOverlay
import com.example.myapplication.pose.AnalysisResult
import com.example.myapplication.pose.FeedbackType
import com.example.myapplication.pose.PoseLandmark
import com.example.myapplication.pose.WorkoutSummary
import com.example.myapplication.audio.VoiceFeedbackManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Camera Setup Screen - Use phone camera with front/back switching
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraSetupScreen(
    isWorkoutStarted: Boolean = false,
    exerciseId: String = "squats",
    onBackPressed: () -> Unit = {},
    onHelpPressed: () -> Unit = {},
    onStartWorkout: () -> Unit = {},
    onPauseWorkout: (WorkoutSummary, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Camera permission
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    // Camera manager
    val cameraManager = remember { CameraManager(context) }
    
    // Voice feedback manager - created once per screen instance
    val voiceFeedbackManager = remember { VoiceFeedbackManager(context) }
    
    // Session counter - increments each time screen is displayed to force effect re-runs
    // This ensures recording/pose restart even when isWorkoutStarted doesn't change (resume flow)
    var sessionId by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        sessionId++
    }
    
    // UI state
    var showGridOverlay by remember { mutableStateOf(false) }
    var showPositioningGuide by remember { mutableStateOf(false) }
    var isUsingFrontCamera by remember { mutableStateOf(false) }
    var autoHideControls by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var detectedLandmarks by remember { mutableStateOf<List<PoseLandmark>>(emptyList()) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var lastRepCount by remember { mutableStateOf(0) }
    
    // Preview view
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    
    // Setup camera callbacks
    LaunchedEffect(Unit) {
        cameraManager.onRecordingSaved = { message ->
            statusMessage = message
        }
        cameraManager.onError = { error ->
            statusMessage = error
        }
        cameraManager.onPoseDetected = { landmarks ->
            detectedLandmarks = landmarks
        }
        cameraManager.onAnalysisResult = { result ->
            analysisResult = result
            
            // Voice feedback
            if (isWorkoutStarted && result != null) {
                // Announce rep count changes
                val newRepCount = result.repCount
                if (newRepCount > lastRepCount) {
                    voiceFeedbackManager.announceRep(newRepCount, result.correctCount > 0)
                    lastRepCount = newRepCount
                }
                
                // Speak form feedback
                voiceFeedbackManager.speakFeedback(result.feedbackType)
            }
        }
    }
    
    // Start/stop pose estimation and recording based on workout state
    // Key on sessionId ensures this retriggers on screen resume (when isWorkoutStarted is still true)
    LaunchedEffect(isWorkoutStarted, sessionId) {
        if (isWorkoutStarted && sessionId > 0) {
            // Small delay to ensure camera is fully initialized on resume
            kotlinx.coroutines.delay(300)
            // Start on-device pose estimation and analysis with correct exercise
            cameraManager.startPoseEstimation(exerciseId = exerciseId)
            // Auto-start video recording
            cameraManager.startRecording()
            // Reset voice feedback for new workout session
            voiceFeedbackManager.reset()
            lastRepCount = 0
        } else if (!isWorkoutStarted) {
            cameraManager.stopPoseEstimation()
            // Stop recording when pausing
            cameraManager.stopRecording()
            detectedLandmarks = emptyList()
            analysisResult = null
        }
    }
    
    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
        if (!audioPermission.status.isGranted) {
            audioPermission.launchPermissionRequest()
        }
    }
    
    // Start camera when permission granted
    LaunchedEffect(cameraPermission.status.isGranted, isUsingFrontCamera) {
        if (cameraPermission.status.isGranted) {
            cameraManager.startCamera(lifecycleOwner, previewView, isUsingFrontCamera)
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.shutdown()
            voiceFeedbackManager.shutdown()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                autoHideControls = !autoHideControls
            }
    ) {
        // Camera Preview or Permission Request
        if (cameraPermission.status.isGranted) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            
            // Pose overlay when workout is started
            if (isWorkoutStarted && detectedLandmarks.isNotEmpty()) {
                SimplifiedPoseOverlay(
                    landmarks = detectedLandmarks,
                    modifier = Modifier.fillMaxSize(),
                    mirrorHorizontally = isUsingFrontCamera
                )
                
                // Real-time analysis overlay with rep count, feedback, etc.
                RealTimeAnalysisOverlay(
                    analysisResult = analysisResult,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Permission not granted - show request UI
            PermissionRequestUI(
                onRequestPermission = { cameraPermission.launchPermissionRequest() },
                shouldShowRationale = cameraPermission.status.shouldShowRationale,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Grid Overlay
        if (showGridOverlay && cameraPermission.status.isGranted) {
            GridOverlay(modifier = Modifier.fillMaxSize())
        }
        
        // Top Bar - hide during workout to not block analysis overlay
        AnimatedVisibility(
            visible = !isWorkoutStarted && !autoHideControls,
            enter = slideInVertically(),
            exit = slideOutVertically(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            CameraTopBar(
                onBackPressed = onBackPressed,
                onHelpPressed = onHelpPressed,
                onGridToggle = { showGridOverlay = !showGridOverlay },
                isGridVisible = showGridOverlay,
                modifier = Modifier
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            )
        }
        
        // Camera Switch Button (top right) - hide during workout
        AnimatedVisibility(
            visible = cameraPermission.status.isGranted && !autoHideControls && !isWorkoutStarted,
            enter = slideInVertically(),
            exit = slideOutVertically(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            IconButton(
                onClick = {
                    isUsingFrontCamera = !isUsingFrontCamera
                    cameraManager.switchCamera(lifecycleOwner, previewView)
                },
                modifier = Modifier
                    .padding(top = statusBarPadding + 56.dp, end = 16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Switch Camera",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        // Status message banner
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = slideInVertically(),
            exit = slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            StatusBanner(
                message = statusMessage ?: "",
                onDismiss = { statusMessage = null },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Positioning Guide Modal
        if (showPositioningGuide) {
            PositioningGuideModal(
                onDismiss = { showPositioningGuide = false },
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Bottom Control Panel
        if (!autoHideControls && cameraPermission.status.isGranted) {
            CameraBottomPanel(
                isWorkoutStarted = isWorkoutStarted,
                onStartWorkout = onStartWorkout,
                onPauseWorkout = {
                    // Get summary before stopping
                    val summary = cameraManager.getSummary()
                    // Stop recording and wait for finalization with callback
                    cameraManager.stopRecordingWithCallback { videoUri ->
                        onPauseWorkout(summary, videoUri)
                    }
                },
                onShowPositioningGuide = { showPositioningGuide = true },
                isUsingFrontCamera = isUsingFrontCamera,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Permission request UI
 */
@Composable
fun PermissionRequestUI(
    onRequestPermission: () -> Unit,
    shouldShowRationale: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDark)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = ElectricGreen,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OffWhite,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (shouldShowRationale) {
                "Formly needs camera access to record your workouts and analyze your form. Please grant camera permission in Settings."
            } else {
                "Formly needs camera access to record your workouts and analyze your form."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MediumGray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen)
        ) {
            Text(
                text = "Grant Permission",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = DeepDark
            )
        }
    }
}

/**
 * Status banner for messages
 */
@Composable
fun StatusBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .padding(horizontal = Spacing.md),
        shape = RoundedCornerShape(CornerRadius.md),
        colors = CardDefaults.cardColors(containerColor = ElectricGreen.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DeepDark,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepDark
                )
            }
            
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = DeepDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Top app bar for camera
 */
@Composable
fun CameraTopBar(
    onBackPressed: () -> Unit = {},
    onHelpPressed: () -> Unit = {},
    onGridToggle: () -> Unit = {},
    isGridVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(color = DeepDark.copy(alpha = 0.8f))
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = ElectricGreen,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = "Camera Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OffWhite
        )

        Row {
            IconButton(onClick = onGridToggle) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Grid",
                    tint = if (isGridVisible) ElectricGreen else MediumGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onHelpPressed) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Help",
                    tint = ElectricGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Bottom control panel with camera controls
 */
@Composable
fun CameraBottomPanel(
    isWorkoutStarted: Boolean = false,
    onStartWorkout: () -> Unit = {},
    onPauseWorkout: () -> Unit = {},
    onShowPositioningGuide: () -> Unit = {},
    isUsingFrontCamera: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, DeepDark.copy(alpha = 0.95f))
                )
            )
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Camera info
        Text(
            text = if (isUsingFrontCamera) "📷 Front Camera" else "📷 Back Camera",
            style = MaterialTheme.typography.bodyMedium,
            color = MediumGray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // Start Workout / Pause Button based on state
        if (isWorkoutStarted) {
            // PAUSE button when workout is active
            Button(
                onClick = onPauseWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = DeepDark,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    Text(
                        text = "PAUSE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepDark
                    )
                }
            }
        } else {
            // START WORKOUT button
            Button(
                onClick = onStartWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DeepDark,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    Text(
                        text = "START WORKOUT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepDark
                    )
                }
            }
        }

        // Show Positioning Guide (only when not started)
        if (!isWorkoutStarted) {
            TextButton(
                onClick = onShowPositioningGuide,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ElectricGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(Spacing.sm))
                Text(
                    text = "Show Positioning Guide",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ElectricGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
    }
}

/**
 * Grid overlay for camera alignment
 */
@Composable
fun GridOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // Vertical lines
        repeat(2) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.33f * (index + 1))
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 1.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        }

        // Horizontal lines
        repeat(2) { index ->
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.33f * (index + 1))
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 1.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        }

        // Center crosshair
        Box(modifier = Modifier.align(Alignment.Center)) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

/**
 * Positioning guide modal dialog
 */
@Composable
fun PositioningGuideModal(
    onDismiss: () -> Unit = {},
    guide: PositioningGuide = PositioningGuide(),
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                horizontalAlignment = Alignment.Start
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MediumGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = guide.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(color = SubtleGray, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📱\n6-8 ft\nHip Height",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediumGray,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    guide.checklist.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ElectricGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightGray
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen)
                ) {
                    Text(
                        text = "Got it!",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepDark
                    )
                }
            }
        }
    }
}
