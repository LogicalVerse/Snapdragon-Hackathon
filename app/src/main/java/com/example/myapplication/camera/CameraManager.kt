package com.example.myapplication.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.pose.AnalysisResult
import com.example.myapplication.pose.PoseAnalyzer
import com.example.myapplication.pose.PoseLandmark
import com.example.myapplication.pose.PoseLandmarkerHelper
import com.example.myapplication.pose.WorkoutSummary
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(private val context: Context) : PoseLandmarkerHelper.LandmarkerListener {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // MediaPipe Pose Landmarker
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private val poseAnalyzer = PoseAnalyzer()
    var isPoseEstimationEnabled = false
        private set
    
    var isUsingFrontCamera = false
        private set
    
    var isRecording = false
        private set
    
    var onRecordingStateChanged: ((Boolean) -> Unit)? = null
    var onRecordingSaved: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onPoseDetected: ((List<PoseLandmark>) -> Unit)? = null
    var onAnalysisResult: ((AnalysisResult) -> Unit)? = null
    
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean = false
    ) {
        isUsingFrontCamera = useFrontCamera
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(lifecycleOwner, previewView)
        }, ContextCompat.getMainExecutor(context))
    }
    
    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProvider = cameraProvider ?: return
        
        // Select camera
        val cameraSelector = if (isUsingFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        // Preview
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Video capture
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)
        
        // Image analysis for pose estimation
        @Suppress("DEPRECATION")
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    if (isPoseEstimationEnabled) {
                        processFrame(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }
            }
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            onError?.invoke("Failed to bind camera: ${e.message}")
        }
    }
    
    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        isUsingFrontCamera = !isUsingFrontCamera
        bindCameraUseCases(lifecycleOwner, previewView)
    }
    
    fun startRecording() {
        val videoCapture = videoCapture ?: return
        
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Formly")
            }
        }
        
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
        
        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutput)
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        onRecordingStateChanged?.invoke(true)
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        onRecordingStateChanged?.invoke(false)
                        
                        if (!recordEvent.hasError()) {
                            val savedUri = recordEvent.outputResults.outputUri
                            onRecordingSaved?.invoke(savedUri.toString())
                        } else {
                            onError?.invoke("Recording failed: ${recordEvent.error}")
                        }
                    }
                }
            }
    }
    
    fun stopRecording() {
        recording?.stop()
        recording = null
    }
    
    /**
     * Start pose estimation - initialize MediaPipe PoseLandmarker
     */
    fun startPoseEstimation() {
        if (poseLandmarkerHelper == null) {
            poseLandmarkerHelper = PoseLandmarkerHelper(context, this)
        }
        isPoseEstimationEnabled = true
        Log.d(TAG, "Pose estimation started")
    }
    
    /**
     * Stop pose estimation
     */
    fun stopPoseEstimation() {
        isPoseEstimationEnabled = false
        Log.d(TAG, "Pose estimation stopped")
    }
    
    /**
     * Process camera frame for pose estimation using MediaPipe
     */
    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                val frameTime = SystemClock.uptimeMillis()
                poseLandmarkerHelper?.detectAsync(bitmap, frameTime)
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing failed", e)
        } finally {
            imageProxy.close()
        }
    }
    
    /**
     * Convert ImageProxy (YUV_420_888) to Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 80, out)
            val imageBytes = out.toByteArray()
            
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            // Rotate bitmap based on image rotation
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert image", e)
            null
        }
    }
    
    // PoseLandmarkerHelper.LandmarkerListener implementation
    override fun onResults(landmarks: List<PoseLandmark>, inferenceTime: Long) {
        // Run pose analysis
        val analysisResult = poseAnalyzer.analyzePose(landmarks)
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onPoseDetected?.invoke(landmarks)
            onAnalysisResult?.invoke(analysisResult)
        }
    }
    
    /**
     * Get workout summary for pause screen
     */
    fun getWorkoutSummary(): WorkoutSummary = poseAnalyzer.getWorkoutSummary()
    
    /**
     * Reset analyzer for new workout
     */
    fun resetAnalyzer() = poseAnalyzer.reset()
    
    override fun onError(error: String) {
        Log.e(TAG, "Pose detection error: $error")
        onError?.invoke(error)
    }
    
    fun shutdown() {
        stopRecording()
        stopPoseEstimation()
        poseLandmarkerHelper?.close()
        poseLandmarkerHelper = null
        cameraExecutor.shutdown()
        analysisExecutor.shutdown()
    }
    
    companion object {
        private const val TAG = "CameraManager"
    }
}
