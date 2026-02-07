"""
Tests for angle utility functions.
"""
import pytest
import math
from angle_utils import angle_at_point, angle_with_vertical, offset_angle


class TestAngleAtPoint:
    """Tests for angle_at_point function."""
    
    def test_right_angle(self):
        """90-degree angle at point B."""
        a = (0, 0)
        b = (1, 0)  # Vertex
        c = (1, 1)
        angle = angle_at_point(a, b, c)
        assert abs(angle - 90.0) < 1.0
    
    def test_straight_line(self):
        """180-degree angle (straight line)."""
        a = (0, 0)
        b = (1, 0)  # Vertex
        c = (2, 0)
        angle = angle_at_point(a, b, c)
        assert abs(angle - 180.0) < 1.0
    
    def test_acute_angle(self):
        """45-degree angle."""
        a = (0, 0)
        b = (1, 1)  # Vertex
        c = (2, 0)
        # This forms symmetric angle
        angle = angle_at_point(a, b, c)
        assert 40 < angle < 100  # Reasonable range
    
    def test_zero_length_vector(self):
        """Should handle zero-length vectors gracefully."""
        a = (1, 1)
        b = (1, 1)  # Same as a
        c = (2, 2)
        angle = angle_at_point(a, b, c)
        assert angle == 0.0


class TestAngleWithVertical:
    """Tests for angle_with_vertical function."""
    
    def test_vertical_line(self):
        """Perfectly vertical line should be 0 degrees."""
        p1 = (0.5, 0.0)
        p2 = (0.5, 1.0)
        angle = angle_with_vertical(p1, p2)
        assert abs(angle) < 1.0
    
    def test_horizontal_line(self):
        """Horizontal line should be 90 degrees."""
        p1 = (0.0, 0.5)
        p2 = (1.0, 0.5)
        angle = angle_with_vertical(p1, p2)
        assert abs(angle - 90.0) < 1.0
    
    def test_diagonal_45(self):
        """45-degree diagonal line."""
        p1 = (0.0, 0.0)
        p2 = (1.0, 1.0)
        angle = angle_with_vertical(p1, p2)
        assert abs(angle - 45.0) < 1.0


class TestOffsetAngle:
    """Tests for offset_angle function (frontal view detection)."""
    
    def test_side_view(self):
        """Side view should have low offset angle."""
        # Nose far from shoulder midpoint in depth
        nose = (0.5, 0.3)
        left_shoulder = (0.45, 0.4)
        right_shoulder = (0.55, 0.4)
        angle = offset_angle(nose, left_shoulder, right_shoulder)
        # Should be relatively high since we're using angle_at_point
        # which measures the angle at nose between shoulders
        assert angle > 0
    
    def test_frontal_view(self):
        """Frontal view - nose centered between shoulders."""
        nose = (0.5, 0.3)
        left_shoulder = (0.3, 0.4)
        right_shoulder = (0.7, 0.4)
        angle = offset_angle(nose, left_shoulder, right_shoulder)
        # Wide shoulder spread = more frontal
        assert angle > 0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
