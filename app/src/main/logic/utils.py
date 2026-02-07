import numpy as np


def get_landmark_array(landmarks, key, frame_width, frame_height):
    """Convert a normalized landmark to pixel coordinates (x,y) as ints.

    landmarks: sequence of landmark objects with .x and .y
    key: int index of the landmark
    """
    lm = landmarks[key]
    x = int(lm.x * frame_width)
    y = int(lm.y * frame_height)
    return np.array([x, y])


def get_landmark_features(landmarks, dict_features, feature, frame_width, frame_height):
    """Return pixel coordinates for a requested feature.

    feature can be 'nose', 'left' or 'right'. For left/right a tuple of
    (shoulder, elbow, wrist, hip, knee, ankle, foot) is returned.
    """
    if feature == 'nose':
        return get_landmark_array(landmarks, dict_features['nose'], frame_width, frame_height)

    if feature in ('left', 'right'):
        fdict = dict_features[feature]
        shldr = get_landmark_array(landmarks, fdict['shoulder'], frame_width, frame_height)
        elbow = get_landmark_array(landmarks, fdict['elbow'], frame_width, frame_height)
        wrist = get_landmark_array(landmarks, fdict['wrist'], frame_width, frame_height)
        hip = get_landmark_array(landmarks, fdict['hip'], frame_width, frame_height)
        knee = get_landmark_array(landmarks, fdict['knee'], frame_width, frame_height)
        ankle = get_landmark_array(landmarks, fdict['ankle'], frame_width, frame_height)
        foot = get_landmark_array(landmarks, fdict['foot'], frame_width, frame_height)
        return shldr, elbow, wrist, hip, knee, ankle, foot

    raise ValueError("feature needs to be either 'nose', 'left' or 'right'")


def find_angle(p1, p2, ref_pt=np.array([0, 0])):
    """Compute angle (in degrees) between vectors (p1-ref_pt) and (p2-ref_pt).

    Returns int degrees. Guards against zero-length vectors.
    """
    v1 = np.array(p1, dtype=float) - np.array(ref_pt, dtype=float)
    v2 = np.array(p2, dtype=float) - np.array(ref_pt, dtype=float)

    n1 = np.linalg.norm(v1)
    n2 = np.linalg.norm(v2)
    if n1 == 0 or n2 == 0:
        return 0

    cos_theta = np.dot(v1, v2) / (n1 * n2)
    cos_theta = np.clip(cos_theta, -1.0, 1.0)
    theta = np.arccos(cos_theta)
    deg = theta * 180.0 / np.pi
    return int(deg)


def angle_at_point(a, b, c):
    """Return the angle (degrees) at point b formed by points a-b-c.

    Useful for joint angles (e.g., hip-knee-ankle -> angle at knee).
    """
    a = np.array(a, dtype=float)
    b = np.array(b, dtype=float)
    c = np.array(c, dtype=float)

    v1 = a - b
    v2 = c - b
    n1 = np.linalg.norm(v1)
    n2 = np.linalg.norm(v2)
    if n1 == 0 or n2 == 0:
        return 0

    cos_theta = np.dot(v1, v2) / (n1 * n2)
    cos_theta = np.clip(cos_theta, -1.0, 1.0)
    theta = np.arccos(cos_theta)
    return float(theta * 180.0 / np.pi)
