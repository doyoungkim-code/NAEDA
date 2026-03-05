from fastapi import APIRouter, Depends, File, UploadFile

from app.core.arcface import extract_embedding
from app.core.config import Settings, get_settings
from app.core.security import verify_internal_service_token
from app.schemas.embedding import EmbeddingExtractResponse

router = APIRouter(prefix="/internal/v1/embeddings", tags=["internal-embeddings"])


@router.post(
    "/extract",
    response_model=EmbeddingExtractResponse,
    responses={
        400: {"description": "Invalid image or face validation failed"},
        401: {"description": "Invalid service token"},
        503: {"description": "AI service unavailable"},
        504: {"description": "AI timeout"},
    },
)
async def extract_face_embedding(
    image: UploadFile = File(...),
    _: None = Depends(verify_internal_service_token),
    settings: Settings = Depends(get_settings),
) -> EmbeddingExtractResponse:
    result = await extract_embedding(image, timeout_seconds=settings.ai_timeout_seconds)
    embedding = result["embedding"]
    return EmbeddingExtractResponse(
        embedding=embedding,
        dim=len(embedding),
        model=f"arcface-{settings.arcface_model_name}",
        face_count=1,
        quality_score=result["quality_score"],
        yaw=result["yaw"],
        pitch=result["pitch"],
        roll=result["roll"],
    )
