package com.example.myapplication.ui.screens

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.data.SampleExercises
import com.example.myapplication.ui.theme.FormlyTheme
import com.example.myapplication.ui.theme.DeepDark

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=1440px,height=3088px,dpi=512",
    name = "Samsung Galaxy S25 Ultra"
)
@Composable
fun HomeScreenPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            HomeScreen(
                exercises = SampleExercises.all
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1440px,height=600px,dpi=512"
)
@Composable
fun ExerciseCardPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            ExerciseCard(
                exercise = SampleExercises.squats
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1440px,height=200px,dpi=512"
)
@Composable
fun WelcomeBannerPreview() {
    FormlyTheme {
        Surface(color = DeepDark) {
            WelcomeBanner()
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1440px,height=100px,dpi=512"
)
@Composable
fun TopBarPreview() {
    FormlyTheme {
        FormlyTopBar()
    }
}
