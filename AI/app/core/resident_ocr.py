import os
import re
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
    "DRIVER_LICENSE": ("운전면허증", "운전면허"),
}
NAME_LABELS = ("성명", "이름")
COMMON_NAME_STOPWORDS = (
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
DOCUMENT_NAME_STOPWORDS = {
    "RESIDENT_ID": (
        "세대주",
        "세대원",
        "주소지",
    ),
    "DRIVER_LICENSE": (
        "보통",
        "대형",
        "소형",
        "특수",
        "원동기",
        "1종",
        "2종",
        "종보통",
        "종대형",
        "종소형",
        "종특수",
        "면허번호",
        "적성검사",
        "적성검사기간",
        "발급일",
        "발행일",
    ),
}
RESIDENT_NUMBER_PATTERNS = (
    r"(\d{6})\s*[-]?\s*([1-4])[\d\*xX●•Oo]{6}",
    r"(\d{6})\s*[-]?\s*([1-4])",
)
NAME_DIRECT_PATTERN = re.compile(r"(?:성명|이름)\s*[:：]?\s*([가-힣\s]{2,10})")
RETAKE_REQUIRED = "RETAKE_REQUIRED"
REVIEW_REQUIRED = "REVIEW_REQUIRED"
SUCCESS = "SUCCESS"


def _normalize_name(value: str | None) -> str:
    return re.sub(r"\s+", "", value or "")


def _normalize_text(value: str | None) -> str:
    return re.sub(r"\s+", "", value or "")


def _clean_name_candidate(value: str | None) -> str:
    return _normalize_name(re.sub(r"[^가-힣\s]", " ", value or ""))


def _document_stopwords(document_type: str | None) -> tuple[str, ...]:
    if document_type is None:
        return COMMON_NAME_STOPWORDS
    return COMMON_NAME_STOPWORDS + DOCUMENT_NAME_STOPWORDS.get(document_type, ())


def _contains_name_stopword(candidate: str, document_type: str | None) -> bool:
    return any(fragment in candidate for fragment in _document_stopwords(document_type))


def _is_plausible_name_piece(value: str, document_type: str | None) -> bool:
    return bool(re.fullmatch(r"[가-힣]{1,5}", value)) and not _contains_name_stopword(value, document_type)


def _is_plausible_name(value: str, document_type: str | None) -> bool:
    return bool(re.fullmatch(r"[가-힣]{2,5}", value)) and not _contains_name_stopword(value, document_type)


def _decode_image(image_raw: bytes) -> np.ndarray:
    image_bytes = np.frombuffer(image_raw, dtype=np.uint8)
    bgr = cv2.imdecode(image_bytes, cv2.IMREAD_COLOR)
    if bgr is None:
        raise AIServiceError(status_code=400, code="INVALID_IMAGE", message="Invalid image format")
    return bgr


def _resize_for_ocr(image: np.ndarray, min_dimension: int = 1400) -> np.ndarray:
    height, width = image.shape[:2]
    longest = max(height, width)
    if longest >= min_dimension:
        return image
    scale = min_dimension / float(longest)
    resized_width = max(1, int(round(width * scale)))
    resized_height = max(1, int(round(height * scale)))
    return cv2.resize(image, (resized_width, resized_height), interpolation=cv2.INTER_CUBIC)


def _preprocess_variants_for_ocr(bgr: np.ndarray) -> list[tuple[str, np.ndarray]]:
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    normalized = cv2.normalize(gray, None, 0, 255, cv2.NORM_MINMAX)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8)).apply(normalized)
    sharpened = cv2.filter2D(
        clahe,
        -1,
        np.array([[0, -1, 0], [-1, 5, -1], [0, -1, 0]], dtype=np.float32),
    )
    threshold = cv2.adaptiveThreshold(
        sharpened,
        255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY,
        31,
        9,
    )

    variants = [
        ("normalized", cv2.cvtColor(normalized, cv2.COLOR_GRAY2BGR)),
        ("clahe_sharpened", cv2.cvtColor(sharpened, cv2.COLOR_GRAY2BGR)),
        ("threshold", cv2.cvtColor(threshold, cv2.COLOR_GRAY2BGR)),
    ]
    return [(name, _resize_for_ocr(image)) for name, image in variants]


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
                    "normalized_text": _normalize_text(text),
                    "confidence": confidence,
                    **metrics,
                }
            )

    entries.sort(key=lambda entry: (round(entry["top"] / 10.0), entry["left"]))
    return entries


def _detect_document_type(entries: list[dict[str, Any]]) -> tuple[str | None, float]:
    scores = {document_type: 0.0 for document_type in DOCUMENT_KEYWORDS}
    for entry in entries:
        normalized = entry["normalized_text"]
        confidence = entry["confidence"]
        for document_type, keywords in DOCUMENT_KEYWORDS.items():
            for keyword in keywords:
                normalized_keyword = _normalize_text(keyword)
                if normalized_keyword in normalized:
                    scores[document_type] = max(scores[document_type], min(0.99, 0.72 + confidence * 0.28))

    best_document_type = max(scores, key=scores.get, default=None)
    best_score = scores.get(best_document_type, 0.0) if best_document_type else 0.0
    if best_score <= 0.0:
        return None, 0.0
    return best_document_type, best_score


def _extract_resident_number(entries: list[dict[str, Any]]) -> tuple[str | None, str | None, float]:
    best_match: tuple[str, str, float] | None = None
    joined = " ".join(entry["text"] for entry in entries)

    for entry in entries:
        text = entry["text"]
        for pattern in RESIDENT_NUMBER_PATTERNS:
            match = re.search(pattern, text)
            if match:
                confidence = min(0.99, 0.65 + entry["confidence"] * 0.35)
                candidate = (match.group(1), match.group(2), confidence)
                if best_match is None or candidate[2] > best_match[2]:
                    best_match = candidate

    if best_match is not None:
        return best_match

    for pattern in RESIDENT_NUMBER_PATTERNS:
        match = re.search(pattern, joined)
        if match:
            digit_entries = [entry["confidence"] for entry in entries if re.search(r"\d", entry["text"] or "")]
            confidence = min(0.94, 0.56 + (sum(digit_entries) / len(digit_entries) if digit_entries else 0.0) * 0.3)
            return match.group(1), match.group(2), confidence

    return None, None, 0.0


def _contains_resident_number(text: str) -> bool:
    return any(re.search(pattern, text or "") for pattern in RESIDENT_NUMBER_PATTERNS)


def _extract_name_pieces(text: str, document_type: str | None) -> list[str]:
    pieces = [_clean_name_candidate(piece) for piece in re.findall(r"[가-힣\s]{1,10}", text or "")]
    return [piece for piece in pieces if piece and _is_plausible_name_piece(piece, document_type)]


def _name_length_bonus(name: str) -> float:
    if len(name) == 3:
        return 0.08
    if len(name) in (2, 4):
        return 0.05
    return 0.02


def _candidate_score(base_confidence: float, boost: float, name: str, label_distance: float = 0.0) -> float:
    distance_penalty = min(0.18, label_distance / 180.0)
    return max(0.0, min(0.99, boost + base_confidence * 0.35 + _name_length_bonus(name) - distance_penalty))


def _extract_name(entries: list[dict[str, Any]], document_type: str | None) -> tuple[str | None, float]:
    candidates: dict[str, float] = {}

    def register(candidate: str, confidence: float) -> None:
        cleaned = _clean_name_candidate(candidate)
        if not cleaned or not _is_plausible_name(cleaned, document_type):
            return
        candidates[cleaned] = max(confidence, candidates.get(cleaned, 0.0))

    for entry in entries:
        text = entry["text"]
        direct_match = NAME_DIRECT_PATTERN.search(text)
        if direct_match:
            register(
                direct_match.group(1),
                _candidate_score(entry["confidence"], 0.55, _clean_name_candidate(direct_match.group(1))),
            )

    for index, entry in enumerate(entries):
        if not any(label in entry["text"] for label in NAME_LABELS):
            continue

        same_row = [
            other
            for other in entries[index + 1 :]
            if abs(other["center_y"] - entry["center_y"]) <= max(18.0, entry["height"] * 1.5)
            and other["left"] >= entry["right"] - 16.0
            and not _contains_resident_number(other["text"])
        ]
        same_row.sort(key=lambda other: other["left"])

        pieces: list[tuple[str, float, float]] = []
        for other in same_row[:4]:
            for piece in _extract_name_pieces(other["text"], document_type):
                pieces.append((piece, other["confidence"], other["left"] - entry["right"]))

        for piece, confidence, distance in pieces:
            register(piece, _candidate_score(confidence, 0.42, piece, distance))

        for start in range(0, len(pieces)):
            joined = ""
            source_confidences: list[float] = []
            farthest_distance = 0.0
            for end in range(start, min(start + 3, len(pieces))):
                joined += pieces[end][0]
                source_confidences.append(pieces[end][1])
                farthest_distance = max(farthest_distance, pieces[end][2])
                if len(joined) > 5:
                    break
                register(joined, _candidate_score(sum(source_confidences) / len(source_confidences), 0.48, joined, farthest_distance))

    resident_number_index = next(
        (index for index, entry in enumerate(entries) if _contains_resident_number(entry["text"])),
        None,
    )
    if resident_number_index is not None:
        nearby_entries = entries[max(0, resident_number_index - 4) : resident_number_index]
        for entry in nearby_entries:
            for piece in _extract_name_pieces(entry["text"], document_type):
                register(piece, _candidate_score(entry["confidence"], 0.28, piece))

    for entry in entries:
        for piece in _extract_name_pieces(entry["text"], document_type):
            register(piece, _candidate_score(entry["confidence"], 0.12, piece))

    if not candidates:
        return None, 0.0

    name, confidence = max(candidates.items(), key=lambda item: item[1])
    return name, confidence


def _extract_fields_from_entries(entries: list[dict[str, Any]]) -> dict[str, Any]:
    document_type, document_confidence = _detect_document_type(entries)
    resident_front6, resident_back_first1, resident_number_confidence = _extract_resident_number(entries)
    name, name_confidence = _extract_name(entries, document_type)
    return {
        "document_type": document_type,
        "document_confidence": document_confidence,
        "resident_front6": resident_front6,
        "resident_back_first1": resident_back_first1,
        "resident_number_confidence": resident_number_confidence,
        "name": name,
        "name_confidence": name_confidence,
    }


def _vote_document_type(results: list[dict[str, Any]]) -> tuple[str | None, float]:
    scores: dict[str, float] = {}
    confidences: dict[str, float] = {}
    for result in results:
        document_type = result.get("document_type")
        confidence = float(result.get("document_confidence") or 0.0)
        if document_type not in SUPPORTED_DOCUMENT_TYPES:
            continue
        scores[document_type] = scores.get(document_type, 0.0) + confidence
        confidences[document_type] = max(confidences.get(document_type, 0.0), confidence)

    if not scores:
        return None, 0.0

    document_type = max(scores, key=scores.get)
    return document_type, confidences[document_type]


def _vote_text_field(results: list[dict[str, Any]], field_name: str, confidence_name: str) -> tuple[str | None, float]:
    scores: dict[str, float] = {}
    confidences: dict[str, float] = {}
    for result in results:
        value = result.get(field_name)
        confidence = float(result.get(confidence_name) or 0.0)
        if not value:
            continue
        scores[value] = scores.get(value, 0.0) + confidence
        confidences[value] = max(confidences.get(value, 0.0), confidence)

    if not scores:
        return None, 0.0

    value = max(scores, key=scores.get)
    return value, confidences[value]


def _build_extraction_summary(results: list[dict[str, Any]]) -> dict[str, Any]:
    settings = get_settings()
    document_type, document_confidence = _vote_document_type(results)
    if document_type not in SUPPORTED_DOCUMENT_TYPES:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="Unsupported id card type")

    filtered_results = [result for result in results if result.get("document_type") == document_type]
    if not filtered_results:
        filtered_results = results

    name, name_confidence = _vote_text_field(filtered_results, "name", "name_confidence")
    resident_number, resident_number_confidence = _vote_text_field(
        filtered_results,
        "resident_number_key",
        "resident_number_confidence",
    )

    resident_front6 = resident_number[:6] if resident_number else None
    resident_back_first1 = resident_number[6:] if resident_number else None

    warnings: list[str] = []
    status = SUCCESS

    if document_confidence < 0.78:
        warnings.append("문서 종류 인식 신뢰도가 낮습니다. 신분증을 정면으로 맞춰주세요.")
        status = REVIEW_REQUIRED

    if resident_front6 is None or resident_back_first1 is None:
        warnings.append("주민등록번호 일부를 다시 인식해 주세요.")
        status = RETAKE_REQUIRED
    elif resident_number_confidence < max(0.72, settings.resident_ocr_min_confidence):
        warnings.append("주민등록번호 일부 인식이 불안정합니다. 확인 화면에서 다시 확인해 주세요.")
        status = REVIEW_REQUIRED if status != RETAKE_REQUIRED else status

    if not name:
        warnings.append("이름을 자동으로 정확히 읽지 못했습니다. 확인 화면에서 직접 확인해 주세요.")
        if status == SUCCESS:
            status = REVIEW_REQUIRED
    elif name_confidence < max(0.78, settings.resident_ocr_min_confidence):
        warnings.append("이름 인식 신뢰도가 낮습니다. 확인 화면에서 이름을 다시 확인해 주세요.")
        if status == SUCCESS:
            status = REVIEW_REQUIRED

    available_confidences = [document_confidence, resident_number_confidence]
    if name:
        available_confidences.append(name_confidence)
    confidence = sum(available_confidences) / len(available_confidences) if available_confidences else 0.0

    return {
        "documentType": document_type,
        "documentMatched": True,
        "name": name,
        "residentFront6": resident_front6,
        "residentBackFirst1": resident_back_first1,
        "provider": "paddleocr",
        "confidence": round(float(confidence), 4),
        "documentConfidence": round(float(document_confidence), 4),
        "nameConfidence": round(float(name_confidence), 4),
        "residentNumberConfidence": round(float(resident_number_confidence), 4),
        "extractionStatus": status,
        "warnings": warnings,
    }


def _extract_with_paddle_provider(image_raw: bytes) -> dict[str, Any]:
    bgr = _decode_image(image_raw)
    variants = _preprocess_variants_for_ocr(bgr)
    ocr = _get_paddle_ocr()
    parsed_results: list[dict[str, Any]] = []

    for _, image in variants:
        try:
            result = ocr.ocr(image, cls=True)
        except AIServiceError:
            raise
        except Exception as exc:
            raise AIServiceError(status_code=503, code="OCR_UNAVAILABLE", message="PaddleOCR inference failed") from exc

        entries = _flatten_ocr_entries(result)
        if not entries:
            continue

        parsed = _extract_fields_from_entries(entries)
        parsed["resident_number_key"] = (
            f'{parsed["resident_front6"]}{parsed["resident_back_first1"]}'
            if parsed.get("resident_front6") and parsed.get("resident_back_first1")
            else None
        )
        parsed_results.append(parsed)

    if not parsed_results:
        raise AIServiceError(status_code=400, code="OCR_EXTRACTION_FAILED", message="No text detected")

    return _build_extraction_summary(parsed_results)


async def extract_resident_id_fields(upload_file: UploadFile) -> dict[str, Any]:
    settings = get_settings()
    image_raw = await upload_file.read()
    await upload_file.close()
    validate_image_bytes(image_raw, settings.ai_max_image_bytes)

    try:
        provider = settings.resident_ocr_provider.lower()
        if provider == "mock":
            raise AIServiceError(
                status_code=503,
                code="OCR_UNAVAILABLE",
                message="Mock OCR provider is disabled. Set RESIDENT_OCR_PROVIDER to paddleocr",
            )
        if provider == "paddleocr":
            return _extract_with_paddle_provider(image_raw)

        raise AIServiceError(
            status_code=503,
            code="OCR_UNAVAILABLE",
            message="ID card OCR provider is not configured",
        )
    finally:
        del image_raw
