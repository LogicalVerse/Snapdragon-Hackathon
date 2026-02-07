package com.example.myapplication.data

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.R

enum class ExerciseDifficulty {
    BEGINNER, INTERMEDIATE, ADVANCED
}

enum class MusclGroup {
    QUADS, GLUTES, HAMSTRINGS, CHEST, TRICEPS, SHOULDERS, BACK, BICEPS, ABS, LEGS
}

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector = Icons.Default.FavoriteBorder,
    @DrawableRes val imageRes: Int? = null, // New field for drawable resource
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
    val muscleGroups: List<MusclGroup> = emptyList(),
    val isAvailable: Boolean = true,
    val lastPerformed: String? = null, // e.g., "2 days ago"
    val personalRecord: String? = null // e.g., "50 lbs"
)

// Sample exercises data
object SampleExercises {
    val squats = Exercise(
        id = "squats",
        name = "Squats",
        description = "Lower body strength exercise",
        imageRes = R.drawable.ic_squat,
        difficulty = ExerciseDifficulty.BEGINNER,
        muscleGroups = listOf(MusclGroup.QUADS, MusclGroup.GLUTES, MusclGroup.HAMSTRINGS),
        isAvailable = true,
        lastPerformed = "2 days ago",
        personalRecord = "225 lbs"
    )

    val pushups = Exercise(
        id = "pushups",
        name = "Push-ups",
        description = "Upper body calisthenics exercise",
        imageRes = R.drawable.ic_pushup,
        difficulty = ExerciseDifficulty.BEGINNER,
        muscleGroups = listOf(MusclGroup.CHEST, MusclGroup.TRICEPS, MusclGroup.SHOULDERS),
        isAvailable = true,
        lastPerformed = "Yesterday",
        personalRecord = "45 reps"
    )

    val deadlifts = Exercise(
        id = "deadlifts",
        name = "Deadlifts",
        description = "Full-body compound lift",
        imageRes = R.drawable.ic_deadlift,
        difficulty = ExerciseDifficulty.ADVANCED,
        muscleGroups = listOf(MusclGroup.BACK, MusclGroup.GLUTES, MusclGroup.HAMSTRINGS),
        isAvailable = false,
        personalRecord = "315 lbs"
    )

    val benchPress = Exercise(
        id = "bench_press",
        name = "Bench Press",
        description = "Upper body pressing movement",
        imageRes = R.drawable.ic_benchpress,
        difficulty = ExerciseDifficulty.INTERMEDIATE,
        muscleGroups = listOf(MusclGroup.CHEST, MusclGroup.TRICEPS, MusclGroup.SHOULDERS),
        isAvailable = true,
        lastPerformed = "3 days ago",
        personalRecord = "275 lbs"
    )

    val rows = Exercise(
        id = "rows",
        name = "Barbell Rows",
        description = "Back and biceps compound movement",
        imageRes = R.drawable.ic_rows,
        difficulty = ExerciseDifficulty.INTERMEDIATE,
        muscleGroups = listOf(MusclGroup.BACK, MusclGroup.BICEPS),
        isAvailable = true,
        lastPerformed = "1 week ago",
        personalRecord = "300 lbs"
    )

    val all = listOf(squats, pushups, deadlifts, benchPress, rows)
    val available = all.filter { it.isAvailable }
}
