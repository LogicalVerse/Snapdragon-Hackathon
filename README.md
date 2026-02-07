# Formly: Multi-Device Real-Time AI Workout Form Analysis Assistant

An intelligent fitness companion built for the Snapdragon Hackathon that combines real-time exercise tracking with AI-powered form analysis. Whether you're working out at the gym or recovering through physiotherapy, this app provides instant feedback and guidance using advanced computer vision and AI.

## 🚀 Project Overview

This application transforms Snapdragon-powered Android device into a personal trainer and physiotherapy assistant. Using on-device pose detection and Google's Gemini 3 Flash Preview API, the app:

- **Analyzes Exercise Form**: Real-time tracking of body movements during workouts and physiotherapy exercises
- **Provides Instant Feedback**: AI-generated analysis of your exercise technique, posture, and execution
- **Supports Multiple Use Cases**: 
  - Gym workouts (squats, push-ups, lunges, etc.)
  - Physiotherapy rehabilitation exercises
  - Home fitness routines
- **Leverages Snapdragon AI**: Optimized pose detection and inference on Snapdragon's NPU for smooth, responsive tracking
- **Gemini-Powered Insights**: Detailed exercise analysis and recommendations generated using Gemini 3 Flash Preview API

## 📋 Features

- **Real-Time Pose Detection**: Advanced computer vision tracks your body movements during exercises
- **AI Exercise Analysis**: Gemini 3 Flash Preview API generates detailed feedback on form, posture, and technique
- **Dual Purpose Application**:
  - **Gym Assistant**: Track and analyze strength training exercises (squats, push-ups, deadlifts, etc.)
  - **Physiotherapy Coach**: Guide and monitor rehabilitation exercises with precise movement tracking
- **Snapdragon Optimization**: Leverages Snapdragon NPU for efficient on-device pose estimation
- **Instant Feedback**: Get immediate AI-powered recommendations to improve your form
- **Exercise Analysis**: Detailed analytics based on number of correct and incorrect forms in a rep
- **Privacy-Focused**: Video processing happens on-device; only pose data sent for analysis
- **Offline Capable**: Pose detection works without internet; AI analysis available when connected

## 🏗️ Architecture

```
Snapdragon-Hackathon/
├── app/                    # Android application (Kotlin)
│   ├── Pose Detection      # Real-time body tracking using Snapdragon NPU
│   ├── Camera Interface    # Video capture and processing
│   └── UI Components       # Exercise tracking interface
├── backend/                # Python backend service
│   ├── Gemini API Client   # Integration with Gemini 3 Flash Preview
│   └── Exercise Analytics  # Analysis and feedback generation
├── gradle/                 # Gradle wrapper files
├── .idea/                  # IntelliJ IDEA configuration
├── .vscode/                # VS Code configuration
├── build.gradle.kts        # Project-level build configuration
├── settings.gradle.kts     # Gradle settings
└── gradle.properties       # Gradle properties
```

## 💡 How It Works

1. **Camera Capture**: The app uses your device's camera to record your exercise
2. **Pose Detection**: Snapdragon NPU processes the video in real-time to detect body keypoints
3. **Movement Analysis**: Joint angles, movement patterns, and exercise metrics are calculated
4. **AI Analysis**: Pose data is sent to Gemini 3 Flash Preview API for intelligent analysis
5. **Feedback Generation**: Gemini provides detailed feedback on:
   - Form correctness
   - Common mistakes detected
   - Injury risk assessment
   - Improvement suggestions
   - Rep counting and quality
6. **Display Results**: User receives instant, actionable feedback on their device

### Use Cases

**🏋️ Gym Workouts**
- Track squats, deadlifts, bench press, and more
- Get form corrections to prevent injury
- Count reps automatically with quality assessment
- Compare your form against proper technique

**🏥 Physiotherapy**
- Monitor rehabilitation exercise execution
- Ensure proper movement patterns during recovery
- Track range of motion improvements
- Receive guidance on exercise progression

## 🛠️ Tech Stack

### Android App
- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL)
- **Platform**: Android (Snapdragon optimized)
- **IDE**: Android Studio / IntelliJ IDEA
- **AI/ML**: 
  - On-device pose detection (Snapdragon NPU accelerated)
  - Google Gemini 3 Flash Preview API for exercise analysis
  - MediaPipe for pose estimation

### Backend
- **Language**: Python
- **Purpose**: API integration, data processing, and exercise analytics
- **AI Integration**: Gemini 3 Flash Preview API client

## 📦 Prerequisites

### For Android Development
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK (API level as specified in build.gradle)
- Snapdragon-powered Android device (recommended for optimal pose detection)
- Device with camera support

### For Backend Development
- Python 3.8 or higher
- pip package manager
- Virtual environment (recommended)
- **Google AI Studio API Key** for Gemini 3 Flash Preview access
  - Get your API key from [Google AI Studio](https://makersuite.google.com/app/apikey)

## 🚦 Getting Started

### Clone the Repository

```bash
git clone https://github.com/LogicalVerse/Snapdragon-Hackathon.git
cd Snapdragon-Hackathon
```

### Android App Setup

1. Open the project in Android Studio
2. Sync Gradle files
3. Connect your Snapdragon-powered device or use an emulator
4. Build and run the app:
   ```bash
   ./gradlew assembleDebug
   ```

### Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Create and activate a virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Configure Gemini API:
   Create a `.env` file in the backend directory:
   ```env
   GEMINI_API_KEY=your_api_key_here
   ```

5. Run the backend server:
   ```bash
   python main.py  # or your entry point script
   ```

## 🔧 Build Commands

### Android
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

## 📱 Deployment

### Android APK
1. Generate a signed APK through Android Studio
2. Or use Gradle:
   ```bash
   ./gradlew assembleRelease
   ```
3. Find the APK in `app/build/outputs/apk/release/`

## 🎯 AI & Performance Optimization

### Performance Benefits
- **Fast Processing**: Snapdragon NPU handles pose detection locally
- **Smart Analysis**: Gemini provides human-like coaching feedback
- **Battery Efficient**: Optimized for mobile devices
- **Privacy-Preserving**: Video stays on device; only pose data sent to API

## 📄 License

This project is created for the Snapdragon Hackathon.

---

Built with ❤️ for the Snapdragon Hackathon | Your AI-Powered Personal Trainer & Physiotherapy Coach
