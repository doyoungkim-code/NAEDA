import re
import os
from functools import lru_cache
from pathlib import Path

import cv2
import numpy as np

from fastapi import UploadFile

from app.core.config import get_settings
from app.core.errors import AIServiceError
from app.core.upload_validation import validate_image_bytes

SUPPORTED_DOCUMENT_TYPES = {"RESIDENT_ID", "DRIVER_LICENSE"}
DOCUMENT_KEYWORDS = {
    "RESIDENT_ID": ("주민등록증",),
    "DRIVER_LICENSE": ("운전면허", "운전면허증"),
}
NON_NAME_TOKENS = (
    "주민등록증",
    "운전면허",
    "운전면허증",
    "성명",
    "이름",
    "기간",
    "경찰청",
    "면허",
    "번호",
)


def _normalize_name(value: str) -> str:
    return re.sub(r"\s+", "", value or "")


def _normalize_digits(value: str, expected_length: int) -> str:
    digits = re.sub(r"\D", "", value or "")
    if len(digits) != expected_length:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Resident ID fields not extracted")
    return digits


def _decode_image(image_raw: bytes) -> np.ndarray:
    image_bytes = np.frombuffer(image_raw, dtype=np.uint8)
    bgr = cv2.imdecode(image_bytes, cv2.IMREAD_COLOR)
    if bgr is None:
        raise AIServiceError(status_code=400, code="INVALID_IMAGE", message="Invalid image format")
    return bgr


def _preprocess_for_ocr(bgr: np.ndarray) -> np.ndarray:
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    normalized = cv2.normalize(gray, None, 0, 255, cv2.NORM_MINMAX)
    return cv2.cvtColor(normalized, cv2.COLOR_GRAY2BGR)


@lru_cache(maxsize=1)
def _get_paddle_ocr():
    settings = get_settings()
    paddle_home = Path(settings.resident_ocr_paddle_home).resolve()
    paddle_home.mkdir(parents=True, exist_ok=True)
    os.environ["PADDLE_HOME"] = str(paddle_home)
    os.environ["HUB_HOME"] = str(paddle_home / "hub")
    os.environ["HOME"] = str(paddle_home)
    os.environ["USERPROFILE"] = str(paddle_home)

    try:
        from paddleocr import PaddleOCR  # type: ignore
    except ImportError as exc:
        raise AIServiceError(
            status_code=503,
            code="OCR_UNAVAILABLE",
            message="PaddleOCR is not installed",
        ) from exc

    try:
        return PaddleOCR(
            use_angle_cls=True,
            lang=settings.resident_ocr_paddle_lang,
            show_log=False,
        )
    except Exception as exc:
        raise AIServiceError(
            status_code=503,
            code="OCR_UNAVAILABLE",
            message="PaddleOCR initialization failed",
        ) from exc


def _flatten_ocr_result(result) -> list[tuple[str, float]]:
    flattened: list[tuple[str, float]] = []
    if not result:
        return flattened

    for block in result:
        if not block:
            continue
        for line in block:
            if not line or len(line) < 2 or not line[1]:
                continue
            text = str(line[1][0] or "").strip()
            confidence = float(line[1][1] or 0.0)
            if text:
                flattened.append((text, confidence))
    return flattened


def _detect_document_type(texts: list[str]) -> str | None:
    joined = " ".join(texts)
    for document_type, keywords in DOCUMENT_KEYWORDS.items():
        if any(keyword in joined for keyword in keywords):
            return document_type
    return None


def _extract_resident_number(texts: list[str]) -> tuple[str, str]:
    joined = " ".join(texts)
    patterns = [
        r"(\d{6})\s*[-]?\s*([1-4])[\d\*xX●•Oo]{6}",
        r"(\d{6})\s*[-]?\s*([1-4])",
    ]
    for pattern in patterns:
        match = re.search(pattern, joined)
        if match:
            return match.group(1), match.group(2)
    raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Resident number not extracted")


def _extract_name(texts: list[str]) -> str:
    for text in texts:
        match = re.search(r"(?:성명|이름)\s*[:：]?\s*([가-힣]{2,5})", text)
        if match:
            return _normalize_name(match.group(1))

    for text in texts:
        normalized = _normalize_name(text)
        if not normalized:
            continue
        if any(token in normalized for token in NON_NAME_TOKENS):
            continue
        if re.fullmatch(r"[가-힣]{2,5}", normalized):
            return normalized

    raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Name not extracted")


def _extract_with_paddle_provider(image_raw: bytes) -> dict:
    settings = get_settings()
    bgr = _decode_image(image_raw)
    preprocessed = _preprocess_for_ocr(bgr)

    try:
        result = _get_paddle_ocr().ocr(preprocessed, cls=True)
    except AIServiceError:
        raise
    except Exception as exc:
        raise AIServiceError(status_code=503, code="OCR_UNAVAILABLE", message="PaddleOCR inference failed") from exc

    lines = _flatten_ocr_result(result)
    if not lines:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="No text detected")

    texts = [text for text, _ in lines]
    document_type = _detect_document_type(texts)
    if document_type not in SUPPORTED_DOCUMENT_TYPES:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Unsupported id card type")

    resident_front6, resident_back_first1 = _extract_resident_number(texts)
    name = _extract_name(texts)
    confidence = min(
        1.0,
        max(settings.resident_ocr_min_confidence, sum(score for _, score in lines) / len(lines)),
    )

    return {
        "documentType": document_type,
        "documentMatched": True,
        "name": name,
        "residentFront6": resident_front6,
        "residentBackFirst1": resident_back_first1,
        "provider": "paddleocr",
        "confidence": confidence,
    }


def _extract_with_mock_provider() -> dict:
    settings = get_settings()
    document_type = (settings.resident_ocr_mock_document_type or "").upper()
    if document_type not in {"RESIDENT_ID", "DRIVER_LICENSE"}:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Unsupported id card type")
    return {
        "documentType": document_type,
        "documentMatched": True,
        "name": _normalize_name(settings.resident_ocr_mock_name),
        "residentFront6": _normalize_digits(settings.resident_ocr_mock_front6, 6),
        "residentBackFirst1": _normalize_digits(settings.resident_ocr_mock_back1, 1),
        "provider": "mock",
        "confidence": settings.resident_ocr_mock_confidence,
    }


async def extract_resident_id_fields(upload_file: UploadFile) -> dict:
    settings = get_settings()
    image_raw = await upload_file.read()
    await upload_file.close()
    validate_image_bytes(image_raw, settings.ai_max_image_bytes)

    try:
        provider = settings.resident_ocr_provider.lower()
        if provider == "mock":
            return _extract_with_mock_provider()
        if provider == "paddleocr":
            return _extract_with_paddle_provider(image_raw)

        raise AIServiceError(
            status_code=503,
            code="OCR_UNAVAILABLE",
            message="ID card OCR provider is not configured",
        )
    finally:
        del image_raw
