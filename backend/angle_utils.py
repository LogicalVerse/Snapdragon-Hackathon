"""
Angle calculation utilities for pose analysis.
Implements LearnOpenCV AI Fitness Trainer angle calculations.
"""
import numpy as np
from typing import Tuple, List


def angle_at_point(a: Tuple[float, float], b: Tuple[float, float], c: Tuple[float, float]) -> float:
    """
    Calculate angle at point B formed by points A-B-C.
    Used for joint angles like hip-knee-ankle.
    
    Args:
        a: First point (x, y)
        b: Vertex point (x, y) - angle is measured here
        c: Third point (x, y)
    
    Returns:
        Angle in degrees (0-180)
    """
    a = np.array(a, dtype=float)
    b = np.array(b, dtype=float)
    c = np.array(c, dtype=float)
    
    v1 = a - b
    v2 = c - b
    
    n1 = np.linalg.norm(v1)
    n2 = np.linalg.norm(v2)
    
    if n1 == 0 or n2 == 0:
        return 0.0
    
    cos_theta = np.dot(v1, v2) / (n1 * n2)
    cos_theta = np.clip(cos_theta, -1.0, 1.0)
    theta = np.arccos(cos_theta)
    
    return float(theta * 180.0 / np.pi)


def angle_with_vertical(p1: Tuple[float, float], p2: Tuple[float, float]) -> float:
    """
    Calculate angle between line (p1 to p2) and vertical axis.
    Used for posture analysis (shoulder-hip, hip-knee, knee-ankle with vertical).
    
    The vertical is defined as pointing downward (y increases down in image coords).
    
    Args:
        p1: Upper point (x, y) - e.g., shoulder or hip
        p2: Lower point (x, y) - e.g., hip or knee
    
    Returns:
        Angle in degrees (0 = perfectly vertical, 90 = horizontal)
    """
    p1 = np.array(p1, dtype=float)
    p2 = np.array(p2, dtype=float)
    
    # Vector from p1 to p2
    line_vec = p2 - p1
    
    # Vertical vector (pointing down in image coordinates)
    vertical = np.array([0, 1], dtype=float)
    
    line_norm = np.linalg.norm(line_vec)
    if line_norm == 0:
        return 0.0
    
    cos_theta = np.dot(line_vec, vertical) / line_norm
    cos_theta = np.clip(cos_theta, -1.0, 1.0)
    theta = np.arccos(cos_theta)
    
    return float(theta * 180.0 / np.pi)


def offset_angle(nose: Tuple[float, float], 
                 left_shoulder: Tuple[float, float], 
                 right_shoulder: Tuple[float, float]) -> float:
    """
    Calculate offset angle to detect if person is facing the camera (frontal view).
    Uses nose position relative to shoulder midpoint.
    
    Args:
        nose: Nose coordinates (x, y)
        left_shoulder: Left shoulder (x, y)
        right_shoulder: Right shoulder (x, y)
    
    Returns:
        Offset angle in degrees. Low values = side view (good for squat).
        High values = frontal view (not ideal for squat analysis).
    """
    nose = np.array(nose, dtype=float)
    left_shoulder = np.array(left_shoulder, dtype=float)
    right_shoulder = np.array(right_shoulder, dtype=float)
    
    # Midpoint of shoulders
    shoulder_mid = (left_shoulder + right_shoulder) / 2
    
    # Vector from shoulder midpoint to nose
    to_nose = nose - shoulder_mid
    
    # Shoulder line vector
    shoulder_line = right_shoulder - left_shoulder
    
    # We want to measure how much the nose is in front vs to the side
    # Use cross product to determine offset
    shoulder_width = np.linalg.norm(shoulder_line)
    if shoulder_width == 0:
        return 0.0
    
    # Calculate how far nose is from shoulder midpoint in x-direction
    # relative to shoulder width
    nose_offset = abs(to_nose[0]) / shoulder_width
    
    # Convert to angle (approximate)
    # If nose is directly above shoulder midpoint, offset is low (side view)
    # If nose is far from midpoint in x, person is at an angle
    
    # Using the approach from LearnOpenCV: angle between nose and shoulder line
    return angle_at_point(left_shoulder, nose, right_shoulder)


def get_landmark_coords(landmarks: List[dict], index: int, 
                        frame_width: float = 1.0, 
                        frame_height: float = 1.0) -> Tuple[float, float]:
    """
    Extract (x, y) coordinates from landmarks array.
    
    Args:
        landmarks: List of landmark dicts with 'x', 'y' keys (normalized 0-1)
        index: Landmark index (0-32)
        frame_width: Optional frame width for pixel conversion
        frame_height: Optional frame height for pixel conversion
    
    Returns:
        (x, y) tuple in specified coordinate space
    """
    if index >= len(landmarks):
        return (0.0, 0.0)
    
    lm = landmarks[index]
    x = lm.get('x', 0.0) * frame_width
    y = lm.get('y', 0.0) * frame_height
    
    return (x, y)


# MediaPipe landmark indices
class LandmarkIndex:
    NOSE = 0
    LEFT_SHOULDER = 11
    RIGHT_SHOULDER = 12
    LEFT_ELBOW = 13
    RIGHT_ELBOW = 14
    LEFT_WRIST = 15
    RIGHT_WRIST = 16
    LEFT_HIP = 23
    RIGHT_HIP = 24
    LEFT_KNEE = 25
    RIGHT_KNEE = 26
    LEFT_ANKLE = 27
    RIGHT_ANKLE = 28
    LEFT_FOOT = 31
    RIGHT_FOOT = 32
