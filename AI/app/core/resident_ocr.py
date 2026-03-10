import re

from fastapi import UploadFile

from app.core.config import get_settings
from app.core.errors import AIServiceError
from app.core.upload_validation import validate_image_bytes


def _normalize_name(value: str) -> str:
    return re.sub(r"\s+", "", value or "")


def _normalize_digits(value: str, expected_length: int) -> str:
    digits = re.sub(r"\D", "", value or "")
    if len(digits) != expected_length:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Resident ID fields not extracted")
    return digits


def _extract_with_mock_provider() -> dict:
    settings = get_settings()
    return {
        "name": _normalize_name(settings.resident_ocr_mock_name),
        "residentFront6": _normalize_digits(settings.resident_ocr_mock_front6, 6),
        "residentBackFirst1": _normalize_digits(settings.resident_ocr_mock_back1, 1),
        "provider": "mock",
    }


async def extract_resident_id_fields(upload_file: UploadFile) -> dict:
    settings = get_settings()
    image_raw = await upload_file.read()
    await upload_file.close()
    validate_image_bytes(image_raw, settings.ai_max_image_bytes)

    provider = settings.resident_ocr_provider.lower()
    if provider == "mock":
        return _extract_with_mock_provider()

    raise AIServiceError(
        status_code=503,
        code="OCR_UNAVAILABLE",
        message="Resident ID OCR provider is not configured",
    )
