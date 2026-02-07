package com.example.myapplication.pose

import com.example.myapplication.pose.trainers.*

/**
 * Factory for creating exercise-specific trainers.
 */
object ExerciseTrainerFactory {
    
    /**
     * Create an ExerciseTrainer for the given exercise ID.
     * 
     * @param exerciseId The exercise ID (e.g., "squats", "pushup", "deadlift")
     * @param mode Workout mode (BEGINNER or PRO)
     * @return The appropriate ExerciseTrainer implementation
     */
    fun create(exerciseId: String, mode: WorkoutMode = WorkoutMode.BEGINNER): ExerciseTrainer {
        return when (exerciseId.lowercase()) {
            "squats", "squat", "barbell_squat", "goblet_squat" -> SquatTrainer(mode)
            "pushup", "pushups", "push_ups", "push-ups" -> PushUpTrainer(mode)
            "deadlift", "deadlifts", "romanian_deadlift", "sumo_deadlift" -> DeadliftTrainer(mode)
            "bench_press", "benchpress", "bench", "dumbbell_press" -> BenchPressTrainer(mode)
            "rows", "row", "bent_over_row", "barbell_row", "dumbbell_row" -> RowTrainer(mode)
            "bicep_curl", "bicep_curls", "curls", "curl", "dumbbell_curl" -> BicepCurlTrainer(mode)
            else -> {
                // Default to squat for unknown exercises
                android.util.Log.w("ExerciseTrainerFactory", "Unknown exercise '$exerciseId', defaulting to Squat")
                SquatTrainer(mode)
            }
        }
    }
    
    /**
     * Get list of supported exercise IDs.
     */
    fun getSupportedExercises(): List<String> {
        return listOf("squats", "pushup", "deadlift", "bench_press", "rows", "bicep_curl")
    }
    
    /**
     * Check if an exercise is supported.
     */
    fun isSupported(exerciseId: String): Boolean {
        return when (exerciseId.lowercase()) {
            "squats", "squat", "barbell_squat", "goblet_squat",
            "pushup", "pushups", "push_ups", "push-ups",
            "deadlift", "deadlifts", "romanian_deadlift", "sumo_deadlift",
            "bench_press", "benchpress", "bench", "dumbbell_press",
            "rows", "row", "bent_over_row", "barbell_row", "dumbbell_row",
            "bicep_curl", "bicep_curls", "curls", "curl", "dumbbell_curl" -> true
            else -> false
        }
    }
}
