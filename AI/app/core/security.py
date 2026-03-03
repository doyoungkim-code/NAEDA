from fastapi import Depends, Request

from app.core.config import Settings, get_settings
from app.core.errors import AIServiceError


def verify_internal_service_token(
    request: Request,
    settings: Settings = Depends(get_settings),
) -> None:
    token = request.headers.get("X-Service-Token")
    if not token or token != settings.internal_service_token:
        raise AIServiceError(status_code=401, code="UNAUTHORIZED", message="Invalid service token")
