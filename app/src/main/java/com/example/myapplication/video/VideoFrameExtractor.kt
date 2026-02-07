package com.example.myapplication.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Extracts frames from a video file for AI analysis.
 * Uses aggressive compression to prevent memory issues.
 */
class VideoFrameExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoFrameExtractor"
        
        // Aggressive compression settings to prevent memory overflow
        private const val MAX_IMAGE_DIMENSION = 512  // Max width/height in pixels
        private const val JPEG_QUALITY = 50  // Lower quality for smaller size
        private const val MAX_BASE64_SIZE = 100_000  // ~100KB max per image
    }
    
    /**
     * Extract evenly distributed frames from a video.
     * Frames are resized and compressed to prevent memory issues.
     * 
     * @param videoUri URI of the video file
     * @param frameCount Number of frames to extract
     * @return List of base64-encoded JPEG frames
     */
    fun extractFrames(videoUri: String, frameCount: Int = 6): List<String> {
        val frames = mutableListOf<String>()
        var retriever: MediaMetadataRetriever? = null
        
        try {
            Log.d(TAG, "=== Starting frame extraction ===")
            Log.d(TAG, "Video URI: $videoUri")
            Log.d(TAG, "Requested frames: $frameCount")
            
            retriever = MediaMetadataRetriever()
            
            // Handle content:// URIs properly
            val uri = Uri.parse(videoUri)
            if (uri.scheme == "content") {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd == null) {
                    Log.e(TAG, "Failed to open file descriptor for: $videoUri")
                    return emptyList()
                }
                pfd.use { 
                    retriever.setDataSource(it.fileDescriptor)
                }
            } else {
                retriever.setDataSource(context, uri)
            }
            
            // Get video duration in microseconds
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val durationUs = durationMs * 1000
            
            Log.d(TAG, "Video duration: $durationMs ms")
            
            if (durationUs <= 0) {
                Log.e(TAG, "Invalid video duration: $durationMs ms")
                return emptyList()
            }
            
            // Calculate timestamps for evenly distributed frames
            val interval = durationUs / (frameCount + 1)
            Log.d(TAG, "Frame interval: ${interval / 1000} ms")
            
            for (i in 1..frameCount) {
                val timestampUs = interval * i
                
                try {
                    Log.d(TAG, "Extracting frame $i at ${timestampUs / 1000}ms...")
                    
                    val bitmap = retriever.getFrameAtTime(
                        timestampUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    
                    if (bitmap != null) {
                        Log.d(TAG, "Frame $i raw size: ${bitmap.width}x${bitmap.height}")
                        
                        val base64 = compressBitmapToBase64(bitmap)
                        bitmap.recycle()  // Immediately recycle to free memory
                        
                        if (base64 != null) {
                            frames.add(base64)
                            Log.d(TAG, "Frame $i compressed: ${base64.length} chars")
                        } else {
                            Log.w(TAG, "Frame $i compression failed")
                        }
                    } else {
                        Log.w(TAG, "Failed to extract frame $i at ${timestampUs / 1000}ms")
                    }
                    
                    // Force garbage collection between frames to avoid OOM
                    System.gc()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting frame $i: ${e.message}", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in extractFrames: ${e.message}", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing retriever: ${e.message}")
            }
        }
        
        Log.d(TAG, "=== Extraction complete: ${frames.size}/${frameCount} frames ===")
        return frames
    }
    
    /**
     * Load all professional squat frames from assets.
     * Frames are resized and compressed to prevent memory issues.
     */
    fun loadProfessionalFrames(): List<String> {
        val frames = mutableListOf<String>()
        val assetDir = "squat_professional_frames"
        
        try {
            Log.d(TAG, "=== Loading professional frames ===")
            
            val files = context.assets.list(assetDir) ?: emptyArray()
            Log.d(TAG, "Found ${files.size} files in $assetDir")
            
            // Sort files to ensure consistent ordering
            files.sorted().forEachIndexed { index, fileName ->
                try {
                    Log.d(TAG, "Loading professional frame ${index + 1}: $fileName")
                    
                    val base64 = loadAndCompressAsset("$assetDir/$fileName")
                    if (base64 != null) {
                        frames.add(base64)
                        Log.d(TAG, "Professional frame ${index + 1}: ${base64.length} chars")
                    } else {
                        Log.w(TAG, "Failed to load: $fileName")
                    }
                    
                    // Force GC between frames
                    System.gc()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading $fileName: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load professional frames: ${e.message}", e)
        }
        
        Log.d(TAG, "=== Loaded ${frames.size} professional frames ===")
        return frames
    }
    
    /**
     * Load asset, decode, resize, and compress to base64.
     */
    private fun loadAndCompressAsset(assetPath: String): String? {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                // Decode with inSampleSize to reduce memory usage
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                
                // First pass - get dimensions
                val tempStream = context.assets.open(assetPath)
                BitmapFactory.decodeStream(tempStream, null, options)
                tempStream.close()
                
                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                options.inJustDecodeBounds = false
                
                // Second pass - decode with sample size
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                
                if (bitmap != null) {
                    val result = compressBitmapToBase64(bitmap)
                    bitmap.recycle()
                    result
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load asset: $assetPath - ${e.message}")
            null
        }
    }
    
    /**
     * Resize and compress bitmap to base64 string.
     */
    private fun compressBitmapToBase64(originalBitmap: Bitmap): String? {
        return try {
            // Resize to max dimension
            val scaledBitmap = resizeBitmap(originalBitmap, MAX_IMAGE_DIMENSION)
            
            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            
            // Clean up scaled bitmap if it's different from original
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            
            val bytes = outputStream.toByteArray()
            outputStream.close()
            
            Log.d(TAG, "Compressed image: ${bytes.size} bytes")
            
            // Check if still too large
            if (bytes.size > MAX_BASE64_SIZE * 0.75) {  // base64 adds ~33% overhead
                Log.w(TAG, "Image still large: ${bytes.size} bytes, may cause issues")
            }
            
            Base64.encodeToString(bytes, Base64.NO_WRAP)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing bitmap: ${e.message}")
            null
        }
    }
    
    /**
     * Resize bitmap to fit within maxDimension while maintaining aspect ratio.
     */
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val scale = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        Log.d(TAG, "Resizing: ${width}x${height} -> ${newWidth}x${newHeight}")
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Calculate sample size for BitmapFactory to reduce memory usage.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}
