import asyncio
from functools import lru_cache

import cv2
import numpy as np
from fastapi import UploadFile
from fastapi.concurrency import run_in_threadpool
from insightface.app import FaceAnalysis

from app.core.config import get_settings
from app.core.errors import AIServiceError


@lru_cache(maxsize=1)
def get_face_analyzer() -> FaceAnalysis:
    settings = get_settings()
    analyzer = FaceAnalysis(name=settings.arcface_model_name, providers=[settings.arcface_provider])
    det_size = max(160, int(settings.arcface_det_size))
    analyzer.prepare(ctx_id=-1, det_size=(det_size, det_size))
    return analyzer


def _extract_embedding_from_bytes(image_raw: bytes) -> list[float]:
    image_bytes = np.frombuffer(image_raw, dtype=np.uint8)
    bgr = cv2.imdecode(image_bytes, cv2.IMREAD_COLOR)
    if bgr is None:
        raise AIServiceError(status_code=400, code="INVALID_IMAGE", message="Invalid image format")

    try:
        faces = get_face_analyzer().get(bgr)
    except AIServiceError:
        raise
    except Exception as exc:
        raise AIServiceError(status_code=503, code="AI_UNAVAILABLE", message="AI inference failed") from exc

    if len(faces) == 0:
        raise AIServiceError(status_code=400, code="NO_FACE", message="No face detected")
    if len(faces) > 1:
        raise AIServiceError(status_code=400, code="MULTIPLE_FACES", message="Multiple faces detected")

    emb = faces[0].embedding
    if emb is None:
        raise AIServiceError(status_code=503, code="AI_UNAVAILABLE", message="Embedding generation failed")

    norm = np.linalg.norm(emb)
    if norm == 0:
        raise AIServiceError(status_code=503, code="AI_UNAVAILABLE", message="Embedding normalization failed")

    normalized = (emb / norm).astype(np.float32).tolist()
    if len(normalized) != 512:
        raise AIServiceError(status_code=503, code="AI_UNAVAILABLE", message="Embedding dimension mismatch")

    return normalized


async def extract_embedding(upload_file: UploadFile, timeout_seconds: float) -> list[float]:
    image_raw = await upload_file.read()
    if not image_raw:
        raise AIServiceError(status_code=400, code="EMPTY_IMAGE", message="Image file is empty")

    try:
        return await asyncio.wait_for(
            run_in_threadpool(_extract_embedding_from_bytes, image_raw),
            timeout=timeout_seconds,
        )
    except asyncio.TimeoutError as exc:
        raise AIServiceError(status_code=504, code="AI_TIMEOUT", message="AI request timeout") from exc
