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
 * Voice feedback manager for hands-free workout coaching.
 * 
 * Provides specific voice commands for different form issues:
 * - "Good form" for correct pose
 * - "Keep your back straight" for back issues
 * - "Go deeper" for depth issues
 * - "Side pose" for camera angle issues
 * - Rep numbers: "1", "2", "3", etc.
 */
class VoiceFeedbackManager(context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var lastSpokenMessage: String? = null
    private var lastSpeakTimeMs = 0L
    
    // Main thread handler for TTS calls
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Queue for messages that arrive before TTS is initialized
    private val pendingMessages = mutableListOf<String>()
    
    // Minimum interval between form feedback (1.5 seconds)
    private val formFeedbackIntervalMs = 1500L
    
    // Track state
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
                    tts?.setSpeechRate(1.15f)  // Faster speech
                    tts?.setPitch(1.0f)
                    isInitialized = true
                    Log.d(TAG, "TTS initialized successfully")
                    
                    // Speak any pending messages that were queued before init
                    if (pendingMessages.isNotEmpty()) {
                        Log.d(TAG, "Speaking ${pendingMessages.size} queued messages")
                        pendingMessages.forEach { msg ->
                            speakInternal(msg, false)
                        }
                        pendingMessages.clear()
                    }
                    
                    // Speak ready message
                    speakInternal("Ready", false)
                } else {
                    Log.e(TAG, "TTS initialization failed with status: $status")
                }
            }
        }
    }
    
    /**
     * Speak a message. If TTS is not yet initialized, queue the message for later.
     */
    private fun speak(message: String, priority: Boolean = false) {
        Log.d(TAG, "speak() called: '$message', initialized=$isInitialized")
        
        if (message.isBlank()) {
            Log.w(TAG, "Message blank, ignoring")
            return
        }
        
        // If TTS not ready yet, queue the message for when it initializes
        if (!isInitialized) {
            Log.d(TAG, "TTS not ready, queuing message: '$message'")
            pendingMessages.add(message)
            return
        }
        
        speakInternal(message, priority)
    }
    
    /**
     * Internal speak function that actually calls TTS.
     * Only call this when isInitialized is true.
     */
    private fun speakInternal(message: String, priority: Boolean) {
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
        lastSpeakTimeMs = System.currentTimeMillis()
    }
    
    /**
     * Speak feedback based on FeedbackType.
     * Uses specific voice commands for each issue type.
     */
    fun speakFeedback(feedbackType: FeedbackType) {
        Log.d(TAG, "speakFeedback() called: $feedbackType")
        
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, skipping feedback")
            return
        }
        
        // Skip these - no action needed
        if (feedbackType == FeedbackType.NONE || feedbackType == FeedbackType.READY) {
            // Reset tracking when no feedback
            if (feedbackType == FeedbackType.NONE) {
                lastFeedbackType = null
            }
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFeedback = currentTime - lastSpeakTimeMs
        
        // Only speak if:
        // 1. Feedback type changed
        // 2. OR enough time has passed since last feedback
        if (feedbackType != lastFeedbackType || timeSinceLastFeedback >= formFeedbackIntervalMs) {
            lastFeedbackType = feedbackType
            
            val message = getVoiceCommand(feedbackType)
            if (message != null) {
                val isPriority = feedbackType == FeedbackType.GOOD_FORM || feedbackType == FeedbackType.REP_COMPLETE
                Log.d(TAG, ">>> Speaking: $message")
                speak(message, priority = isPriority)
            }
        }
    }
    
    /**
     * Get specific voice command for each feedback type.
     * These are short, clear commands for workout coaching.
     */
    private fun getVoiceCommand(feedbackType: FeedbackType): String? {
        return when (feedbackType) {
            // Good form feedback
            FeedbackType.GOOD_FORM -> "Good form"
            FeedbackType.REP_COMPLETE -> "Good rep"
            
            // Squat feedback
            FeedbackType.BEND_FORWARD -> "Lean forward"
            FeedbackType.BEND_BACKWARDS -> "Keep your back straight"
            FeedbackType.LOWER_HIPS -> "Go deeper"
            FeedbackType.KNEE_OVER_TOES -> "Watch your knees"
            FeedbackType.DEEP_SQUAT -> "Too deep"
            
            // Push-up feedback
            FeedbackType.HIPS_TOO_HIGH -> "Lower your hips"
            FeedbackType.HIPS_TOO_LOW -> "Raise your hips"
            FeedbackType.ARMS_NOT_LOCKED -> "Extend your arms"
            FeedbackType.ELBOWS_FLARED -> "Tuck your elbows"
            
            // Deadlift feedback
            FeedbackType.ROUND_BACK -> "Keep your back straight"
            FeedbackType.HIPS_TOO_EARLY -> "Hips too fast"
            FeedbackType.BAR_AWAY -> "Keep bar close"
            
            // Bench/Row feedback
            FeedbackType.ELBOWS_TOO_WIDE -> "Tuck elbows in"
            FeedbackType.INCOMPLETE_LOCKOUT -> "Lock out fully"
            FeedbackType.MOMENTUM -> "Control the weight"
            
            // Camera/Position feedback
            FeedbackType.FRONTAL_WARNING -> "Side pose please"
            FeedbackType.POSITION_BODY -> "Show full body"
            
            // Skip these
            FeedbackType.NONE, FeedbackType.READY -> null
        }
    }
    
    /**
     * Announce rep count when it changes.
     * Just says the number: "1", "2", "3", etc.
     */
    fun announceRep(repCount: Int, isCorrect: Boolean = true) {
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
        lastSpeakTimeMs = 0L
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
