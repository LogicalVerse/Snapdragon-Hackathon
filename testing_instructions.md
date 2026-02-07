# Testing, Usage & Troubleshooting Guide

## 📱 Testing the App

### Pre-Testing Checklist

Before you begin testing, ensure you have:

- [ ] Snapdragon-powered Android device (recommended) or emulator
- [ ] Camera permissions granted to the app
- [ ] Internet connection (for Gemini API analysis)
- [ ] valid Gemini API key
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
1. Select exercise (squats, bicep-curls(physiotherapy)) from the list
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
5. Check if AI suggestions appear on the screen, for ex: knees falling over the toes
6. Tap "Stop"
7. Wait for AI analysis to complete

**Expected Result**:
- Rep counter increases with each squat
- Analysis is generated within 5-10 seconds
- Feedback includes form assessment and suggestions

---

#### 4. Physiotherapy Exercise Test (Bicep Curls)

**Objective**: Verify physiotherapy exercise tracking

**Steps**:
1. Select "Bicep Curls" from the exercise list
2. Tap "Start Exercise"
3. Position yourself so your full body is visible
4. Perform slow, controlled movements
5. Check if AI suggestions appear on the screen, for ex: open your arm more
6. Tap "Stop"
7. Review AI feedback

**Expected Result**:
- App provides detailed movement analysis
- Range of motion metrics displayed
- Specific guidance and form suggestions

---

#### 5. Gemini API Integration Test

**Objective**: Confirm AI analysis is working

**Steps**:
1. Complete any exercise
2. Verify response from Gemini API
3. Review the generated feedback quality

**Expected Result**:
- API call succeeds (HTTP 200)
- Feedback is relevant and actionable
- Response time < 10 seconds

---

#### 6. Edge Case Testing

**Test Case A: Poor Lighting**
- Test in dim lighting conditions
- Verify app shows warning or adjusts detection

**Test Case B: Partial Body Visibility**
- Start exercise with only upper body visible
- Check if app prompts user to adjust position

---

## 🎯 How to Use the App

### First-Time Setup

1. **Install the App**
   - Install the APK on your Snapdragon device

2. **Grant Permissions**
   - Camera access (required for pose detection)
   - Storage access (for saving exercise history)
   - Internet access (for AI analysis)

3. **Configure Backend (if self-hosting)**
   - Update API key in app settings
   - Verify connection to backend server

### Using the Gym Assistant

1. **Select Exercise Type**
   ```
   Home Screen → Gym Workouts → Select Exercise (e.g., Squats, Bicep Curls)
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
   - Tap "Stop" when finished 
   - Wait for AI analysis (5-10 seconds)
   - Review detailed feedback

#### Understanding the Feedback

Your AI analysis includes:

- **✅ Form Assessment**: Overall form rating (Good/Bad/Needs Improvement)
- **📊 Metrics**: Reps completed, time taken, quality score
- **⚠️ Common Mistakes**: Specific issues detected (e.g., "open your arm more")
- **💡 Suggestions**: Actionable tips to improve form


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
- Go deeper in squats, better bicep curls, etc.

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

✅ **Invalid Gemini API key**
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

### Network & Backend Issues

#### Problem: "Cannot connect to server" error

**Possible Causes & Solutions**:

✅ **Incorrect API endpoint**
- Check app settings for correct server URL
- Default should be `http://localhost:8000` or your server IP
- Use your computer's IP address if testing on physical device

✅ **Firewall blocking connection**
- Allow backend port through firewall
- Check network security settings

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

✅ **Improve pose visibility**
- Perform from side view for most exercises
- Ensure all key joints are visible
- Make movements clear and exaggerated

---

## ✅ Success Checklist

Before considering the app fully functional, verify:

- [ ] Pose detection works smoothly in good lighting
- [ ] Rep counting is accurate for basic exercises
- [ ] AI analysis generates relevant feedback
- [ ] App handles poor network gracefully
- [ ] Camera permissions work correctly
- [ ] Backend connects successfully
- [ ] Gemini API integration functions properly
- [ ] UI is responsive and intuitive
- [ ] App doesn't crash during normal use

---

*Last Updated: February 2026*
