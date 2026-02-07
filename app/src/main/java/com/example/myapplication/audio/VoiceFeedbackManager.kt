package com.example.myapplication.audio

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.myapplication.pose.FeedbackType
import java.util.Locale

/**
 * Manages voice feedback using Text-to-Speech for hands-free workout coaching.
 */
class VoiceFeedbackManager(context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var lastSpokenMessage: String? = null
    private var lastSpeakTimeMs = 0L
    
    // Main thread handler for TTS calls
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Minimum interval between messages to avoid spam
    private val minIntervalMs = 1500L
    
    // Track last feedback type to avoid repeating
    private var lastFeedbackType: FeedbackType? = null
    private var lastRepCount = 0
    
    // Audio parameters for TTS
    private val audioParams = Bundle().apply {
        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
    }
    
    init {
        Log.d(TAG, "Creating TTS engine...")
        tts = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post {
                if (status == TextToSpeech.SUCCESS) {
                    val langResult = tts?.setLanguage(Locale.US)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "TTS Language not supported, result: $langResult")
                        return@post
                    }
                    tts?.setSpeechRate(1.0f)
                    tts?.setPitch(1.0f)
                    isInitialized = true
                    Log.d(TAG, "TTS initialized successfully, testing with 'Ready'...")
                    
                    // Test speak with audio params
                    val testResult = tts?.speak("Ready", TextToSpeech.QUEUE_ADD, audioParams, "init_test")
                    Log.d(TAG, "Test speak result: $testResult (0 = SUCCESS)")
                } else {
                    Log.e(TAG, "TTS initialization failed with status: $status")
                }
            }
        }
    }
    
    /**
     * Speak a message with optional priority.
     * Priority messages interrupt current speech.
     */
    fun speak(message: String, priority: Boolean = false) {
        Log.d(TAG, "speak() called: '$message', initialized=$isInitialized, priority=$priority")
        
        if (!isInitialized || message.isBlank()) {
            Log.w(TAG, "TTS not ready or message blank. Initialized: $isInitialized")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Check if we should speak (avoid spam) - but always allow priority
        if (!priority && message == lastSpokenMessage && 
            (currentTime - lastSpeakTimeMs) < minIntervalMs) {
            Log.d(TAG, "Skipping duplicate message: $message")
            return
        }
        
        val queueMode = if (priority) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }
        
        Log.d(TAG, ">>> SPEAKING: '$message'")
        mainHandler.post {
            val speakResult = tts?.speak(message, queueMode, audioParams, "feedback_${System.currentTimeMillis()}")
            Log.d(TAG, "TTS speak() returned: $speakResult (0 = SUCCESS)")
        }
        
        lastSpokenMessage = message
        lastSpeakTimeMs = currentTime
    }
    
    /**
     * Speak feedback based on FeedbackType.
     * Only speaks if feedback type changed (to avoid repeating).
     */
    fun speakFeedback(feedbackType: FeedbackType) {
        Log.d(TAG, "speakFeedback() called: $feedbackType, last=$lastFeedbackType")
        
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, skipping feedback")
            return
        }
        
        // Don't repeat the same feedback
        if (feedbackType == lastFeedbackType) {
            return
        }
        
        // Skip NONE and READY feedback types (too frequent / not actionable)
        if (feedbackType == FeedbackType.NONE || feedbackType == FeedbackType.READY) {
            return
        }
        
        lastFeedbackType = feedbackType
        
        val message = getFeedbackMessage(feedbackType)
        
        if (message != null) {
            Log.d(TAG, "Feedback: $feedbackType -> '$message'")
            speak(message, priority = feedbackType == FeedbackType.GOOD_FORM)
        }
    }
    
    private fun getFeedbackMessage(feedbackType: FeedbackType): String? {
        return when (feedbackType) {
            // Squat feedback
            FeedbackType.BEND_FORWARD -> "Lean forward"
            FeedbackType.BEND_BACKWARDS -> "Straighten your back"
            FeedbackType.LOWER_HIPS -> "Go deeper"
            FeedbackType.KNEE_OVER_TOES -> "Knees over toes"
            FeedbackType.DEEP_SQUAT -> "Too deep"
            FeedbackType.GOOD_FORM -> "Good form"
            
            // Push-up feedback
            FeedbackType.HIPS_TOO_HIGH -> "Lower your hips"
            FeedbackType.HIPS_TOO_LOW -> "Raise your hips"
            FeedbackType.ARMS_NOT_LOCKED -> "Extend arms fully"
            FeedbackType.ELBOWS_FLARED -> "Tuck elbows in"
            
            // Deadlift feedback
            FeedbackType.ROUND_BACK -> "Keep back straight"
            FeedbackType.HIPS_TOO_EARLY -> "Hips rising too fast"
            FeedbackType.BAR_AWAY -> "Keep bar close"
            
            // Bench/Row feedback
            FeedbackType.ELBOWS_TOO_WIDE -> "Tuck elbows more"
            FeedbackType.INCOMPLETE_LOCKOUT -> "Lock out fully"
            FeedbackType.MOMENTUM -> "Control the movement"
            
            // General
            FeedbackType.FRONTAL_WARNING -> "Turn to side view"
            FeedbackType.POSITION_BODY -> "Position full body in frame"
            FeedbackType.REP_COMPLETE -> "Good rep"
            
            // Skip these
            FeedbackType.NONE, FeedbackType.READY -> null
        }
    }
    
    /**
     * Announce rep count when it changes.
     */
    fun announceRep(repCount: Int, isCorrect: Boolean) {
        Log.d(TAG, "announceRep() called: count=$repCount, lastCount=$lastRepCount")
        
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, skipping rep announcement")
            return
        }
        if (repCount == lastRepCount || repCount == 0) {
            return
        }
        
        lastRepCount = repCount
        
        Log.d(TAG, ">>> Announcing rep: $repCount")
        speak("$repCount", priority = true)
    }
    
    /**
     * Reset feedback tracking (for new workout).
     */
    fun reset() {
        lastFeedbackType = null
        lastRepCount = 0
        lastSpokenMessage = null
        Log.d(TAG, "VoiceFeedbackManager reset")
    }
    
    /**
     * Shutdown TTS engine.
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down TTS")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
    
    companion object {
        private const val TAG = "VoiceFeedbackManager"
    }
}
