"""
FastAPI WebSocket server for real-time pose analysis.
Receives pose landmarks from Android app and returns analysis results.
"""
import json
import logging
from typing import Dict
from contextlib import asynccontextmanager

from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from models import (
    PoseData, 
    AnalysisResult, 
    WorkoutSummary, 
    ResetRequest, 
    HealthResponse,
    WorkoutMode
)
from fitness_trainer import FitnessTrainer

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Store active trainers per connection
trainers: Dict[str, FitnessTrainer] = {}

# Default trainer for REST endpoints
default_trainer = FitnessTrainer()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler."""
    logger.info("🏋️ Formly Pose Analysis Server starting...")
    yield
    logger.info("Server shutting down...")
    trainers.clear()


app = FastAPI(
    title="Formly Pose Analysis API",
    description="Real-time pose analysis for squat form using LearnOpenCV AI Fitness Trainer logic",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware for development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    return HealthResponse(status="healthy", version="1.0.0")


@app.post("/api/reset")
async def reset_workout(request: ResetRequest = None):
    """Reset workout state for a new session."""
    mode = request.mode if request else WorkoutMode.BEGINNER
    default_trainer.reset(mode)
    logger.info(f"Workout reset with mode: {mode}")
    return {"status": "reset", "mode": mode}


@app.get("/api/summary", response_model=WorkoutSummary)
async def get_summary():
    """Get workout summary (for pause screen)."""
    return default_trainer.get_summary()


@app.post("/api/analyze", response_model=AnalysisResult)
async def analyze_pose(pose_data: PoseData):
    """
    Analyze pose (REST endpoint - for testing or fallback).
    For real-time analysis, use WebSocket endpoint.
    """
    # Convert Pydantic models to dicts
    landmarks = [lm.model_dump() for lm in pose_data.landmarks]
    
    # Update mode if changed
    if pose_data.mode != default_trainer.mode:
        default_trainer.reset(pose_data.mode)
    
    result = default_trainer.analyze(
        landmarks,
        pose_data.frame_width,
        pose_data.frame_height
    )
    
    return result


@app.websocket("/ws/analyze")
async def websocket_analyze(websocket: WebSocket):
    """
    WebSocket endpoint for real-time pose analysis.
    
    Expected message format:
    {
        "landmarks": [{"x": 0.5, "y": 0.5, "z": 0.0, "visibility": 1.0}, ...],
        "timestamp": 123456789,
        "mode": "beginner",
        "frame_width": 640,
        "frame_height": 480
    }
    
    Response format: AnalysisResult as JSON
    """
    await websocket.accept()
    
    # Create trainer for this connection
    connection_id = str(id(websocket))
    trainer = FitnessTrainer()
    trainers[connection_id] = trainer
    
    logger.info(f"New WebSocket connection: {connection_id}")
    
    try:
        while True:
            # Receive pose data
            data = await websocket.receive_text()
            
            try:
                pose_dict = json.loads(data)
                
                # Handle special commands
                if pose_dict.get("command") == "reset":
                    mode_str = pose_dict.get("mode", "beginner")
                    mode = WorkoutMode.BEGINNER if mode_str == "beginner" else WorkoutMode.PRO
                    trainer.reset(mode)
                    await websocket.send_json({"status": "reset", "mode": mode_str})
                    continue
                
                if pose_dict.get("command") == "summary":
                    summary = trainer.get_summary()
                    await websocket.send_json(summary.model_dump())
                    continue
                
                # Parse pose data
                landmarks = pose_dict.get("landmarks", [])
                
                if not landmarks or len(landmarks) < 33:
                    await websocket.send_json({
                        "error": "Invalid landmarks data",
                        "received_count": len(landmarks)
                    })
                    continue
                
                # Check if mode changed
                mode_str = pose_dict.get("mode", "beginner")
                new_mode = WorkoutMode.BEGINNER if mode_str == "beginner" else WorkoutMode.PRO
                if new_mode != trainer.mode:
                    trainer.reset(new_mode)
                
                # Analyze pose
                frame_width = pose_dict.get("frame_width", 640)
                frame_height = pose_dict.get("frame_height", 480)
                
                result = trainer.analyze(landmarks, frame_width, frame_height)
                
                # Send result
                await websocket.send_json(result.model_dump())
                
            except json.JSONDecodeError as e:
                logger.warning(f"Invalid JSON received: {e}")
                await websocket.send_json({"error": "Invalid JSON format"})
            except Exception as e:
                logger.error(f"Analysis error: {e}")
                await websocket.send_json({"error": str(e)})
    
    except WebSocketDisconnect:
        logger.info(f"WebSocket disconnected: {connection_id}")
    finally:
        # Cleanup
        if connection_id in trainers:
            del trainers[connection_id]


@app.websocket("/ws/test")
async def websocket_test(websocket: WebSocket):
    """Simple WebSocket echo for connection testing."""
    await websocket.accept()
    logger.info("Test WebSocket connected")
    
    try:
        while True:
            data = await websocket.receive_text()
            await websocket.send_text(f"Echo: {data}")
    except WebSocketDisconnect:
        logger.info("Test WebSocket disconnected")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "pose_server:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )
