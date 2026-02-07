package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.WorkoutPreferences
import com.example.myapplication.pose.WorkoutSummary
import com.example.myapplication.ui.screens.ActiveWorkoutScreen
import com.example.myapplication.ui.screens.AnalysisScreen
import com.example.myapplication.ui.screens.CameraSetupScreen
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.SettingsScreen
import com.example.myapplication.ui.theme.FormlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Hide navigation bar for immersive experience
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        
        setContent {
            FormlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FormlyApp()
                }
            }
        }
    }
}

enum class Screen {
    HOME,
    CAMERA_SETUP,
    ACTIVE_WORKOUT,
    ANALYSIS,
    SETTINGS
}

@Composable
fun FormlyApp() {
    val context = LocalContext.current
    val workoutPrefs = remember { WorkoutPreferences(context) }
    
    // Initialize VoiceFeedbackManager at app level so TTS is ready by the time workout starts
    val voiceFeedbackManager = remember { com.example.myapplication.audio.VoiceFeedbackManager(context) }
    
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var isWorkoutStarted by remember { mutableStateOf(false) }
    var selectedExerciseId by remember { mutableStateOf("squats") }
    var selectedExerciseName by remember { mutableStateOf("Squats") }
    
    // Store workout summary and video for analysis screen
    var workoutSummary by remember { mutableStateOf(WorkoutSummary()) }
    var lastVideoUri by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                workoutHistory = workoutPrefs.getWorkoutHistory(),
                onExerciseClick = { exercise ->
                    // Record workout when exercise is clicked
                    workoutPrefs.recordWorkout()
                    isWorkoutStarted = false  // Reset when starting fresh
                    selectedExerciseId = exercise.id
                    selectedExerciseName = exercise.name
                    currentScreen = Screen.CAMERA_SETUP
                },
                onSettingsClick = {
                    currentScreen = Screen.SETTINGS
                }
            )
        }
        Screen.CAMERA_SETUP -> {
            CameraSetupScreen(
                voiceFeedbackManager = voiceFeedbackManager,
                isWorkoutStarted = isWorkoutStarted,
                exerciseId = selectedExerciseId,
                onBackPressed = {
                    isWorkoutStarted = false
                    currentScreen = Screen.HOME
                },
                onStartWorkout = {
                    isWorkoutStarted = true
                },
                onPauseWorkout = { summary, videoUri ->
                    workoutSummary = summary
                    lastVideoUri = videoUri
                    currentScreen = Screen.ANALYSIS
                }
            )
        }
        Screen.ACTIVE_WORKOUT -> {
            ActiveWorkoutScreen(
                onFinishPressed = {
                    currentScreen = Screen.ANALYSIS
                },
                onStopPressed = {
                    currentScreen = Screen.HOME
                }
            )
        }
        Screen.ANALYSIS -> {
            AnalysisScreen(
                onBackPressed = {
                    currentScreen = Screen.HOME
                },
                onResume = {
                    currentScreen = Screen.CAMERA_SETUP  // Return to camera with workout active
                },
                onEndWorkout = {
                    isWorkoutStarted = false
                    currentScreen = Screen.HOME
                },
                exerciseName = selectedExerciseName,
                exerciseId = selectedExerciseId,
                summary = workoutSummary,
                videoUri = lastVideoUri
            )
        }
        Screen.SETTINGS -> {
            SettingsScreen(
                onBackPressed = {
                    currentScreen = Screen.HOME
                }
            )
        }
    }
}