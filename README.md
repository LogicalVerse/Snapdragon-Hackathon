# 🏋️ Formly: AI-Powered Real-Time Fitness Coach


> **Built for the Snapdragon Hackathon** - Transform your Snapdragon-powered device into an intelligent personal trainer with real-time pose detection and AI feedback.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2031+-green.svg)](https://developer.android.com)
[![Gemini](https://img.shields.io/badge/Gemini-3%20Flash-orange.svg)](https://ai.google.dev)
[![License](https://img.shields.io/badge/AGPL-3.0-License.svg)](LICENSE)

---

## ✨ Features

### 🎯 Real-Time Pose Detection
- **On-device ML**: MediaPipe pose detection optimized for Snapdragon NPU
- **33 landmark tracking**: Full body pose analysis at 30+ FPS
- **Side detection**: Automatic left/right side recognition

### 🤖 AI-Powered Form Analysis
- **Gemini 3 Flash Integration**: Intelligent workout analysis using Google's latest AI
- **Dual comparison**: Your form vs. good AND bad reference frames
- **Structured feedback**: `✓ Good` / `✗ Issue` / `💡 Tip` format

### 🔊 Hands-Free Voice Coaching
- **0.8s response time**: Quick, responsive audio feedback
- **Specific commands**: "Back straight", "Go deeper", "Knees out"
- **Rep counting**: Automatic rep detection with audio announcement

### 📊 Workout Analytics
- **Form scoring**: 0-100% accuracy for each rep
- **Issue tracking**: Most common form problems highlighted
- **Streak heatmap**: Visual workout history on home screen

---

## 🏗️ Architecture

```
app/
├── MainActivity.kt              # App entry point & navigation
├── audio/
│   └── VoiceFeedbackManager.kt  # TTS voice coaching
├── camera/
│   └── CameraManager.kt         # Camera preview & recording
├── data/
│   ├── Exercise.kt              # Exercise data models
│   ├── ExerciseData.kt          # Exercise definitions
│   └── WorkoutPreferences.kt    # SharedPrefs for history
├── network/
│   ├── GeminiApiService.kt      # Retrofit API interface
│   ├── GeminiModels.kt          # Request/Response models
│   └── GeminiRepository.kt      # AI analysis logic
├── pose/
│   ├── ExerciseTrainer.kt       # Base trainer class
│   ├── SquatTrainer.kt          # Squat form detection
│   ├── PushUpTrainer.kt         # Push-up form detection
│   └── PoseLandmarkerHelper.kt  # MediaPipe integration
├── ui/
│   ├── screens/                 # Jetpack Compose screens
│   ├── components/              # Reusable UI components
│   └── theme/                   # Material 3 theming
└── video/
    └── VideoFrameExtractor.kt   # Frame extraction for AI
```

---

## 🚀 Quick Start

### Prerequisites
- Android Studio Hedgehog (2023.1+)
- JDK 17
- Android device with camera (Snapdragon recommended)

### Setup

```bash
# Clone the repository
git clone https://github.com/LogicalVerse/Snapdragon-Hackathon.git
cd Snapdragon-Hackathon

# Open in Android Studio and sync Gradle

# Build and run
./gradlew assembleDebug
```

### API Configuration

The Gemini API key is configured in `GeminiRepository.kt`. For production, move to:
```kotlin
// local.properties (not committed to git)
GEMINI_API_KEY=your_api_key_here
```

---

## 📱 Supported Exercises

| Exercise | Detection | AI Analysis | Voice Feedback |
|----------|-----------|-------------|----------------|
| Squats | ✅ Full | ✅ Full | ✅ Full |
| Bicep Curls | ✅ Full | ✅ Full | ✅ Full |

---

## 🔧 Key Technologies

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **UI** | Jetpack Compose | Modern declarative UI |
| **ML** | MediaPipe | On-device pose detection |
| **AI** | Gemini 3 Flash | Form analysis & feedback |
| **Audio** | Android TTS | Voice coaching |
| **Camera** | CameraX | Video capture |
| **Network** | Retrofit + OkHttp | API communication |

---

## 📂 Reference Frames

Good and bad form reference images are stored in:
```
app/src/main/assets/
├── squat_good_form/     # 6 correct technique images
├── squat_bad_form/      # 6 common mistake images
└── pose_landmarker_lite.task  # MediaPipe model
```

To add new exercises:
1. Add 6 good form images to `assets/{exercise}_good_form/`
2. Add 6 bad form images to `assets/{exercise}_bad_form/`
3. Update `VideoFrameExtractor` to load the new folders

---

## 🎨 UI/UX Design

- **Dark theme**: Optimized for gym lighting conditions
- **Immersive mode**: Hidden navigation bar during workouts
- **Large touch targets**: Easy interaction while exercising
- **High contrast**: Form feedback visible at a glance

---

## 📈 Performance

| Metric | Value |
|--------|-------|
| Pose detection | 30+ FPS |
| Voice feedback delay | 0.8 seconds |
| AI analysis time | ~3-5 seconds |
| Memory usage | < 300MB |
| APK size | ~25MB |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## Team Members

- Ayush Verma (av3334@columbia.edu)
- Madhav Tibrewal (madhavtibrewal92@gmail.com)
- Sanskriti Bansal (sb5215@columbia.edu)
- Sriya Rallabandi (sriyarallabandi@gmail.com)
- Tejal Bedmutha (tbedmutha_b19@tx.vjti.ac.in)

---

## 📄 License

This project was created for the **Snapdragon Hackathon 2026**.

---

## 🙏 Acknowledgments

- [Google Gemini](https://ai.google.dev/) - AI form analysis
- [MediaPipe](https://developers.google.com/mediapipe) - Pose detection
- [Qualcomm Snapdragon](https://www.qualcomm.com/snapdragon) - NPU acceleration
- [LearnOpenCV](https://learnopencv.com/) - AI Fitness Trainer reference

---

<p align="center">
  Built with ❤️ for the Snapdragon Hackathon<br>
  <strong>Your AI-Powered Personal Trainer</strong>
</p>
