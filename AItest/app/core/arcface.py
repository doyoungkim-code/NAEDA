from functools import lru_cache

import cv2
import numpy as np
from fastapi import HTTPException, UploadFile
from insightface.app import FaceAnalysis

from app.core.config import get_settings


@lru_cache(maxsize=1)
def get_face_analyzer() -> FaceAnalysis:
    settings = get_settings()
    analyzer = FaceAnalysis(name=settings.arcface_model_name, providers=[settings.arcface_provider])
    # Use CPU context by default for local MVP compatibility.
    analyzer.prepare(ctx_id=-1, det_size=(640, 640))
    return analyzer


async def extract_embedding(upload_file: UploadFile) -> list[float]:
    raw = await upload_file.read()
    if not raw:
        raise HTTPException(status_code=400, detail="Empty image file")

    image_bytes = np.frombuffer(raw, dtype=np.uint8)
    bgr = cv2.imdecode(image_bytes, cv2.IMREAD_COLOR)
    if bgr is None:
        raise HTTPException(status_code=400, detail="Invalid image format")

    analyzer = get_face_analyzer()
    faces = analyzer.get(bgr)

    if len(faces) == 0:
        raise HTTPException(status_code=400, detail="No face detected")
    if len(faces) > 1:
        raise HTTPException(status_code=400, detail="Multiple faces detected")

    emb = faces[0].embedding
    norm = np.linalg.norm(emb)
    if norm == 0:
        raise HTTPException(status_code=500, detail="Invalid embedding generated")

    normalized = (emb / norm).astype(np.float32)
    return normalized.tolist()
