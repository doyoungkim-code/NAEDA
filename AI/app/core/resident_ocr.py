import re
import os
from functools import lru_cache
from pathlib import Path
from typing import Any

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
NAME_LABELS = ("성명", "이름")
NAME_BLACKLIST_FRAGMENTS = (
    "주민등록증",
    "운전면허",
    "운전면허증",
    "성명",
    "이름",
    "기간",
    "경찰청",
    "면허",
    "번호",
    "특별시",
    "광역시",
    "자치시",
    "자치도",
    "경기도",
    "강원도",
    "충청",
    "전라",
    "경상",
    "제주",
    "대한민국",
    "발급",
    "주소",
    "현주소",
    "시장",
    "청장",
    "구청장",
)
RESIDENT_NUMBER_PATTERNS = (
    r"(\d{6})\s*[-]?\s*([1-4])[\d\*xX●•Oo]{6}",
    r"(\d{6})\s*[-]?\s*([1-4])",
)


def _normalize_name(value: str) -> str:
    return re.sub(r"\s+", "", value or "")


def _normalize_digits(value: str, expected_length: int) -> str:
    digits = re.sub(r"\D", "", value or "")
    if len(digits) != expected_length:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Resident ID fields not extracted")
    return digits


def _clean_name_candidate(value: str) -> str:
    return _normalize_name(re.sub(r"[^가-힣\s]", " ", value or ""))


def _is_plausible_name_token(value: str) -> bool:
    return bool(re.fullmatch(r"[가-힣]{1,5}", value)) and not any(
        fragment in value for fragment in NAME_BLACKLIST_FRAGMENTS
    )


def _is_plausible_name(value: str) -> bool:
    return bool(re.fullmatch(r"[가-힣]{2,5}", value)) and not any(
        fragment in value for fragment in NAME_BLACKLIST_FRAGMENTS
    )


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


def _box_metrics(raw_box: Any) -> dict[str, float]:
    if not isinstance(raw_box, (list, tuple)):
        return {
            "left": 0.0,
            "right": 0.0,
            "top": 0.0,
            "bottom": 0.0,
            "center_x": 0.0,
            "center_y": 0.0,
            "height": 0.0,
        }

    xs: list[float] = []
    ys: list[float] = []
    for point in raw_box:
        if not isinstance(point, (list, tuple)) or len(point) < 2:
            continue
        try:
            xs.append(float(point[0]))
            ys.append(float(point[1]))
        except (TypeError, ValueError):
            continue

    if not xs or not ys:
        return {
            "left": 0.0,
            "right": 0.0,
            "top": 0.0,
            "bottom": 0.0,
            "center_x": 0.0,
            "center_y": 0.0,
            "height": 0.0,
        }

    left = min(xs)
    right = max(xs)
    top = min(ys)
    bottom = max(ys)
    return {
        "left": left,
        "right": right,
        "top": top,
        "bottom": bottom,
        "center_x": (left + right) / 2.0,
        "center_y": (top + bottom) / 2.0,
        "height": bottom - top,
    }


def _flatten_ocr_entries(result) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    if not result:
        return entries

    for block in result:
        if not block:
            continue
        for line in block:
            if not line or len(line) < 2 or not line[1]:
                continue
            text = str(line[1][0] or "").strip()
            confidence = float(line[1][1] or 0.0)
            if not text:
                continue
            metrics = _box_metrics(line[0] if len(line) > 0 else None)
            entries.append(
                {
                    "text": text,
                    "confidence": confidence,
                    **metrics,
                }
            )

    entries.sort(key=lambda entry: (round(entry["top"] / 10.0), entry["left"]))
    return entries


def _detect_document_type(texts: list[str]) -> str | None:
    joined = " ".join(texts)
    for document_type, keywords in DOCUMENT_KEYWORDS.items():
        if any(keyword in joined for keyword in keywords):
            return document_type
    return None


def _extract_resident_number(texts: list[str]) -> tuple[str, str]:
    joined = " ".join(texts)
    for pattern in RESIDENT_NUMBER_PATTERNS:
        match = re.search(pattern, joined)
        if match:
            return match.group(1), match.group(2)
    raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Resident number not extracted")


def _contains_resident_number(text: str) -> bool:
    return any(re.search(pattern, text or "") for pattern in RESIDENT_NUMBER_PATTERNS)


def _collect_name_from_entries(entries: list[dict[str, Any]]) -> str | None:
    tokens: list[str] = []
    for entry in entries:
        cleaned = _clean_name_candidate(entry["text"])
        if not cleaned or not _is_plausible_name_token(cleaned):
            continue
        tokens.append(cleaned)

    if not tokens:
        return None

    joined = "".join(tokens)
    if _is_plausible_name(joined):
        return joined

    for token in tokens:
        if _is_plausible_name(token):
            return token

    return None


def _extract_name(entries: list[dict[str, Any]]) -> str:
    for index, entry in enumerate(entries):
        text = entry["text"]
        direct_match = re.search(r"(?:성명|이름)\s*[:：]?\s*([가-힣\s]{2,10})", text)
        if direct_match:
            candidate = _clean_name_candidate(direct_match.group(1))
            if _is_plausible_name(candidate):
                return candidate

        if not any(label in text for label in NAME_LABELS):
            continue

        same_row_candidates = [
            other
            for other in entries[index + 1 :]
            if abs(other["center_y"] - entry["center_y"]) <= max(18.0, entry["height"] * 1.4)
            and other["left"] >= entry["right"] - 12.0
            and not _contains_resident_number(other["text"])
        ]
        candidate = _collect_name_from_entries(same_row_candidates[:3])
        if candidate:
            return candidate

        trailing_candidates = [
            other
            for other in entries[index + 1 : index + 5]
            if not _contains_resident_number(other["text"])
        ]
        candidate = _collect_name_from_entries(trailing_candidates)
        if candidate:
            return candidate

    resident_number_index = next(
        (index for index, entry in enumerate(entries) if _contains_resident_number(entry["text"])),
        None,
    )
    if resident_number_index is not None:
        candidate = _collect_name_from_entries(entries[max(0, resident_number_index - 4) : resident_number_index])
        if candidate:
            return candidate

    for window_size in (1, 2, 3):
        for start in range(0, max(0, len(entries) - window_size + 1)):
            candidate = _collect_name_from_entries(entries[start : start + window_size])
            if candidate:
                return candidate

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

    entries = _flatten_ocr_entries(result)
    if not entries:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="No text detected")

    texts = [entry["text"] for entry in entries]
    document_type = _detect_document_type(texts)
    if document_type not in SUPPORTED_DOCUMENT_TYPES:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Unsupported id card type")

    resident_front6, resident_back_first1 = _extract_resident_number(texts)
    name = _extract_name(entries)
    confidence = min(
        1.0,
        max(settings.resident_ocr_min_confidence, sum(entry["confidence"] for entry in entries) / len(entries)),
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
