from fastapi import APIRouter, Depends

from app.core.config import Settings, get_settings
from app.core.security import verify_internal_service_token
from app.schemas.model_version import ModelVersionResponse

router = APIRouter(prefix="/internal/v1/model", tags=["internal-model-version"])


@router.get(
    "/version",
    response_model=ModelVersionResponse,
    responses={
        401: {"description": "Invalid service token"},
    },
)
async def get_model_version(
    _: None = Depends(verify_internal_service_token),
    settings: Settings = Depends(get_settings),
) -> ModelVersionResponse:
    return ModelVersionResponse(
        feature_version=settings.ai_feature_version,
        rule_version=settings.ai_rule_version,
        model_version=settings.ai_model_version,
        algorithm=settings.ai_model_algorithm,
        artifact_path=settings.ai_model_artifact_path,
        updated_at=settings.ai_model_updated_at,
    )
