"""
Pydantic models for request/response data structures.
"""
from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum


class WorkoutMode(str, Enum):
    BEGINNER = "beginner"
    PRO = "pro"


class SquatState(str, Enum):
    S1_NORMAL = "s1"      # Standing - knee-vertical angle ≤ 32°
    S2_TRANSITION = "s2"  # Going down/up - 35°-65°
    S3_PASS = "s3"        # Full squat depth - 75°-95°


class FeedbackType(str, Enum):
    NONE = "none"
    READY = "ready"
    BEND_FORWARD = "bend_forward"       # Torso too upright, lean forward
    BEND_BACKWARDS = "bend_backwards"   # Torso leaning too far forward
    LOWER_HIPS = "lower_hips"           # Not going deep enough
    KNEE_OVER_TOES = "knee_over_toes"   # Knee tracking issue (severe)
    DEEP_SQUAT = "deep_squat"           # Going too deep (severe)
    FRONTAL_WARNING = "frontal_warning" # Turn to side view


class PoseLandmark(BaseModel):
    """Single pose landmark from MediaPipe (33 total)."""
    x: float = Field(..., ge=0.0, le=1.0, description="Normalized x coordinate")
    y: float = Field(..., ge=0.0, le=1.0, description="Normalized y coordinate")
    z: float = Field(default=0.0, description="Depth relative to hips")
    visibility: float = Field(default=1.0, ge=0.0, le=1.0, description="Landmark visibility")


class PoseData(BaseModel):
    """Pose data sent from Android app."""
    landmarks: List[PoseLandmark] = Field(..., min_length=33, max_length=33)
    timestamp: int = Field(..., description="Frame timestamp in milliseconds")
    mode: WorkoutMode = Field(default=WorkoutMode.BEGINNER)
    frame_width: int = Field(default=640)
    frame_height: int = Field(default=480)


class AnalysisResult(BaseModel):
    """Real-time analysis result returned to Android app."""
    # State machine
    current_state: SquatState = SquatState.S1_NORMAL
    state_sequence: List[str] = Field(default_factory=list)
    
    # Counters
    correct_count: int = 0
    incorrect_count: int = 0
    
    # Angles (in degrees)
    knee_angle: float = 180.0          # Hip-knee-ankle angle
    hip_vertical_angle: float = 0.0    # Shoulder-hip with vertical
    knee_vertical_angle: float = 0.0   # Hip-knee with vertical
    ankle_vertical_angle: float = 0.0  # Knee-ankle with vertical
    
    # Feedback
    feedback_type: FeedbackType = FeedbackType.NONE
    feedback_message: str = ""
    is_severe_feedback: bool = False   # For knee_over_toes and deep_squat
    
    # View detection
    detected_side: str = "right"       # Which side is more visible
    offset_angle: float = 0.0          # Frontal view detection
    is_frontal_view: bool = False      # Warning if facing camera
    
    # Readiness
    is_full_body_visible: bool = False
    is_ready: bool = False             # Ready to count reps
    
    # Timing
    inactivity_seconds: float = 0.0
    
    # Debug
    debug_info: str = ""


class WorkoutSummary(BaseModel):
    """Summary returned when workout is paused."""
    # Rep counts
    total_reps: int = 0
    correct_reps: int = 0
    incorrect_reps: int = 0
    
    # Accuracy
    accuracy_percentage: float = 0.0
    
    # Depth analysis
    average_depth_angle: float = 180.0   # Average knee angle at bottom
    best_depth_angle: float = 180.0      # Best (lowest) knee angle
    depth_angles: List[float] = Field(default_factory=list)
    
    # Form score (0-100)
    form_score: float = 0.0
    form_label: str = "N/A"
    
    # Feedback breakdown
    feedback_counts: dict = Field(default_factory=dict)  # {feedback_type: count}
    most_common_issue: Optional[str] = None
    
    # Timing
    total_duration_seconds: int = 0
    active_time_seconds: int = 0
    
    # Mode
    mode: WorkoutMode = WorkoutMode.BEGINNER


class ResetRequest(BaseModel):
    """Request to reset workout state."""
    mode: WorkoutMode = Field(default=WorkoutMode.BEGINNER)


class HealthResponse(BaseModel):
    """Health check response."""
    status: str = "healthy"
    version: str = "1.0.0"
