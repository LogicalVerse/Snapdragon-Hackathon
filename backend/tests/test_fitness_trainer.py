"""
Tests for FitnessTrainer class.
"""
import pytest
from fitness_trainer import FitnessTrainer, BEGINNER_CONFIG, PRO_CONFIG
from models import SquatState, FeedbackType, WorkoutMode


def create_mock_landmarks(knee_vertical_angle: float = 0.0, 
                          hip_vertical_angle: float = 30.0,
                          ankle_vertical_angle: float = 10.0):
    """
    Create mock landmarks that will produce desired angles.
    This is a simplified mock - in reality the angles would be calculated from positions.
    """
    # Create 33 landmarks with default positions
    landmarks = []
    for i in range(33):
        landmarks.append({
            "x": 0.5,
            "y": 0.5,
            "z": 0.0,
            "visibility": 0.9
        })
    
    # Position key landmarks to simulate the desired knee_vertical_angle
    # The angle is based on hip-knee line with vertical
    # For simplicity, set positions that approximate the angles
    
    # Shoulders at top
    landmarks[11] = {"x": 0.4, "y": 0.2, "z": 0.0, "visibility": 0.9}  # L shoulder
    landmarks[12] = {"x": 0.6, "y": 0.2, "z": 0.0, "visibility": 0.9}  # R shoulder
    
    # Hips
    landmarks[23] = {"x": 0.4, "y": 0.4, "z": 0.0, "visibility": 0.9}  # L hip
    landmarks[24] = {"x": 0.6, "y": 0.4, "z": 0.0, "visibility": 0.9}  # R hip
    
    # Knees - adjust x based on knee_vertical_angle
    # More angle = knees further forward (x offset)
    import math
    knee_x_offset = math.sin(math.radians(knee_vertical_angle)) * 0.2
    landmarks[25] = {"x": 0.4 + knee_x_offset, "y": 0.6, "z": 0.0, "visibility": 0.9}  # L knee
    landmarks[26] = {"x": 0.6 + knee_x_offset, "y": 0.6, "z": 0.0, "visibility": 0.9}  # R knee
    
    # Ankles
    landmarks[27] = {"x": 0.4, "y": 0.8, "z": 0.0, "visibility": 0.9}  # L ankle
    landmarks[28] = {"x": 0.6, "y": 0.8, "z": 0.0, "visibility": 0.9}  # R ankle
    
    # Feet
    landmarks[31] = {"x": 0.4, "y": 0.85, "z": 0.0, "visibility": 0.9}  # L foot
    landmarks[32] = {"x": 0.6, "y": 0.85, "z": 0.0, "visibility": 0.9}  # R foot
    
    # Nose
    landmarks[0] = {"x": 0.5, "y": 0.1, "z": 0.0, "visibility": 0.9}
    
    return landmarks


class TestFitnessTrainerInit:
    """Tests for FitnessTrainer initialization."""
    
    def test_beginner_mode(self):
        """Beginner mode uses relaxed thresholds."""
        trainer = FitnessTrainer(WorkoutMode.BEGINNER)
        assert trainer.mode == WorkoutMode.BEGINNER
        assert trainer.config.state_s1_max == 35.0  # Beginner threshold
    
    def test_pro_mode(self):
        """Pro mode uses strict thresholds."""
        trainer = FitnessTrainer(WorkoutMode.PRO)
        assert trainer.mode == WorkoutMode.PRO
        assert trainer.config.state_s1_max == 32.0  # Pro threshold
    
    def test_initial_state(self):
        """Initial state is s1 (standing)."""
        trainer = FitnessTrainer()
        assert trainer.current_state == SquatState.S1_NORMAL
        assert trainer.correct_count == 0
        assert trainer.incorrect_count == 0


class TestFitnessTrainerReset:
    """Tests for reset functionality."""
    
    def test_reset_clears_counts(self):
        """Reset clears counters."""
        trainer = FitnessTrainer()
        trainer.correct_count = 5
        trainer.incorrect_count = 2
        
        trainer.reset()
        
        assert trainer.correct_count == 0
        assert trainer.incorrect_count == 0
    
    def test_reset_changes_mode(self):
        """Reset can change mode."""
        trainer = FitnessTrainer(WorkoutMode.BEGINNER)
        trainer.reset(WorkoutMode.PRO)
        
        assert trainer.mode == WorkoutMode.PRO


class TestFitnessTrainerAnalysis:
    """Tests for pose analysis."""
    
    def test_full_body_visibility(self):
        """Returns is_full_body_visible when all landmarks present."""
        trainer = FitnessTrainer()
        landmarks = create_mock_landmarks(knee_vertical_angle=10.0)
        
        # Run multiple times to pass visibility threshold
        for _ in range(5):
            result = trainer.analyze(landmarks)
        
        assert result.is_full_body_visible == True
    
    def test_incomplete_landmarks(self):
        """Handles incomplete landmarks gracefully."""
        trainer = FitnessTrainer()
        landmarks = [{"x": 0.5, "y": 0.5, "z": 0.0, "visibility": 0.9}] * 10  # Only 10 landmarks
        
        result = trainer.analyze(landmarks)
        
        assert "Incomplete" in result.debug_info or not result.is_full_body_visible


class TestWorkoutSummary:
    """Tests for workout summary generation."""
    
    def test_empty_summary(self):
        """Summary with no reps."""
        trainer = FitnessTrainer()
        summary = trainer.get_summary()
        
        assert summary.total_reps == 0
        assert summary.accuracy_percentage == 0.0
    
    def test_summary_with_reps(self):
        """Summary after some reps."""
        trainer = FitnessTrainer()
        trainer.correct_count = 8
        trainer.incorrect_count = 2
        trainer.depth_angles = [85.0, 88.0, 82.0]
        
        summary = trainer.get_summary()
        
        assert summary.total_reps == 10
        assert summary.correct_reps == 8
        assert summary.incorrect_reps == 2
        assert summary.accuracy_percentage == 80.0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
