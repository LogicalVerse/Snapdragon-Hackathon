package com.example.myapplication.pose

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Helper class for MediaPipe Pose Landmarker
 * Provides real-time pose detection with 33 body landmarks
 */
class PoseLandmarkerHelper(
    private val context: Context,
    private val poseLandmarkerListener: LandmarkerListener? = null
) {
    companion object {
        private const val TAG = "PoseLandmarkerHelper"
        private const val MODEL_NAME = "pose_landmarker_lite.task"
        private const val MIN_POSE_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_POSE_TRACKING_CONFIDENCE = 0.5f
        private const val MIN_POSE_PRESENCE_CONFIDENCE = 0.5f
    }

    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupPoseLandmarker()
    }

    /**
     * Initialize the PoseLandmarker with LIVE_STREAM mode
     */
    private fun setupPoseLandmarker() {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath(MODEL_NAME)
                .setDelegate(Delegate.CPU)

            val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMinPoseDetectionConfidence(MIN_POSE_DETECTION_CONFIDENCE)
                .setMinTrackingConfidence(MIN_POSE_TRACKING_CONFIDENCE)
                .setMinPosePresenceConfidence(MIN_POSE_PRESENCE_CONFIDENCE)
                .setNumPoses(1)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)

            val options = optionsBuilder.build()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            
            Log.d(TAG, "PoseLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PoseLandmarker", e)
            poseLandmarkerListener?.onError("Failed to initialize pose detection: ${e.message}")
        }
    }

    /**
     * Detect pose landmarks in a bitmap (for live stream)
     */
    fun detectAsync(bitmap: Bitmap, frameTime: Long) {
        if (poseLandmarker == null) {
            Log.w(TAG, "PoseLandmarker not initialized")
            return
        }

        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            poseLandmarker?.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed", e)
        }
    }

    /**
     * Handle detection results from live stream
     */
    private fun returnLivestreamResult(result: PoseLandmarkerResult, input: com.google.mediapipe.framework.image.MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        
        if (result.landmarks().isNotEmpty()) {
            // Get raw MediaPipe landmarks for server communication
            val rawLandmarks: List<NormalizedLandmark> = result.landmarks()[0]
            
            // Convert to our PoseLandmark format for local processing
            val landmarks = rawLandmarks.map { landmark ->
                PoseLandmark(
                    x = landmark.x(),
                    y = landmark.y(),
                    z = landmark.z(),
                    visibility = landmark.visibility().orElse(1.0f)
                )
            }
            
            poseLandmarkerListener?.onResults(landmarks, rawLandmarks, finishTimeMs)
        }
    }

    /**
     * Handle errors from live stream
     */
    private fun returnLivestreamError(error: RuntimeException) {
        Log.e(TAG, "Pose detection error: ${error.message}")
        poseLandmarkerListener?.onError(error.message ?: "Unknown error")
    }

    /**
     * Release resources
     */
    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        Log.d(TAG, "PoseLandmarker released")
    }

    /**
     * Listener interface for pose detection results
     */
    interface LandmarkerListener {
        /**
         * Called when pose landmarks are detected
         * @param landmarks Converted PoseLandmark list for local processing
         * @param rawLandmarks Raw MediaPipe NormalizedLandmark list for server communication
         * @param inferenceTime Time taken for inference in milliseconds
         */
        fun onResults(
            landmarks: List<PoseLandmark>, 
            rawLandmarks: List<NormalizedLandmark>,
            inferenceTime: Long
        )
        fun onError(error: String)
    }
}

