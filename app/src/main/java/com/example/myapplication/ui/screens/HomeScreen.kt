package com.example.myapplication.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Exercise
import com.example.myapplication.data.ExerciseDifficulty
import com.example.myapplication.data.MusclGroup
import com.example.myapplication.data.SampleExercises
import com.example.myapplication.ui.theme.ElectricGreen
import com.example.myapplication.ui.theme.DeepBlue
import com.example.myapplication.ui.theme.EnergeticOrange
import com.example.myapplication.ui.theme.Spacing
import com.example.myapplication.ui.theme.SubtleGray
import com.example.myapplication.ui.theme.ElevatedDark
import com.example.myapplication.ui.theme.DeepDark
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.LightGray
import com.example.myapplication.ui.theme.MediumGray
import com.example.myapplication.ui.theme.AlertRed
import com.example.myapplication.ui.theme.Amber
import com.example.myapplication.ui.theme.White
import com.example.myapplication.ui.theme.CornerRadius

/**
 * Home Screen - Main entry point for Formly app
 * Shows available exercises for form analysis
 */
@Composable
fun HomeScreen(
    workoutHistory: Map<java.time.LocalDate, Int> = emptyMap(),
    onExerciseClick: (Exercise) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    exercises: List<Exercise> = SampleExercises.all
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar with status bar padding
        FormlyTopBar(
            onSettingsClick = onSettingsClick,
            modifier = Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = Spacing.xl
            )
        ) {
            // Welcome Banner
            item {
                WelcomeBanner(
                    modifier = Modifier.padding(
                        horizontal = Spacing.md,
                        vertical = Spacing.md
                    )
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Streak Heat Map
            item {
                com.example.myapplication.ui.components.StreakHeatMap(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    history = workoutHistory
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }

            // Section Header
            item {
                SectionHeader(
                    title = "Your Exercises",
                    count = exercises.size,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Exercise Cards
            items(exercises) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onClick = { onExerciseClick(exercise) },
                    modifier = Modifier
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Top app bar with logo and settings
 */
@Composable
fun FormlyTopBar(
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Logo/Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.myapplication.R.drawable.ic_logo),
                    contentDescription = "Formly Logo",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Formly",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElectricGreen
                )
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = ElectricGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Welcome banner card with gradient background
 */
@Composable
fun WelcomeBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(CornerRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = SubtleGray
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(ElectricGreen.copy(alpha = 0.1f), DeepBlue.copy(alpha = 0.1f))
                    )
                )
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Fitness Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = ElectricGreen.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Fitness",
                        tint = ElectricGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.md))

                // Welcome Text
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Welcome back!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = "Perfect your form • 2-day streak 🔥",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediumGray
                    )
                }
            }
        }
    }
}

/**
 * Section header with title and optional count badge
 */
@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OffWhite
        )

        if (count != null) {
            Box(
                modifier = Modifier
                    .background(
                        color = SubtleGray,
                        shape = RoundedCornerShape(CornerRadius.sm)
                    )
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count available",
                    style = MaterialTheme.typography.labelMedium,
                    color = MediumGray
                )
            }
        }
    }
}

/**
 * Individual exercise card with full layout
 */
@Composable
fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }

    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (exercise.isAvailable) {
                    onClick()
                }
            }
            .alpha(if (exercise.isAvailable) 1f else 0.6f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ElevatedDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // LEFT SIDE: Exercise Icon Container
            ExerciseIconContainer(
                exercise = exercise,
                modifier = Modifier.size(80.dp)
            )

            // MIDDLE SECTION: Exercise Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Exercise Name
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )

                // Description
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MediumGray,
                    maxLines = 1
                )

                // Muscle Groups Badges
                MuscleGroupBadges(
                    muscleGroups = exercise.muscleGroups,
                    modifier = Modifier.fillMaxWidth()
                )

                // Difficulty Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DifficultyBadge(
                        difficulty = exercise.difficulty,
                        modifier = Modifier.clip(RoundedCornerShape(CornerRadius.sm))
                    )

                    if (exercise.personalRecord != null) {
                        Text(
                            text = "PR: ${exercise.personalRecord}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // RIGHT SIDE: Action Button or Coming Soon
            if (exercise.isAvailable) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = ElectricGreen,
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Start exercise",
                        tint = DeepDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.size(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MediumGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Exercise icon container with gradient background
 */
@Composable
fun ExerciseIconContainer(
    exercise: Exercise,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SubtleGray,
                        SubtleGray.copy(alpha = 0.7f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (exercise.imageRes != null) {
            // Use drawable image
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = exercise.imageRes),
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .alpha(if (exercise.isAvailable) 1f else 0.5f),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            // Fallback to vector icon
            Icon(
                imageVector = exercise.icon,
                contentDescription = exercise.name,
                tint = if (exercise.isAvailable) ElectricGreen else MediumGray,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/**
 * Muscle group badges with colored dots
 */
@Composable
fun MuscleGroupBadges(
    muscleGroups: List<MusclGroup>,
    modifier: Modifier = Modifier
) {
    if (muscleGroups.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        muscleGroups.take(3).forEachIndexed { index, muscle ->
            Box(
                modifier = Modifier
                    .background(
                        color = getMuscleGroupColor(muscle),
                        shape = CircleShape
                    )
                    .size(4.dp)
            )

            if (index < 2) {
                Text(
                    text = getMuscleGroupName(muscle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray
                )
            }
        }

        if (muscleGroups.size > 3) {
            Text(
                text = "+${muscleGroups.size - 3}",
                style = MaterialTheme.typography.labelSmall,
                color = MediumGray
            )
        }
    }
}

/**
 * Difficulty badge with color coding
 */
@Composable
fun DifficultyBadge(
    difficulty: ExerciseDifficulty,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = when (difficulty) {
        ExerciseDifficulty.BEGINNER -> Triple(
            ElectricGreen.copy(alpha = 0.15f),
            ElectricGreen,
            "BEG"
        )
        ExerciseDifficulty.INTERMEDIATE -> Triple(
            Amber.copy(alpha = 0.15f),
            Amber,
            "INT"
        )
        ExerciseDifficulty.ADVANCED -> Triple(
            AlertRed.copy(alpha = 0.15f),
            AlertRed,
            "ADV"
        )
    }

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(CornerRadius.sm)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Helper function to get muscle group display name
 */
private fun getMuscleGroupName(muscle: MusclGroup): String = when (muscle) {
    MusclGroup.QUADS -> "Quads"
    MusclGroup.GLUTES -> "Glutes"
    MusclGroup.HAMSTRINGS -> "Hams"
    MusclGroup.CHEST -> "Chest"
    MusclGroup.TRICEPS -> "Tri"
    MusclGroup.SHOULDERS -> "Sho"
    MusclGroup.BACK -> "Back"
    MusclGroup.BICEPS -> "Bi"
    MusclGroup.ABS -> "Abs"
    MusclGroup.LEGS -> "Legs"
}

/**
 * Helper function to get muscle group color
 */
private fun getMuscleGroupColor(muscle: MusclGroup) = when (muscle) {
    MusclGroup.QUADS -> ElectricGreen
    MusclGroup.GLUTES -> ElectricGreen
    MusclGroup.HAMSTRINGS -> DeepBlue
    MusclGroup.CHEST -> EnergeticOrange
    MusclGroup.TRICEPS -> EnergeticOrange
    MusclGroup.SHOULDERS -> DeepBlue
    MusclGroup.BACK -> ElectricGreen
    MusclGroup.BICEPS -> DeepBlue
    MusclGroup.ABS -> EnergeticOrange
    MusclGroup.LEGS -> ElectricGreen
}
