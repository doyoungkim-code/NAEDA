from fastapi import APIRouter, Depends, File, Form, UploadFile

from app.core.config import Settings, get_settings
from app.core.headpose import check_headpose
from app.core.security import verify_internal_service_token
from app.schemas.headpose import HeadPoseCheckResponse

router = APIRouter(prefix="/internal/v1/liveness", tags=["internal-liveness"])


@router.post(
    "/headpose/check",
    response_model=HeadPoseCheckResponse,
    responses={
        400: {"description": "Invalid image, direction, or face validation failed"},
        401: {"description": "Invalid service token"},
        503: {"description": "AI service unavailable"},
        504: {"description": "AI timeout"},
    },
)
async def check_headpose_direction(
    expected_direction: str = Form(..., alias="expectedDirection"),
    image: UploadFile = File(...),
    _: None = Depends(verify_internal_service_token),
    settings: Settings = Depends(get_settings),
) -> HeadPoseCheckResponse:
    result = await check_headpose(
        upload_file=image,
        expected_direction=expected_direction.lower(),
        timeout_seconds=settings.ai_timeout_seconds,
    )
    return HeadPoseCheckResponse(
        expected_direction=result.expected_direction,
        detected_direction=result.detected_direction,
        matched=result.matched,
        yaw=result.yaw,
        pitch=result.pitch,
        confidence=result.confidence,
    )

