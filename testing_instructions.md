# Testing, Usage & Troubleshooting Guide

## 📱 Testing the App

### Pre-Testing Checklist

Before you begin testing, ensure you have:

- [ ] Snapdragon-powered Android device (recommended) or emulator
- [ ] Camera permissions granted to the app
- [ ] Internet connection (for Gemini API analysis)
- [ ] Backend server running with valid Gemini API key
- [ ] Adequate lighting in your testing environment
- [ ] Sufficient space to perform exercises (2-3 meters from camera)

### Test Scenarios

#### 1. Basic Functionality Test

**Objective**: Verify core app features work correctly

**Steps**:
1. Launch the app
2. Grant camera and storage permissions when prompted
3. Navigate through all main screens (Home, Exercise Selection, History)
4. Verify UI elements load correctly
5. Check that exercise categories display properly

**Expected Result**: All screens load without crashes, permissions granted successfully

---

#### 2. Pose Detection Test

**Objective**: Confirm real-time pose tracking works

**Steps**:
1. Select any exercise from the list
2. Position yourself in front of the camera (full body visible)
3. Observe the skeleton overlay on your body
4. Move your arms and legs slowly
5. Check if keypoints follow your movements

**Expected Result**: 
- Skeleton overlay appears on your body
- Keypoints track smoothly with minimal lag
- All major joints are detected (shoulders, elbows, wrists, hips, knees, ankles)

---

#### 3. Exercise Analysis Test (Squats)

**Objective**: Test end-to-end exercise analysis

**Steps**:
1. Select "Squats" from the exercise list
2. Tap "Start Exercise"
3. Position yourself so your full body is visible
4. Perform 5 squats with proper form
5. Tap "Stop" or wait for auto-stop
6. Wait for AI analysis to complete

**Expected Result**:
- Rep counter increases with each squat
- Analysis is generated within 5-10 seconds
- Feedback includes form assessment and suggestions

---

#### 4. Physiotherapy Mode Test

**Objective**: Verify physiotherapy exercise tracking

**Steps**:
1. Navigate to "Physiotherapy" section
2. Select a rehabilitation exercise (e.g., "Shoulder Range of Motion")
3. Start the exercise
4. Perform slow, controlled movements
5. Complete the set and review feedback

**Expected Result**:
- App provides detailed movement analysis
- Range of motion metrics displayed
- Specific guidance for rehabilitation

---

#### 5. Gemini API Integration Test

**Objective**: Confirm AI analysis is working

**Steps**:
1. Complete any exercise
2. Check network logs for API calls
3. Verify response from Gemini API
4. Review the generated feedback quality

**Expected Result**:
- API call succeeds (HTTP 200)
- Feedback is relevant and actionable
- Response time < 10 seconds

---

#### 6. History & Progress Test

**Objective**: Test data persistence and history tracking

**Steps**:
1. Complete 3 different exercises
2. Navigate to History screen
3. Verify all exercises are logged
4. Tap on a previous exercise to view details
5. Check if analysis is preserved

**Expected Result**:
- All exercises appear in chronological order
- Details include date, time, reps, and analysis
- Data persists after app restart

---

#### 7. Edge Case Testing

**Test Case A: Poor Lighting**
- Test in dim lighting conditions
- Verify app shows warning or adjusts detection

**Test Case B: Partial Body Visibility**
- Start exercise with only upper body visible
- Check if app prompts user to adjust position

**Test Case C: Multiple People in Frame**
- Have 2+ people in camera view
- Verify app focuses on primary user or shows warning

**Test Case D: Network Interruption**
- Complete exercise, then disable internet before analysis
- Check error handling and retry mechanism

**Test Case E: Rapid Movements**
- Perform exercise very quickly
- Verify pose detection keeps up or shows appropriate warning

---

## 🎯 How to Use the App

### First-Time Setup

1. **Install the App**
   - Install the APK on your Snapdragon device
   - Or build from source using Android Studio

2. **Grant Permissions**
   - Camera access (required for pose detection)
   - Storage access (for saving exercise history)
   - Internet access (for AI analysis)

3. **Configure Backend (if self-hosting)**
   - Update API endpoint in app settings
   - Verify connection to backend server

### Using the Gym Assistant

#### Starting a Workout Session

1. **Select Exercise Type**
   ```
   Home Screen → Gym Workouts → Select Exercise (e.g., Squats)
   ```

2. **Position Your Camera**
   - Place device 2-3 meters away
   - Ensure full body is visible in frame
   - Use landscape mode for best results
   - Ensure good lighting

3. **Start Tracking**
   - Tap "Start Exercise" button
   - Wait for countdown (3-2-1)
   - Begin performing the exercise
   - Watch the skeleton overlay for real-time feedback

4. **During Exercise**
   - Rep counter appears on screen
   - Keypoints should track your movements
   - Maintain visibility of all body parts
   - Perform with controlled movements

5. **Complete and Review**
   - Tap "Stop" when finished OR
   - App auto-stops after configured time/reps
   - Wait for AI analysis (5-10 seconds)
   - Review detailed feedback

#### Understanding the Feedback

Your AI analysis includes:

- **✅ Form Assessment**: Overall form rating (Good/Fair/Needs Improvement)
- **📊 Metrics**: Reps completed, time taken, quality score
- **⚠️ Common Mistakes**: Specific issues detected (e.g., "Knees extending past toes")
- **💡 Suggestions**: Actionable tips to improve form
- **🎯 Next Steps**: Progressive training recommendations

### Using the Physiotherapy Mode

#### Starting a Rehabilitation Session

1. **Select Physiotherapy**
   ```
   Home Screen → Physiotherapy → Choose Exercise
   ```

2. **Read Exercise Instructions**
   - Review the demonstration
   - Understand the target movement
   - Note any precautions

3. **Perform the Exercise**
   - Follow the guided movements
   - Move slowly and deliberately
   - Focus on proper form over speed
   - Stay within comfortable range

4. **Review Progress**
   - Check range of motion metrics
   - Compare with previous sessions
   - Note improvements or concerns

#### Tracking Recovery Progress

1. Navigate to "Progress" tab
2. View metrics over time:
   - Range of motion improvements
   - Pain levels (if logged)
   - Exercise completion rates
   - Consistency tracking

### Best Practices for Accurate Analysis

#### Camera Setup
- **Distance**: 2-3 meters from camera
- **Angle**: Slight downward angle, capturing full body
- **Height**: Camera at chest level for most exercises
- **Orientation**: Landscape mode recommended

#### Environment
- **Lighting**: Bright, even lighting (avoid backlighting)
- **Background**: Plain, uncluttered background preferred
- **Space**: Clear area around you (2m radius minimum)
- **Stability**: Secure device position (use tripod or stand)

#### Clothing
- **Fit**: Wear form-fitting clothes (not baggy)
- **Colors**: Contrasting colors from background
- **Avoid**: Accessories that obscure joints

#### Performance Tips
- Start with slower, controlled movements
- Keep entire body in frame throughout
- Perform exercises from the side view for best analysis
- Complete full range of motion for each rep
- Pause briefly between reps for accurate counting

---

## 🔧 Troubleshooting

### Camera & Pose Detection Issues

#### Problem: Pose not detected / Skeleton not showing

**Possible Causes & Solutions**:

✅ **Lighting too dim**
- Move to well-lit area
- Turn on additional lights
- Avoid backlighting from windows

✅ **Body not fully visible**
- Step back from camera
- Adjust camera angle
- Remove obstructions

✅ **Wearing baggy clothes**
- Change to form-fitting clothing
- Ensure joints are visible

✅ **Camera permission denied**
- Go to Settings → Apps → [App Name] → Permissions
- Enable Camera permission
- Restart app

---

#### Problem: Skeleton tracking is jittery or laggy

**Possible Causes & Solutions**:

✅ **Device performance**
- Close background apps
- Restart device
- Ensure Snapdragon device is being used for optimal performance

✅ **Insufficient lighting variations**
- Improve consistent lighting
- Avoid direct sunlight creating harsh shadows

✅ **Multiple people in frame**
- Ensure only one person is visible
- Remove others from camera view

---

#### Problem: Incorrect rep counting

**Possible Causes & Solutions**:

✅ **Partial range of motion**
- Perform full range of motion
- Go deeper in squats, lower in push-ups, etc.

✅ **Too fast movements**
- Slow down exercise tempo
- Pause briefly at top/bottom of each rep

✅ **Body parts leaving frame**
- Adjust camera distance
- Keep all joints visible throughout movement

---

### AI Analysis Issues

#### Problem: "Analysis Failed" or no feedback generated

**Possible Causes & Solutions**:

✅ **No internet connection**
- Check WiFi/mobile data
- Verify internet connectivity
- Retry after connection restored

✅ **Backend server not running**
- Ensure backend is started
- Check server logs for errors
- Verify API endpoint in app settings

✅ **Invalid Gemini API key**
- Check `.env` file in backend
- Verify API key is correct
- Ensure API key has necessary permissions
- Check quota limits on Google AI Studio

✅ **API rate limit exceeded**
- Wait a few minutes before retrying
- Check Gemini API quota in Google AI Studio
- Consider upgrading API plan

---

#### Problem: Analysis takes too long (>30 seconds)

**Possible Causes & Solutions**:

✅ **Slow internet connection**
- Switch to faster WiFi
- Move closer to router
- Try mobile data if WiFi is slow

✅ **API server issues**
- Check Gemini API status page
- Retry after a few minutes

✅ **Large video file**
- Reduce recording duration
- App should only send pose data, not video - verify implementation

---

#### Problem: Generic or irrelevant feedback

**Possible Causes & Solutions**:

✅ **Poor pose detection quality**
- Improve camera setup (lighting, distance)
- Ensure clear view of all joints
- Redo exercise with better positioning

✅ **Exercise not clearly performed**
- Follow proper form guidelines
- Make movements more deliberate
- Ensure exercise type is correctly selected

---

### App Crashes & Performance Issues

#### Problem: App crashes on launch

**Possible Causes & Solutions**:

✅ **Corrupted installation**
- Uninstall and reinstall app
- Clear app cache and data

✅ **Incompatible device**
- Check minimum Android version (API level)
- Verify device compatibility

✅ **Missing dependencies**
- Rebuild app with all dependencies
- Check Gradle sync success

---

#### Problem: App freezes during exercise

**Possible Causes & Solutions**:

✅ **Insufficient memory**
- Close other apps
- Restart device
- Clear cached data

✅ **Overheating**
- Let device cool down
- Reduce screen brightness
- Remove phone case if present

---

#### Problem: High battery drain

**Possible Causes & Solutions**:

✅ **Continuous camera usage**
- This is expected behavior
- Keep device charged during long sessions

✅ **Background processes**
- Ensure app optimizes for Snapdragon NPU
- Check for unnecessary background tasks

---

### Data & History Issues

#### Problem: Exercise history not saving

**Possible Causes & Solutions**:

✅ **Storage permission denied**
- Grant storage permissions in app settings
- Restart app after granting

✅ **Insufficient storage**
- Free up device storage
- Clear app cache

✅ **Database corruption**
- Clear app data (⚠️ will delete history)
- Reinstall app if issue persists

---

#### Problem: Cannot view past exercise details

**Possible Causes & Solutions**:

✅ **Data corruption**
- Check app logs for errors
- Try exporting data before clearing cache

✅ **App version mismatch**
- Ensure you're using latest version
- Data format may have changed in updates

---

### Network & Backend Issues

#### Problem: "Cannot connect to server" error

**Possible Causes & Solutions**:

✅ **Backend not running**
```bash
# Start the backend server
cd backend
python main.py
```

✅ **Incorrect API endpoint**
- Check app settings for correct server URL
- Default should be `http://localhost:8000` or your server IP
- Use your computer's IP address if testing on physical device

✅ **Firewall blocking connection**
- Allow backend port through firewall
- Check network security settings

---

#### Problem: "Unauthorized" or "API Key Invalid" error

**Possible Causes & Solutions**:

✅ **Gemini API key issue**
```bash
# Check your .env file
cd backend
cat .env
```
- Verify `GEMINI_API_KEY` is set correctly
- Generate new API key from Google AI Studio
- Ensure no extra spaces in .env file

✅ **API key expired or revoked**
- Generate new key from Google AI Studio
- Update .env file and restart backend

---

### Quality Issues

#### Problem: Feedback is not specific enough

**Improvement Steps**:

1. Ensure proper camera setup (distance, angle, lighting)
2. Perform exercise with clear, deliberate movements
3. Complete at least 3-5 reps for better analysis
4. Select the correct exercise type before starting
5. Keep entire body in frame throughout

---

#### Problem: App doesn't recognize my exercise

**Solutions**:

✅ **Ensure exercise is in supported list**
- Check README for supported exercises
- Similar exercises may work (e.g., sumo squat vs regular squat)

✅ **Improve pose visibility**
- Perform from side view for most exercises
- Ensure all key joints are visible
- Make movements clear and exaggerated

---

## 📞 Getting Help

If you continue to experience issues:

1. **Check Logs**
   - Android: Use `adb logcat` to view app logs
   - Backend: Check terminal output for errors

2. **Review Documentation**
   - README.md for setup instructions
   - API documentation for Gemini integration

3. **Report Issues**
   - Create an issue on GitHub repository
   - Include: device model, Android version, steps to reproduce, error logs

4. **Community Support**
   - Check existing GitHub issues for similar problems
   - Join project discussions

---

## 🔍 Diagnostic Commands

### Check Backend Status
```bash
# Test if backend is running
curl http://localhost:8000/health

# Check Gemini API connectivity
curl http://localhost:8000/api/test-gemini
```

### Check Android Device
```bash
# View real-time logs
adb logcat | grep "GymAssistant"

# Check device info
adb shell getprop ro.product.model
adb shell getprop ro.build.version.sdk

# Clear app data
adb shell pm clear com.logicalverse.gymassistant
```

### Verify Gemini API
```bash
# Test API key directly
curl -H "Content-Type: application/json" \
     -d '{"contents":[{"parts":[{"text":"Hello"}]}]}' \
     "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=YOUR_API_KEY"
```

---

## ✅ Success Checklist

Before considering the app fully functional, verify:

- [ ] Pose detection works smoothly in good lighting
- [ ] Rep counting is accurate for basic exercises
- [ ] AI analysis generates relevant feedback
- [ ] Exercise history saves and displays correctly
- [ ] App handles poor network gracefully
- [ ] Camera permissions work correctly
- [ ] Backend connects successfully
- [ ] Gemini API integration functions properly
- [ ] UI is responsive and intuitive
- [ ] App doesn't crash during normal use

---

## 📈 Performance Benchmarks

Expected performance metrics:

| Metric | Expected Value | Acceptable Range |
|--------|---------------|------------------|
| Pose Detection FPS | 30 FPS | 25-60 FPS |
| Analysis Response Time | 5-7 seconds | 3-10 seconds |
| Rep Count Accuracy | 95%+ | 90%+ |
| Battery Usage/Hour | ~15-20% | 10-25% |
| Memory Usage | <300 MB | <500 MB |

---

*Last Updated: February 2026*
