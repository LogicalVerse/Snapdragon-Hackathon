package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.pose.WorkoutSummary
import com.example.myapplication.pose.RepInfo
import com.example.myapplication.ui.theme.ElectricGreen
import com.example.myapplication.ui.theme.Spacing
import android.net.Uri
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect

import com.example.myapplication.network.GeminiRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Analysis Screen - Shows detailed workout summary with Resume and End Workout options
 */
@Composable
fun AnalysisScreen(
    onBackPressed: () -> Unit = {},
    onResume: () -> Unit = {},
    onEndWorkout: () -> Unit = {},
    exerciseName: String = "Squats",
    summary: WorkoutSummary = WorkoutSummary(),
    videoUri: String? = null
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.lg)
            .padding(top = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = "Workout Paused",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rep Counter Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Correct Reps
                RepCountCard(
                    modifier = Modifier.weight(1f),
                    count = summary.correctReps,
                    label = "Correct",
                    icon = Icons.Default.Check,
                    color = ElectricGreen
                )
                
                // Incorrect Reps
                RepCountCard(
                    modifier = Modifier.weight(1f),
                    count = summary.incorrectReps,
                    label = "Incorrect",
                    icon = Icons.Default.Close,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Video Preview (if available)
            if (videoUri != null) {
                VideoPreviewCard(
                    videoUri = videoUri,
                    context = context
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                }
            
            // AI Coaching Card (analyze video with Gemini)
            if (videoUri != null) {
                AiCoachingCard(
                    videoUri = videoUri,
                    context = context
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            
            // Form Score Card
            FormScoreCard(
                score = summary.formScore,
                label = summary.formLabel
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Stats Card
            StatsCard(summary = summary)
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // Feedback Issues Card (if any)
            if (summary.feedbackCounts.isNotEmpty()) {
                FeedbackBreakdownCard(
                    feedbackCounts = summary.feedbackCounts,
                    mostCommonIssue = summary.mostCommonIssueDisplay
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            
            // Per-rep breakdown (if available)
            if (summary.repDetails.isNotEmpty()) {
                RepBreakdownCard(repDetails = summary.repDetails)
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
        }
        
        // Action Buttons
        Column(
            modifier = Modifier.padding(bottom = Spacing.lg)
        ) {
            // Resume Button (Green)
            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Resume Workout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // End Workout Button (Red)
            Button(
                onClick = onEndWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "End Workout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@Composable
private fun RepCountCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FormScoreCard(
    score: Float,
    label: String
) {
    val scoreColor = when {
        score >= 80f -> ElectricGreen
        score >= 60f -> Color(0xFFFFC107) // Amber
        score >= 40f -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.error
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Form Score",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Text(
                text = "${score.toInt()}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            LinearProgressIndicator(
                progress = (score / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun StatsCard(summary: WorkoutSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                text = "Workout Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            StatRow(label = "Duration", value = summary.formattedDuration)
            Divider(
                modifier = Modifier.padding(vertical = Spacing.sm),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            StatRow(label = "Total Reps", value = summary.totalReps.toString())
            Divider(
                modifier = Modifier.padding(vertical = Spacing.sm),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            StatRow(
                label = "Accuracy",
                value = "${summary.accuracyPercentage.toInt()}%"
            )
            Divider(
                modifier = Modifier.padding(vertical = Spacing.sm),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            StatRow(
                label = "Avg Depth",
                value = "${summary.averageDepthAngle.toInt()}°"
            )
            Divider(
                modifier = Modifier.padding(vertical = Spacing.sm),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            StatRow(
                label = "Best Depth",
                value = "${summary.bestDepthAngle.toInt()}°"
            )
        }
    }
}

@Composable
private fun FeedbackBreakdownCard(
    feedbackCounts: Map<String, Int>,
    mostCommonIssue: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Form Issues Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Text(
                text = "Most Common: $mostCommonIssue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            feedbackCounts.entries.take(3).forEach { (feedback, count) ->
                val displayName = feedback.replace("_", " ")
                    .replaceFirstChar { it.uppercase() }
                Text(
                    text = "• $displayName: $count times",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Video preview card with playback
 */
@Composable
private fun VideoPreviewCard(
    videoUri: String,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Text(
                text = "📹 Workout Recording",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Video player
            AndroidView(
                factory = { ctx ->
                    try {
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(videoUri))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                            setOnErrorListener { _, what, extra ->
                                android.util.Log.e("AnalysisScreen", "Video play error: $what, $extra")
                                true
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AnalysisScreen", "Failed to initialize VideoView", e)
                        // Return dummy view to avoid crash
                        android.view.View(ctx)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

/**
 * Per-rep breakdown card showing individual rep details
 */
@Composable
fun RepBreakdownCard(
    repDetails: List<RepInfo>,
    modifier: Modifier = Modifier
) {
    if (repDetails.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Text(
                text = "📊 Rep Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            repDetails.forEach { rep ->
                RepDetailRow(rep)
                if (rep != repDetails.last()) {
                    Divider(
                        modifier = Modifier.padding(vertical = Spacing.xs),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepDetailRow(rep: RepInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Rep number and status icon
            Icon(
                imageVector = if (rep.isCorrect) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (rep.isCorrect) "Correct" else "Incorrect",
                tint = if (rep.isCorrect) ElectricGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(Spacing.sm))
            
            Text(
                text = "Rep ${rep.repNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Depth percentage
            Text(
                text = "${rep.depthPercentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    rep.depthPercentage >= 80 -> ElectricGreen
                    rep.depthPercentage >= 50 -> Color(0xFFFFA726)
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            // Duration
            Text(
                text = "${(rep.durationMs / 1000.0).coerceIn(0.0, 99.9).let { "%.1fs".format(it) }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}




/**
 * AI Coaching Card - Displays Gemini AI feedback comparing user form to professional reference
 */
@Composable
fun AiCoachingCard(
    videoUri: String,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    var feedbackState by remember { mutableStateOf<AiFeedbackState>(AiFeedbackState.Loading) }
    
    // Safely create repository - catch any initialization errors
    val geminiRepository = remember {
        try {
            GeminiRepository(context)
        } catch (e: Exception) {
            android.util.Log.e("AiCoachingCard", "Failed to create repository", e)
            null
        }
    }
    
    // Start analysis when component is displayed
    LaunchedEffect(videoUri) {
        try {
            if (geminiRepository == null) {
                feedbackState = AiFeedbackState.Error("Oops! AI coach unavailable")
                return@LaunchedEffect
            }
            
            feedbackState = AiFeedbackState.Loading
            val result = geminiRepository.analyzeSquatForm(videoUri)
            feedbackState = result.fold(
                onSuccess = { AiFeedbackState.Success(it) },
                onFailure = { AiFeedbackState.Error(it.message ?: "Analysis failed") }
            )
        } catch (e: Exception) {
            // Catch any uncaught exception to prevent crash
            android.util.Log.e("AiCoachingCard", "Analysis crashed", e)
            feedbackState = AiFeedbackState.Error("Oops! Analysis unavailable")
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A237E).copy(alpha = 0.2f)  // Deep blue tint
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🤖",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "AI Coach Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            when (val state = feedbackState) {
                is AiFeedbackState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Simple text loading indicator (avoids Compose version conflicts)
                        Text(
                            text = "⏳",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(
                            text = "Analyzing your form...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is AiFeedbackState.Success -> {
                    Text(
                        text = state.feedback,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
                    )
                }
                is AiFeedbackState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Could not analyze: ${state.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * State for AI feedback loading
 */
sealed class AiFeedbackState {
    object Loading : AiFeedbackState()
    data class Success(val feedback: String) : AiFeedbackState()
    data class Error(val message: String) : AiFeedbackState()
}
