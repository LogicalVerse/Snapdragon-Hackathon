import time
import cv2
import mediapipe as mp
import numpy as np
from utils import get_landmark_features, angle_at_point

# quick compatibility check for MediaPipe: some installs expose the newer
# "tasks" API instead of the classic `mp.solutions` module used here.
import argparse
import time
import cv2
import numpy as np
from utils import angle_at_point

# Try Tasks API first (requires .task model), fallback to legacy solutions API (bundled)
USING_TASKS_API = False
try:
    from mediapipe.tasks.python import vision
    from mediapipe.tasks.python import core as tasks_core
    from mediapipe.tasks.python.vision.core.image import Image, ImageFormat
    USING_TASKS_API = True
except ImportError:
    pass

# Fallback: legacy solutions API
if not USING_TASKS_API:
    try:
        import mediapipe as mp
        mp_pose = mp.solutions.pose
        USING_LEGACY = True
    except AttributeError:
        USING_LEGACY = False


DICT_FEATURES = {
    'left': {
        'shoulder': 11,
        'elbow': 13,
        'wrist': 15,
        'hip': 23,
        'knee': 25,
        'foot': 31,
    },
    'right': {
        'shoulder': 12,
        'elbow': 14,
        'wrist': 16,
        'hip': 24,
        'knee': 26,
        'foot': 32,
    },
    'nose': 0,
}


def is_full_body_visible(landmarks, tol=0.03):
    """Return True if key landmarks are inside the central image region.

    landmarks: sequence of normalized landmarks (have .x and .y, and optionally .visibility)
    tol: fraction margin around edges to consider (0.0..0.5)
    """
    # indices to check: shoulders, hips, knees, wrists, nose (skip ankles)
    indices = []
    for side in ('left', 'right'):
        for key, idx in DICT_FEATURES[side].items():
            if key == 'ankle':
                continue
            indices.append(idx)
    indices.append(DICT_FEATURES['nose'])

    for idx in set(indices):
        try:
            lm = landmarks[idx]
        except Exception:
            return False

        x = getattr(lm, 'x', None)
        y = getattr(lm, 'y', None)
        if x is None or y is None:
            return False

        if not (tol <= x <= 1.0 - tol and tol <= y <= 1.0 - tol):
            return False

        vis = getattr(lm, 'visibility', None)
        VIS_THRESH = 0.15
        if vis is not None and vis < VIS_THRESH:
            return False

    return True


def run_legacy_squat_monitor(camera_index: int = 0, depth_angle: float = 100.0, stand_angle: float = 160.0):
    """Run monitor using legacy mp.solutions.pose (bundled, no model needed)."""
    cap = cv2.VideoCapture(camera_index)
    if not cap.isOpened():
        print('Unable to open camera')
        return

    # Set camera properties for stability
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    cap.set(cv2.CAP_PROP_FPS, 30)

    pose = mp_pose.Pose(min_detection_confidence=0.5, min_tracking_confidence=0.5)

    squat_count = 0
    state = 'up'
    reached_depth = False
    full_body_visible_count = 0
    FULL_BODY_REQUIRED = 5
    ready = False
    frame_count = 0
    window_name = 'Squat Monitor (Legacy API)'
    full_body_visible_count = 0
    FULL_BODY_REQUIRED = 5
    ready = False

    print('Press ESC to exit. Squat detection (Legacy Solutions API) running...')
    print('Opening camera window...')

    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                print(f'[Frame {frame_count}] Failed to read frame from camera')
                break

            frame_count += 1
            h, w, _ = frame.shape
            
            # Flip frame for selfie view (mirror)
            frame = cv2.flip(frame, 1)
            
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            result = pose.process(rgb)

            # Display frame info
            cv2.putText(frame, f'Frame: {frame_count} | Size: {w}x{h}', (10, frame.shape[0] - 20), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

            if result.pose_landmarks:
                landmarks = result.pose_landmarks.landmark

                # check full-body visibility
                if is_full_body_visible(landmarks, tol=0.03):
                    full_body_visible_count += 1
                else:
                    full_body_visible_count = 0

                if full_body_visible_count >= FULL_BODY_REQUIRED:
                    if not ready:
                        print('Full body detected. Starting squat count.')
                    ready = True
                else:
                    # periodic debug print of key landmarks when not ready
                    if frame_count % 30 == 0 or full_body_visible_count == 0:
                        keys = ['l_shldr','r_shldr','l_hip','r_hip','l_knee','r_knee','l_foot','r_foot','nose']
                        idxs = [
                            DICT_FEATURES['left']['shoulder'], DICT_FEATURES['right']['shoulder'],
                            DICT_FEATURES['left']['hip'], DICT_FEATURES['right']['hip'],
                            DICT_FEATURES['left']['knee'], DICT_FEATURES['right']['knee'],
                            DICT_FEATURES['left']['foot'], DICT_FEATURES['right']['foot'],
                            DICT_FEATURES['nose']
                        ]
                        print(f'Full-body check debug (legacy) frame {frame_count}:')
                        for name, idx in zip(keys, idxs):
                            lm = landmarks[idx]
                            print(f'  {name}: x={lm.x:.3f} y={lm.y:.3f} vis={getattr(lm, "visibility", None)}')
                    ready = False

                def to_px(lm):
                    return (int(lm.x * w), int(lm.y * h))

                def extract_side(side):
                    f = DICT_FEATURES[side]
                    return tuple(to_px(landmarks[f[k]]) for k in ('shoulder', 'elbow', 'wrist', 'hip', 'knee', 'foot'))

                try:
                    ls = extract_side('left')
                    rs = extract_side('right')
                except Exception:
                    ls = rs = None

                if ls and rs and ready:
                    l_sh, l_el, l_wr, l_hp, l_kn, l_ft = ls
                    r_sh, r_el, r_wr, r_hp, r_kn, r_ft = rs

                    left_knee = angle_at_point(l_hp, l_kn, l_ft)
                    right_knee = angle_at_point(r_hp, r_kn, r_ft)

                    if left_knee < right_knee:
                        knee_angle = left_knee
                        side = 'left'
                        sh, el, wr, hp, kn, ft = ls
                    else:
                        knee_angle = right_knee
                        side = 'right'
                        sh, el, wr, hp, kn, ft = rs

                    cv2.line(frame, sh, el, (200, 200, 255), 3)
                    cv2.line(frame, el, wr, (200, 200, 255), 3)
                    cv2.line(frame, sh, hp, (200, 200, 255), 3)
                    cv2.line(frame, hp, kn, (200, 200, 255), 3)
                    cv2.line(frame, kn, ft, (200, 200, 255), 3)
                    for p in (sh, el, wr, hp, kn, ft):
                        cv2.circle(frame, p, 5, (0, 255, 255), -1)

                    if state == 'up' and knee_angle < depth_angle:
                        state = 'down'
                        reached_depth = True

                    if state == 'down' and knee_angle > stand_angle and reached_depth:
                        squat_count += 1
                        state = 'up'
                        reached_depth = False

                    cv2.putText(frame, f"Side: {side}", (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)
                    cv2.putText(frame, f"Knee angle: {int(knee_angle)}", (10, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (180, 255, 180), 2)
                    cv2.putText(frame, f"Squats: {squat_count}", (10, 95), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (50, 200, 255), 2)
            else:
                cv2.putText(frame, "No pose detected. Get in frame.", (10, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)

            if not ready:
                cv2.putText(frame, "Position your full body in the frame to start counting", (10, 90), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 255), 2)

            # Create window and display
            cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
            cv2.resizeWindow(window_name, 960, 720)
            cv2.imshow(window_name, frame)
            
            key = cv2.waitKey(1) & 0xFF
            if key == 27:  # ESC
                print(f'User pressed ESC. Exiting.')
                break
    except Exception as e:
        print(f'Error in monitor loop: {e}')
        import traceback
        traceback.print_exc()
    finally:
        cap.release()
        cv2.destroyAllWindows()
        print('Monitor stopped.')


def run_tasks_squat_monitor(model_path: str, camera_index: int = 0, depth_angle: float = 100.0, stand_angle: float = 160.0):
    """Run monitor using MediaPipe Tasks API (requires .task model file)."""
    if not model_path:
        print("No model specified. Tasks API requires a .task model file (see README).")
        return

    cap = cv2.VideoCapture(camera_index)
    if not cap.isOpened():
        print('Unable to open camera')
        return

    base_options = tasks_core.BaseOptions(model_asset_path=model_path)
    options = vision.PoseLandmarkerOptions(
        base_options=base_options,
        running_mode=vision.RunningMode.VIDEO,
        num_poses=1,
        min_pose_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    )

    landmarker = vision.PoseLandmarker.create_from_options(options)

    squat_count = 0
    state = 'up'
    reached_depth = False
    full_body_visible_count = 0
    FULL_BODY_REQUIRED = 5
    ready = False
    frame_count = 0

    print('Press ESC to exit. Squat detection (Tasks API) running...')

    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                break

            frame_count += 1

            h, w, _ = frame.shape
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            mp_img = Image(ImageFormat.SRGB, rgb)

            timestamp_ms = int(time.time() * 1000)
            result = landmarker.detect_for_video(mp_img, timestamp_ms)

            if result.pose_landmarks:
                landmarks = result.pose_landmarks[0]

                # check full-body visibility
                if is_full_body_visible(landmarks, tol=0.03):
                    full_body_visible_count += 1
                else:
                    full_body_visible_count = 0

                if full_body_visible_count >= FULL_BODY_REQUIRED:
                    if not ready:
                        print('Full body detected (Tasks API). Starting squat count.')
                    ready = True
                else:
                    if frame_count % 30 == 0 or full_body_visible_count == 0:
                        keys = ['l_shldr','r_shldr','l_hip','r_hip','l_knee','r_knee','l_foot','r_foot','nose']
                        idxs = [
                            DICT_FEATURES['left']['shoulder'], DICT_FEATURES['right']['shoulder'],
                            DICT_FEATURES['left']['hip'], DICT_FEATURES['right']['hip'],
                            DICT_FEATURES['left']['knee'], DICT_FEATURES['right']['knee'],
                            DICT_FEATURES['left']['foot'], DICT_FEATURES['right']['foot'],
                            DICT_FEATURES['nose']
                        ]
                        print(f'Full-body check debug (tasks) frame {frame_count}:')
                        for name, idx in zip(keys, idxs):
                            lm = landmarks[idx]
                            print(f'  {name}: x={lm.x:.3f} y={lm.y:.3f} vis={getattr(lm, "visibility", None)}')
                    ready = False

                def to_px(lm):
                    return (int(lm.x * w), int(lm.y * h))

                def extract_side(side):
                    f = DICT_FEATURES[side]
                    return tuple(to_px(landmarks[f[k]]) for k in ('shoulder', 'elbow', 'wrist', 'hip', 'knee', 'foot'))

                try:
                    ls = extract_side('left')
                    rs = extract_side('right')
                except Exception:
                    ls = rs = None

                if ls and rs and ready:
                    l_sh, l_el, l_wr, l_hp, l_kn, l_ft = ls
                    r_sh, r_el, r_wr, r_hp, r_kn, r_ft = rs

                    left_knee = angle_at_point(l_hp, l_kn, l_ft)
                    right_knee = angle_at_point(r_hp, r_kn, r_ft)

                    if left_knee < right_knee:
                        knee_angle = left_knee
                        side = 'left'
                        sh, el, wr, hp, kn, ft = ls
                    else:
                        knee_angle = right_knee
                        side = 'right'
                        sh, el, wr, hp, kn, ft = rs

                    cv2.line(frame, sh, el, (200, 200, 255), 3)
                    cv2.line(frame, el, wr, (200, 200, 255), 3)
                    cv2.line(frame, sh, hp, (200, 200, 255), 3)
                    cv2.line(frame, hp, kn, (200, 200, 255), 3)
                    cv2.line(frame, kn, ft, (200, 200, 255), 3)
                    for p in (sh, el, wr, hp, kn, ft):
                        cv2.circle(frame, p, 5, (0, 255, 255), -1)

                    if state == 'up' and knee_angle < depth_angle:
                        state = 'down'
                        reached_depth = True

                    if state == 'down' and knee_angle > stand_angle and reached_depth:
                        squat_count += 1
                        state = 'up'
                        reached_depth = False

                    cv2.putText(frame, f"Side: {side}", (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)
                    cv2.putText(frame, f"Knee angle: {int(knee_angle)}", (10, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (180, 255, 180), 2)
                    cv2.putText(frame, f"Squats: {squat_count}", (10, 95), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (50, 200, 255), 2)

            if not ready:
                cv2.putText(frame, "Position your full body in the frame to start counting", (10, 90), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 255), 2)

            cv2.imshow('Squat Monitor (Tasks API)', frame)
            if cv2.waitKey(1) & 0xFF == 27:
                break
    finally:
        cap.release()
        cv2.destroyAllWindows()
        landmarker.close()


def main():
    parser = argparse.ArgumentParser(description='Live squat angle monitor')
    parser.add_argument('--model', type=str, default='', help='Path to pose_landmarker .task model (for Tasks API)')
    parser.add_argument('--camera', type=int, default=0)
    parser.add_argument('--depth_angle', type=float, default=100.0)
    parser.add_argument('--stand_angle', type=float, default=160.0)
    parser.add_argument('--api', type=str, choices=['auto', 'legacy', 'tasks'], default='auto', 
                        help='Force an API choice (default: auto-detect)')
    args = parser.parse_args()

    # Auto-detect which API to use
    if args.api == 'auto':
        if USING_TASKS_API and args.model:
            use_tasks = True
        elif USING_LEGACY:
            use_tasks = False
        else:
            print('Error: No compatible MediaPipe API available.')
            return
    else:
        use_tasks = (args.api == 'tasks')

    if use_tasks:
        if not args.model:
            print('Tasks API requires --model <path_to_pose_landmarker_full.task>')
            print('Download the model and run: python3 pose_test.py --model <path> --api tasks')
            return
        run_tasks_squat_monitor(args.model, camera_index=args.camera, depth_angle=args.depth_angle, stand_angle=args.stand_angle)
    else:
        print('Using legacy MediaPipe Solutions API (bundled, no model needed).')
        run_legacy_squat_monitor(camera_index=args.camera, depth_angle=args.depth_angle, stand_angle=args.stand_angle)


if __name__ == '__main__':
    main()
