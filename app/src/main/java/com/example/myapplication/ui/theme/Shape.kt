package com.example.myapplication.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val FormlyShapes = Shapes(
    // Small - Chips, small buttons
    small = RoundedCornerShape(8.dp),
    
    // Medium - Cards, input fields
    medium = RoundedCornerShape(16.dp),
    
    // Large - Bottom sheets, dialogs
    large = RoundedCornerShape(24.dp)
)

// Additional shape definitions for extra-large elements
val ExtraLargeShape = RoundedCornerShape(32.dp)
val SmallShape = RoundedCornerShape(8.dp)
val MediumShape = RoundedCornerShape(16.dp)
val LargeShape = RoundedCornerShape(24.dp)

// Corner radii values for direct use
object CornerRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
