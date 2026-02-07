package com.example.myapplication.data

// Connection states
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String = "Desktop App") : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

// Camera states
data class CameraState(
    val isFrontCamera: Boolean = false,
    val isFlashOn: Boolean = false,
    val isGridVisible: Boolean = false,
    val fps: Int = 30,
    val quality: String = "HD" // HD, FHD, 4K
) {
    val qualityColor: String
        get() = when {
            fps >= 30 -> "green"
            fps >= 15 -> "yellow"
            else -> "red"
        }
}

// Model for positioning guide
data class PositioningGuide(
    val title: String = "Position Your Phone",
    val checklist: List<String> = listOf(
        "Place phone 6-8 feet away",
        "Position at hip height",
        "Ensure full body visible",
        "Stand phone vertically"
    )
)
