import asyncio
from dataclasses import dataclass
from typing import Literal

import cv2
import numpy as np
from fastapi import UploadFile
from fastapi.concurrency import run_in_threadpool

from app.core.arcface import get_face_analyzer, reset_face_analyzer
from app.core.config import get_settings
from app.core.errors import AIServiceError

Direction = Literal["front", "left", "right", "up", "down"]
VALID_DIRECTIONS: set[str] = {"front", "left", "right", "up", "down"}


@dataclass
class HeadPoseResult:
    expected_direction: Direction
    detected_direction: Direction
    matched: bool
    yaw: float
    pitch: float
    confidence: float
    fallback_used: bool
    ai_status: str
    message: str


def _clip01(value: float) -> float:
    return max(0.0, min(1.0, value))


def _detect_direction(yaw: float, pitch: float) -> Direction:
    yaw_threshold = 0.16
    pitch_threshold = 0.12

    if yaw <= -yaw_threshold:
        return "left"
    if yaw >= yaw_threshold:
        return "right"
    if pitch <= -pitch_threshold:
        return "up"
    if pitch >= pitch_threshold:
        return "down"
    return "front"


def _build_result(
    expected_direction: str,
    detected_direction: Direction,
    yaw: float,
    pitch: float,
    confidence: float,
    fallback_used: bool,
    message: str,
) -> HeadPoseResult:
    return HeadPoseResult(
        expected_direction=expected_direction,  # type: ignore[arg-type]
        detected_direction=detected_direction,
        matched=detected_direction == expected_direction,
        yaw=yaw,
        pitch=pitch,
        confidence=confidence,
        fallback_used=fallback_used,
        ai_status="FALLBACK_APPLIED" if fallback_used else "COMPLETED",
        message=message,
    )


def _fallback_from_pose(face, expected_direction: str) -> HeadPoseResult:
    pose = getattr(face, "pose", None)
    if pose is None or len(pose) < 2:
        raise AIServiceError(status_code=503, code="AI_UNAVAILABLE", message="Face keypoints unavailable")

    yaw = float(pose[0])
    pitch = float(pose[1])
    detected_direction = _detect_direction(yaw, pitch)
    confidence = _clip01(0.5 + max(abs(yaw), abs(pitch)))
    return _build_result(
        expected_direction=expected_direction,
        detected_direction=detected_direction,
        yaw=yaw,
        pitch=pitch,
        confidence=confidence,
        fallback_used=True,
        message="Head pose fallback used model pose estimation.",
    )


def _extract_headpose_from_bytes(image_raw: bytes, expected_direction: str) -> HeadPoseResult:
    if expected_direction not in VALID_DIRECTIONS:
        raise AIServiceError(
            status_code=400,
            code="INVALID_DIRECTION",
            message="expectedDirection must be one of front|left|right|up|down",
        )

    image_bytes = np.frombuffer(image_raw, dtype=np.uint8)
    bgr = cv2.imdecode(image_bytes, cv2.IMREAD_COLOR)
    if bgr is None:
        raise AIServiceError(status_code=400, code="INVALID_IMAGE", message="Invalid image format")

    try:
        faces = get_face_analyzer().get(bgr)
    except Exception as exc:
        raise AIServiceError(status_code=503, code="AI_UNAVAILABLE", message="AI inference failed") from exc

    if len(faces) == 0:
        raise AIServiceError(status_code=400, code="NO_FACE", message="No face detected")
    if len(faces) > 1:
        raise AIServiceError(status_code=400, code="MULTIPLE_FACES", message="Multiple faces detected")

    kps = getattr(faces[0], "kps", None)
    if kps is None or len(kps) < 5:
        return _fallback_from_pose(faces[0], expected_direction)

    left_eye = np.array(kps[0], dtype=np.float32)
    right_eye = np.array(kps[1], dtype=np.float32)
    nose = np.array(kps[2], dtype=np.float32)
    left_mouth = np.array(kps[3], dtype=np.float32)
    right_mouth = np.array(kps[4], dtype=np.float32)

    eye_center = (left_eye + right_eye) / 2.0
    mouth_center = (left_mouth + right_mouth) / 2.0

    eye_distance = float(np.linalg.norm(right_eye - left_eye))
    if eye_distance <= 1e-6:
        return _fallback_from_pose(faces[0], expected_direction)

    vertical_span = abs(float(mouth_center[1] - eye_center[1]))
    if vertical_span <= 1e-6:
        return _fallback_from_pose(faces[0], expected_direction)

    yaw = float((nose[0] - eye_center[0]) / eye_distance)
    mid_y = float((eye_center[1] + mouth_center[1]) / 2.0)
    pitch = float((nose[1] - mid_y) / vertical_span)

    detected_direction = _detect_direction(yaw, pitch)
    max_component = max(abs(yaw), abs(pitch))
    confidence = _clip01(0.55 + max_component)

    return _build_result(
        expected_direction=expected_direction,
        detected_direction=detected_direction,
        yaw=yaw,
        pitch=pitch,
        confidence=confidence,
        fallback_used=False,
        message="Primary head pose inference succeeded.",
    )


async def check_headpose(upload_file: UploadFile, expected_direction: str, timeout_seconds: float) -> HeadPoseResult:
    settings = get_settings()
    image_raw = await upload_file.read()
    if not image_raw:
        raise AIServiceError(status_code=400, code="EMPTY_IMAGE", message="Image file is empty")

    last_error: AIServiceError | None = None
    total_attempts = max(1, settings.ai_retry_count + 1)

    for attempt in range(1, total_attempts + 1):
        try:
            result = await asyncio.wait_for(
                run_in_threadpool(_extract_headpose_from_bytes, image_raw, expected_direction),
                timeout=timeout_seconds,
            )
            if attempt > 1 and not result.fallback_used:
                return _build_result(
                    expected_direction=result.expected_direction,
                    detected_direction=result.detected_direction,
                    yaw=result.yaw,
                    pitch=result.pitch,
                    confidence=result.confidence,
                    fallback_used=True,
                    message="Primary head pose inference recovered after retry.",
                )
            return result
        except asyncio.TimeoutError as exc:
            last_error = AIServiceError(status_code=504, code="AI_TIMEOUT", message="AI request timeout")
            if attempt >= total_attempts:
                raise last_error from exc
        except AIServiceError as exc:
            last_error = exc
            if exc.status_code < 500 or attempt >= total_attempts:
                raise

        reset_face_analyzer()
        if settings.ai_retry_backoff_ms > 0:
            await asyncio.sleep(settings.ai_retry_backoff_ms / 1000)

    raise last_error or AIServiceError(status_code=503, code="AI_UNAVAILABLE", message="AI inference failed")

