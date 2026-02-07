package com.example.myapplication.ui.components

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.camera.CameraManager
import com.example.myapplication.ui.theme.DeepDark
import com.example.myapplication.ui.theme.ElectricGreen
import com.example.myapplication.ui.theme.ElevatedDark

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraManager: CameraManager,
    onSwitchCamera: () -> Unit = {},
    showSwitchButton: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    
    DisposableEffect(cameraManager, cameraManager.isUsingFrontCamera) {
        cameraManager.startCamera(lifecycleOwner, previewView, cameraManager.isUsingFrontCamera)
        
        onDispose {
            // Camera will be cleaned up when CameraManager.shutdown() is called
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Camera switch button
        if (showSwitchButton) {
            IconButton(
                onClick = {
                    cameraManager.switchCamera(lifecycleOwner, previewView)
                    onSwitchCamera()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ElevatedDark.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Switch Camera",
                    tint = ElectricGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
