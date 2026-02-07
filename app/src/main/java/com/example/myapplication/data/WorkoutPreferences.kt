package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * SharedPreferences wrapper for workout history and user settings
 */
class WorkoutPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "formly_prefs"
        private const val KEY_WORKOUT_DATES = "workout_dates"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AGE = "user_age"
        private const val KEY_USER_HEIGHT = "user_height"
        private const val KEY_USER_WEIGHT = "user_weight"
        
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }
    
    // ==================== Workout History ====================
    
    /**
     * Record a workout for the given date (defaults to today)
     */
    fun recordWorkout(date: LocalDate = LocalDate.now()) {
        val existing = getWorkoutDatesSet().toMutableSet()
        existing.add(date.format(DATE_FORMATTER))
        prefs.edit().putStringSet(KEY_WORKOUT_DATES, existing).apply()
    }
    
    /**
     * Get workout history as a map of date to workout count
     * For now, each date is counted as 1 workout
     */
    fun getWorkoutHistory(): Map<LocalDate, Int> {
        val dateStrings = getWorkoutDatesSet()
        return dateStrings.mapNotNull { dateStr ->
            try {
                LocalDate.parse(dateStr, DATE_FORMATTER) to 1
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }
    
    private fun getWorkoutDatesSet(): Set<String> {
        return prefs.getStringSet(KEY_WORKOUT_DATES, emptySet()) ?: emptySet()
    }
    
    // ==================== User Profile ====================
    
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun setUserName(name: String) = prefs.edit().putString(KEY_USER_NAME, name).apply()
    
    fun getUserAge(): Int = prefs.getInt(KEY_USER_AGE, 0)
    fun setUserAge(age: Int) = prefs.edit().putInt(KEY_USER_AGE, age).apply()
    
    fun getUserHeight(): Float = prefs.getFloat(KEY_USER_HEIGHT, 0f)
    fun setUserHeight(height: Float) = prefs.edit().putFloat(KEY_USER_HEIGHT, height).apply()
    
    fun getUserWeight(): Float = prefs.getFloat(KEY_USER_WEIGHT, 0f)
    fun setUserWeight(weight: Float) = prefs.edit().putFloat(KEY_USER_WEIGHT, weight).apply()
}
