"""
AI Fitness Trainer - Squat Analysis Engine
Based on LearnOpenCV AI Fitness Trainer implementation.

Implements:
- 3-state machine (s1=Normal, s2=Transition, s3=Pass)
- 5 feedback types with severity levels
- Beginner/Pro modes with different thresholds
- Inactivity detection and counter reset
- Frontal view warning
"""
import time
from typing import List, Tuple, Optional, Dict
from dataclasses import dataclass, field

from angle_utils import (
    angle_at_point, 
    angle_with_vertical, 
    offset_angle,
    get_landmark_coords,
    LandmarkIndex
)
from models import (
    PoseLandmark, 
    AnalysisResult, 
    WorkoutSummary,
    SquatState, 
    FeedbackType, 
    WorkoutMode
)


@dataclass
class ThresholdConfig:
    """Threshold configuration for state detection and feedback."""
    # State thresholds (knee-vertical angle)
    state_s1_max: float = 32.0      # Max angle to be in s1 (Normal/Standing)
    state_s2_min: float = 35.0      # Min angle for s2 (Transition)
    state_s2_max: float = 65.0      # Max angle for s2
    state_s3_min: float = 75.0      # Min angle for s3 (Pass/Full depth)
    state_s3_max: float = 95.0      # Max angle for s3
    
    # Feedback thresholds
    hip_vertical_min: float = 20.0       # Below this = "bend forward" 
    hip_vertical_max: float = 45.0       # Above this = "bend backwards"
    lower_hips_min: float = 50.0         # Hip-knee-vertical range for "lower hips"
    lower_hips_max: float = 80.0
    knee_over_toes: float = 30.0         # Knee-ankle-vertical threshold
    deep_squat: float = 95.0             # Beyond this in s3 = too deep
    
    # View detection
    offset_thresh: float = 45.0          # Frontal view warning
    
    # Inactivity
    inactive_thresh: float = 15.0        # Seconds before reset
    
    # Full body visibility
    visibility_thresh: float = 0.5
    boundary_margin: float = 0.03
    frames_required: int = 3


# Predefined configurations for modes
BEGINNER_CONFIG = ThresholdConfig(
    state_s1_max=35.0,
    state_s2_min=38.0,
    state_s2_max=68.0,
    state_s3_min=72.0,
    state_s3_max=98.0,
    hip_vertical_min=18.0,
    hip_vertical_max=48.0,
    lower_hips_min=48.0,
    lower_hips_max=82.0,
    knee_over_toes=35.0,
    deep_squat=98.0,
    inactive_thresh=20.0
)

PRO_CONFIG = ThresholdConfig(
    state_s1_max=32.0,
    state_s2_min=35.0,
    state_s2_max=65.0,
    state_s3_min=75.0,
    state_s3_max=95.0,
    hip_vertical_min=20.0,
    hip_vertical_max=45.0,
    lower_hips_min=50.0,
    lower_hips_max=80.0,
    knee_over_toes=30.0,
    deep_squat=95.0,
    inactive_thresh=15.0
)


class FitnessTrainer:
    """
    AI Fitness Trainer for squat analysis.
    Maintains state across frames for accurate rep counting.
    """
    
    def __init__(self, mode: WorkoutMode = WorkoutMode.BEGINNER):
        self.mode = mode
        self.config = BEGINNER_CONFIG if mode == WorkoutMode.BEGINNER else PRO_CONFIG
        
        # State machine
        self.current_state = SquatState.S1_NORMAL
        self.prev_state = SquatState.S1_NORMAL
        self.state_sequence: List[str] = []
        
        # Counters
        self.correct_count = 0
        self.incorrect_count = 0
        
        # Tracking
        self.depth_angles: List[float] = []  # Knee angle at bottom of each rep
        self.min_knee_angle_this_rep = 180.0
        self.feedback_history: Dict[str, int] = {}
        
        # Timing
        self.start_time = time.time()
        self.last_active_time = time.time()
        self.active_seconds = 0.0
        
        # Readiness
        self.full_body_visible_count = 0
        self.is_ready = False
        
        # Last frame data for inactivity detection
        self.last_knee_vertical = 0.0
    
    def reset(self, mode: Optional[WorkoutMode] = None):
        """Reset trainer state for new workout."""
        if mode:
            self.mode = mode
            self.config = BEGINNER_CONFIG if mode == WorkoutMode.BEGINNER else PRO_CONFIG
        
        self.current_state = SquatState.S1_NORMAL
        self.prev_state = SquatState.S1_NORMAL
        self.state_sequence = []
        self.correct_count = 0
        self.incorrect_count = 0
        self.depth_angles = []
        self.min_knee_angle_this_rep = 180.0
        self.feedback_history = {}
        self.start_time = time.time()
        self.last_active_time = time.time()
        self.active_seconds = 0.0
        self.full_body_visible_count = 0
        self.is_ready = False
        self.last_knee_vertical = 0.0
    
    def _is_full_body_visible(self, landmarks: List[dict]) -> Tuple[bool, str]:
        """
        Check if key landmarks are visible and within frame.
        Returns (is_visible, debug_info).
        """
        key_indices = [
            (LandmarkIndex.LEFT_SHOULDER, "L.Shoulder"),
            (LandmarkIndex.RIGHT_SHOULDER, "R.Shoulder"),
            (LandmarkIndex.LEFT_HIP, "L.Hip"),
            (LandmarkIndex.RIGHT_HIP, "R.Hip"),
            (LandmarkIndex.LEFT_KNEE, "L.Knee"),
            (LandmarkIndex.RIGHT_KNEE, "R.Knee"),
            (LandmarkIndex.LEFT_ANKLE, "L.Ankle"),
            (LandmarkIndex.RIGHT_ANKLE, "R.Ankle"),
            (LandmarkIndex.NOSE, "Nose"),
        ]
        
        missing = []
        tol = self.config.boundary_margin
        vis_thresh = self.config.visibility_thresh
        
        for idx, name in key_indices:
            if idx >= len(landmarks):
                missing.append(name)
                continue
            
            lm = landmarks[idx]
            x = lm.get('x', 0.0)
            y = lm.get('y', 0.0)
            vis = lm.get('visibility', 1.0)
            
            # Check boundaries
            if x < tol or x > (1.0 - tol) or y < tol or y > (1.0 - tol):
                missing.append(f"{name}(edge)")
            elif vis < vis_thresh:
                missing.append(f"{name}(vis)")
        
        if missing:
            return False, f"Missing: {', '.join(missing[:3])}"
        return True, "Full body visible"
    
    def _determine_state(self, knee_vertical_angle: float) -> SquatState:
        """
        Determine current state based on knee-vertical angle.
        """
        cfg = self.config
        
        if knee_vertical_angle <= cfg.state_s1_max:
            return SquatState.S1_NORMAL
        elif cfg.state_s2_min <= knee_vertical_angle <= cfg.state_s2_max:
            return SquatState.S2_TRANSITION
        elif cfg.state_s3_min <= knee_vertical_angle <= cfg.state_s3_max:
            return SquatState.S3_PASS
        elif knee_vertical_angle > cfg.state_s3_max:
            # Beyond s3 range - still count as s3 but may trigger deep squat warning
            return SquatState.S3_PASS
        else:
            # In the gap between states - use previous state
            return self.current_state
    
    def _determine_feedback(self, 
                            hip_vertical: float,
                            knee_vertical: float, 
                            ankle_vertical: float,
                            is_going_down: bool) -> Tuple[FeedbackType, str, bool]:
        """
        Determine feedback based on angles.
        Returns (feedback_type, message, is_severe).
        """
        cfg = self.config
        
        # Check severe issues first
        
        # Knee falling over toes (severe)
        if ankle_vertical > cfg.knee_over_toes:
            return (
                FeedbackType.KNEE_OVER_TOES,
                "Knee falling over toes! Push hips back.",
                True
            )
        
        # Deep squat (severe) - only in s3 state
        if self.current_state == SquatState.S3_PASS and knee_vertical > cfg.deep_squat:
            return (
                FeedbackType.DEEP_SQUAT,
                "Too deep! Don't go past parallel.",
                True
            )
        
        # Non-severe feedback
        
        # Bend forward (torso too upright)
        if hip_vertical < cfg.hip_vertical_min:
            return (
                FeedbackType.BEND_FORWARD,
                "Lean your torso forward slightly.",
                False
            )
        
        # Bend backwards (torso too forward)
        if hip_vertical > cfg.hip_vertical_max:
            return (
                FeedbackType.BEND_BACKWARDS,
                "Straighten your back, lean less forward.",
                False
            )
        
        # Lower hips (only during descent s1->s2)
        if is_going_down and self.current_state == SquatState.S2_TRANSITION:
            if cfg.lower_hips_min <= knee_vertical <= cfg.lower_hips_max:
                return (
                    FeedbackType.LOWER_HIPS,
                    "Lower your hips more!",
                    False
                )
        
        # Good form - provide encouraging feedback based on state
        if self.current_state == SquatState.S1_NORMAL:
            return (FeedbackType.READY, "Ready - squat down!", False)
        elif self.current_state == SquatState.S3_PASS:
            return (FeedbackType.NONE, "Good depth! Now stand up.", False)
        else:
            return (FeedbackType.NONE, "", False)
    
    def _update_counters(self):
        """
        Update correct/incorrect counters based on state sequence.
        Called when returning to s1.
        """
        # Valid sequence: [s2, s3, s2] - went down through s2, hit s3, came back up through s2
        if len(self.state_sequence) >= 3:
            # Check if sequence contains s2 -> s3 -> s2
            has_s3 = SquatState.S3_PASS.value in self.state_sequence
            
            if has_s3:
                self.correct_count += 1
                # Record depth angle
                if self.min_knee_angle_this_rep < 180:
                    self.depth_angles.append(self.min_knee_angle_this_rep)
            else:
                # Never reached full depth
                self.incorrect_count += 1
        elif len(self.state_sequence) > 0:
            # Started but didn't complete
            self.incorrect_count += 1
        
        # Reset for next rep
        self.state_sequence = []
        self.min_knee_angle_this_rep = 180.0
    
    def _record_feedback(self, feedback_type: FeedbackType):
        """Record feedback occurrence for summary."""
        if feedback_type not in (FeedbackType.NONE, FeedbackType.READY):
            key = feedback_type.value
            self.feedback_history[key] = self.feedback_history.get(key, 0) + 1
    
    def analyze(self, landmarks: List[dict], 
                frame_width: int = 640, 
                frame_height: int = 480) -> AnalysisResult:
        """
        Analyze a single frame of pose landmarks.
        
        Args:
            landmarks: List of 33 landmark dicts with x, y, z, visibility
            frame_width: Frame width in pixels
            frame_height: Frame height in pixels
        
        Returns:
            AnalysisResult with all analysis data
        """
        result = AnalysisResult()
        
        # Check basic requirements
        if len(landmarks) < 33:
            result.debug_info = "Incomplete pose data"
            return result
        
        # Check full body visibility
        is_visible, debug_msg = self._is_full_body_visible(landmarks)
        result.debug_info = debug_msg
        result.is_full_body_visible = is_visible
        
        if is_visible:
            self.full_body_visible_count += 1
        else:
            self.full_body_visible_count = 0
            self.is_ready = False
        
        result.is_ready = self.full_body_visible_count >= self.config.frames_required
        
        # Extract landmarks (using normalized coordinates)
        def get_pt(idx):
            return get_landmark_coords(landmarks, idx, 1.0, 1.0)
        
        nose = get_pt(LandmarkIndex.NOSE)
        l_shoulder = get_pt(LandmarkIndex.LEFT_SHOULDER)
        r_shoulder = get_pt(LandmarkIndex.RIGHT_SHOULDER)
        l_hip = get_pt(LandmarkIndex.LEFT_HIP)
        r_hip = get_pt(LandmarkIndex.RIGHT_HIP)
        l_knee = get_pt(LandmarkIndex.LEFT_KNEE)
        r_knee = get_pt(LandmarkIndex.RIGHT_KNEE)
        l_ankle = get_pt(LandmarkIndex.LEFT_ANKLE)
        r_ankle = get_pt(LandmarkIndex.RIGHT_ANKLE)
        l_foot = get_pt(LandmarkIndex.LEFT_FOOT)
        r_foot = get_pt(LandmarkIndex.RIGHT_FOOT)
        
        # Calculate offset angle (frontal view detection)
        result.offset_angle = offset_angle(nose, l_shoulder, r_shoulder)
        result.is_frontal_view = result.offset_angle > self.config.offset_thresh
        
        if result.is_frontal_view:
            result.feedback_type = FeedbackType.FRONTAL_WARNING
            result.feedback_message = "Turn to side view for better analysis"
            result.current_state = self.current_state
            result.correct_count = self.correct_count
            result.incorrect_count = self.incorrect_count
            return result
        
        # Determine which side is more visible (use side with smaller knee angle)
        left_knee_angle = angle_at_point(l_hip, l_knee, l_ankle)
        right_knee_angle = angle_at_point(r_hip, r_knee, r_ankle)
        
        if left_knee_angle < right_knee_angle:
            side = "left"
            shoulder, hip, knee, ankle, foot = l_shoulder, l_hip, l_knee, l_ankle, l_foot
            knee_angle = left_knee_angle
        else:
            side = "right"
            shoulder, hip, knee, ankle, foot = r_shoulder, r_hip, r_knee, r_ankle, r_foot
            knee_angle = right_knee_angle
        
        result.detected_side = side
        result.knee_angle = knee_angle
        
        # Calculate angles with vertical
        hip_vertical = angle_with_vertical(shoulder, hip)
        knee_vertical = angle_with_vertical(hip, knee)
        ankle_vertical = angle_with_vertical(knee, ankle)
        
        result.hip_vertical_angle = hip_vertical
        result.knee_vertical_angle = knee_vertical
        result.ankle_vertical_angle = ankle_vertical
        
        # Track minimum knee angle for this rep
        if knee_angle < self.min_knee_angle_this_rep:
            self.min_knee_angle_this_rep = knee_angle
        
        # Check inactivity
        current_time = time.time()
        angle_change = abs(knee_vertical - self.last_knee_vertical)
        
        if angle_change > 3.0:  # Movement threshold
            self.last_active_time = current_time
            self.active_seconds += 0.033  # Approximate frame time
        
        inactivity = current_time - self.last_active_time
        result.inactivity_seconds = inactivity
        
        if inactivity > self.config.inactive_thresh:
            # Reset counters due to inactivity
            self.correct_count = 0
            self.incorrect_count = 0
            self.state_sequence = []
            self.last_active_time = current_time
        
        self.last_knee_vertical = knee_vertical
        
        # State machine - only process if ready
        if result.is_ready:
            self.is_ready = True
            
            # Determine new state
            new_state = self._determine_state(knee_vertical)
            
            # Check direction
            is_going_down = knee_vertical > self.last_knee_vertical
            
            # State transition logic
            if new_state != self.current_state:
                self.prev_state = self.current_state
                self.current_state = new_state
                
                # Track state sequence (only s2 and s3)
                if new_state in (SquatState.S2_TRANSITION, SquatState.S3_PASS):
                    if len(self.state_sequence) < 3:
                        self.state_sequence.append(new_state.value)
                
                # Check if we've returned to s1 (standing)
                if new_state == SquatState.S1_NORMAL:
                    self._update_counters()
            
            # Determine feedback
            feedback_type, feedback_msg, is_severe = self._determine_feedback(
                hip_vertical, knee_vertical, ankle_vertical, is_going_down
            )
            
            result.feedback_type = feedback_type
            result.feedback_message = feedback_msg
            result.is_severe_feedback = is_severe
            
            self._record_feedback(feedback_type)
        else:
            # Not ready
            result.feedback_type = FeedbackType.NONE
            result.feedback_message = "Position full body in frame"
        
        # Update result with current state
        result.current_state = self.current_state
        result.state_sequence = self.state_sequence.copy()
        result.correct_count = self.correct_count
        result.incorrect_count = self.incorrect_count
        
        return result
    
    def get_summary(self) -> WorkoutSummary:
        """Get workout summary for pause screen."""
        total_reps = self.correct_count + self.incorrect_count
        
        # Calculate accuracy
        accuracy = 0.0
        if total_reps > 0:
            accuracy = (self.correct_count / total_reps) * 100
        
        # Calculate average and best depth
        avg_depth = 180.0
        best_depth = 180.0
        if self.depth_angles:
            avg_depth = sum(self.depth_angles) / len(self.depth_angles)
            best_depth = min(self.depth_angles)
        
        # Calculate form score
        form_score = self._calculate_form_score()
        
        # Determine form label
        if form_score >= 80:
            form_label = "Excellent"
        elif form_score >= 60:
            form_label = "Good"
        elif form_score >= 40:
            form_label = "Fair"
        else:
            form_label = "Needs Work"
        
        # Most common issue
        most_common = None
        if self.feedback_history:
            most_common = max(self.feedback_history, key=self.feedback_history.get)
        
        # Duration
        total_duration = int(time.time() - self.start_time)
        
        return WorkoutSummary(
            total_reps=total_reps,
            correct_reps=self.correct_count,
            incorrect_reps=self.incorrect_count,
            accuracy_percentage=accuracy,
            average_depth_angle=avg_depth,
            best_depth_angle=best_depth,
            depth_angles=self.depth_angles.copy(),
            form_score=form_score,
            form_label=form_label,
            feedback_counts=self.feedback_history.copy(),
            most_common_issue=most_common,
            total_duration_seconds=total_duration,
            active_time_seconds=int(self.active_seconds),
            mode=self.mode
        )
    
    def _calculate_form_score(self) -> float:
        """Calculate overall form score (0-100)."""
        if self.correct_count + self.incorrect_count == 0:
            return 0.0
        
        # Accuracy component (50% of score)
        total = self.correct_count + self.incorrect_count
        accuracy_score = (self.correct_count / total) * 50
        
        # Depth consistency component (25% of score)
        consistency_score = 0.0
        if len(self.depth_angles) > 1:
            import numpy as np
            std_dev = np.std(self.depth_angles)
            # Lower std_dev = more consistent = higher score
            consistency_score = max(0, 25 - std_dev)
        elif len(self.depth_angles) == 1:
            consistency_score = 25.0
        
        # Severe feedback penalty (25% of score)
        severe_penalty = 0.0
        severe_types = [FeedbackType.KNEE_OVER_TOES.value, FeedbackType.DEEP_SQUAT.value]
        for ft in severe_types:
            count = self.feedback_history.get(ft, 0)
            severe_penalty += count * 2  # 2 points per severe issue
        
        feedback_score = max(0, 25 - severe_penalty)
        
        return min(100, accuracy_score + consistency_score + feedback_score)
