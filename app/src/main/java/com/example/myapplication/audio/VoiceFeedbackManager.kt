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
 * Simplified voice feedback manager for hands-free workout coaching.
 * 
 * Speaks only:
 * - "Good form" when doing correct pose
 * - "Correct your form" when doing incorrect pose  
 * - Rep numbers: "1", "2", "3", etc.
 */
class VoiceFeedbackManager(context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var lastSpokenMessage: String? = null
    private var lastSpeakTimeMs = 0L
    
    // Main thread handler for TTS calls
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Minimum interval between form feedback to avoid spam (3 seconds)
    private val formFeedbackIntervalMs = 3000L
    
    // Track state
    private var isCurrentFormGood = true  // Assume good until proven otherwise
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
                    tts?.setSpeechRate(1.1f)  // Slightly faster
                    tts?.setPitch(1.0f)
                    isInitialized = true
                    Log.d(TAG, "TTS initialized successfully")
                    
                    // Speak ready message
                    speak("Ready")
                } else {
                    Log.e(TAG, "TTS initialization failed with status: $status")
                }
            }
        }
    }
    
    /**
     * Speak a message.
     */
    private fun speak(message: String, priority: Boolean = false) {
        Log.d(TAG, "speak() called: '$message', initialized=$isInitialized")
        
        if (!isInitialized || message.isBlank()) {
            Log.w(TAG, "TTS not ready or message blank")
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
        lastSpeakTimeMs = System.currentTimeMillis()
    }
    
    /**
     * Simplified feedback based on FeedbackType.
     * Only says "Good form" or "Correct your form"
     */
    fun speakFeedback(feedbackType: FeedbackType) {
        Log.d(TAG, "speakFeedback() called: $feedbackType")
        
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, skipping feedback")
            return
        }
        
        // Skip these - no action needed
        if (feedbackType == FeedbackType.NONE || feedbackType == FeedbackType.READY) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFeedback = currentTime - lastSpeakTimeMs
        
        // Determine if current form is good or bad
        val isGoodForm = feedbackType == FeedbackType.GOOD_FORM || feedbackType == FeedbackType.REP_COMPLETE
        
        // Only speak if:
        // 1. Form state changed (good -> bad or bad -> good)
        // 2. OR enough time has passed since last feedback
        if (isGoodForm != isCurrentFormGood || timeSinceLastFeedback >= formFeedbackIntervalMs) {
            isCurrentFormGood = isGoodForm
            
            if (isGoodForm) {
                Log.d(TAG, ">>> Speaking: Good form")
                speak("Good form", priority = true)
            } else {
                Log.d(TAG, ">>> Speaking: Correct your form")
                speak("Correct your form", priority = false)
            }
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
        isCurrentFormGood = true
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
