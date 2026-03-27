import logging
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

logger = logging.getLogger(__name__)

SUPPORTED_DOCUMENT_TYPES = {"RESIDENT_ID", "DRIVER_LICENSE"}
DOCUMENT_KEYWORDS = {
    "RESIDENT_ID": ("주민등록증", "주민등록", "민등록증"),
    "DRIVER_LICENSE": ("운전면허증", "운전면허", "면허증"),
}
DOCUMENT_HINT_KEYWORDS = {
    "RESIDENT_ID": (
        "주민등록",
        "민등록",
        "현주소",
        "등록기준지",
        "세대주",
        "세대원",
        "주소지",
    ),
    "DRIVER_LICENSE": (
        "면허번호",
        "적성검사",
        "적성검사기간",
        "경찰청",
        "1종",
        "2종",
        "보통",
        "대형",
        "소형",
        "특수",
        "원동기",
    ),
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
    r"(\d{6})\s*[-\-ㅡ—~]?\s*([1-4])[\d\*xX●•Oo]{6}",
    r"(\d{6})\s*[-\-ㅡ—~]?\s*([1-4])",
)
NAME_DIRECT_PATTERN = re.compile(r"(?:성명|이름)\s*[:：]?\s*([가-힣\s]{2,10})")

# OCR이 흔히 혼동하는 문자 → 숫자 매핑
_OCR_DIGIT_REPLACEMENTS = {
    "O": "0", "o": "0", "Q": "0",
    "I": "1", "l": "1", "i": "1", "|": "1",
    "Z": "2", "z": "2",
    "S": "5", "s": "5",
    "B": "8", "b": "6",
    "G": "6", "g": "9",
    "T": "7",
    "A": "4",
}
_OCR_DIGIT_PATTERN = re.compile(r"[OoQIli|ZzSsBbGgTA]")
RETAKE_REQUIRED = "RETAKE_REQUIRED"
REVIEW_REQUIRED = "REVIEW_REQUIRED"
SUCCESS = "SUCCESS"


def _retake_result(message: str) -> dict[str, Any]:
    return {
        "documentType": None,
        "documentMatched": False,
        "name": None,
        "residentFront6": None,
        "residentBackFirst1": None,
        "provider": "paddleocr",
        "confidence": 0.0,
        "documentConfidence": 0.0,
        "nameConfidence": 0.0,
        "residentNumberConfidence": 0.0,
        "extractionStatus": RETAKE_REQUIRED,
        "warnings": [message],
    }


def _normalize_name(value: str | None) -> str:
    return re.sub(r"\s+", "", value or "")


def _normalize_text(value: str | None) -> str:
    return re.sub(r"\s+", "", value or "")


def _clean_name_candidate(value: str | None) -> str:
    return _normalize_name(re.sub(r"[^가-힣\s]", " ", value or ""))


def _document_stopwords(document_type: str | None) -> tuple[str, ...]:
    keyword_fragments = tuple(
        keyword
        for keywords in DOCUMENT_KEYWORDS.values()
        for keyword in keywords
    )
    if document_type is None:
        return COMMON_NAME_STOPWORDS + keyword_fragments
    return COMMON_NAME_STOPWORDS + keyword_fragments + DOCUMENT_NAME_STOPWORDS.get(document_type, ())


def _contains_name_stopword(candidate: str, document_type: str | None) -> bool:
    return any(fragment in candidate for fragment in _document_stopwords(document_type))


def _is_plausible_name_piece(value: str, document_type: str | None) -> bool:
    return bool(re.fullmatch(r"[가-힣]{1,5}", value)) and not _contains_name_stopword(value, document_type)


def _is_plausible_name(value: str, document_type: str | None) -> bool:
    return bool(re.fullmatch(r"[가-힣]{2,5}", value)) and not _contains_name_stopword(value, document_type)


def _sanitize_name(value: str | None, document_type: str | None) -> str | None:
    cleaned = _clean_name_candidate(value)
    if not cleaned or not _is_plausible_name(cleaned, document_type):
        return None
    return cleaned


def _decode_image(image_raw: bytes) -> np.ndarray:
    image_bytes = np.frombuffer(image_raw, dtype=np.uint8)
    bgr = cv2.imdecode(image_bytes, cv2.IMREAD_COLOR)
    if bgr is None:
        raise AIServiceError(status_code=400, code="INVALID_IMAGE", message="Invalid image format")
    return bgr


def _resize_for_ocr(image: np.ndarray, min_dimension: int = 1000, max_dimension: int = 2400) -> np.ndarray:
    height, width = image.shape[:2]
    longest = max(height, width)
    if longest < min_dimension:
        scale = min_dimension / float(longest)
        resized_width = max(1, int(round(width * scale)))
        resized_height = max(1, int(round(height * scale)))
        return cv2.resize(image, (resized_width, resized_height), interpolation=cv2.INTER_CUBIC)
    if longest > max_dimension:
        scale = max_dimension / float(longest)
        resized_width = max(1, int(round(width * scale)))
        resized_height = max(1, int(round(height * scale)))
        return cv2.resize(image, (resized_width, resized_height), interpolation=cv2.INTER_AREA)
    return image


def _order_quad_points(points: np.ndarray) -> np.ndarray:
    rect = np.zeros((4, 2), dtype=np.float32)
    sums = points.sum(axis=1)
    diffs = np.diff(points, axis=1).reshape(-1)
    rect[0] = points[np.argmin(sums)]
    rect[2] = points[np.argmax(sums)]
    rect[1] = points[np.argmin(diffs)]
    rect[3] = points[np.argmax(diffs)]
    return rect


def _detect_document_corners(bgr: np.ndarray) -> np.ndarray | None:
    height, width = bgr.shape[:2]
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    # 여러 Canny 임계값으로 시도하여 다양한 조명 조건 대응
    all_contours = []
    for low, high in ((30, 100), (40, 130), (60, 180)):
        edges = cv2.Canny(blurred, low, high)
        edges = cv2.dilate(edges, np.ones((3, 3), dtype=np.uint8), iterations=1)
        edges = cv2.erode(edges, np.ones((3, 3), dtype=np.uint8), iterations=1)
        found, _ = cv2.findContours(edges, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
        all_contours.extend(found)
    contours = all_contours

    best_corners: np.ndarray | None = None
    best_score = 0.0
    min_area = float(height * width) * 0.12

    for contour in contours:
        area = cv2.contourArea(contour)
        if area < min_area:
            continue

        perimeter = cv2.arcLength(contour, True)
        if perimeter <= 0:
            continue

        approx = cv2.approxPolyDP(contour, 0.02 * perimeter, True)
        if len(approx) == 4:
            corners = approx.reshape(4, 2).astype(np.float32)
        else:
            rect = cv2.minAreaRect(contour)
            box = cv2.boxPoints(rect)
            corners = np.array(box, dtype=np.float32)

        ordered = _order_quad_points(corners)
        width_top = np.linalg.norm(ordered[1] - ordered[0])
        width_bottom = np.linalg.norm(ordered[2] - ordered[3])
        height_left = np.linalg.norm(ordered[3] - ordered[0])
        height_right = np.linalg.norm(ordered[2] - ordered[1])
        warped_width = max(width_top, width_bottom)
        warped_height = max(height_left, height_right)
        if warped_width <= 0 or warped_height <= 0:
            continue

        aspect_ratio = max(warped_width, warped_height) / max(1.0, min(warped_width, warped_height))
        aspect_penalty = min(1.0, abs(aspect_ratio - 1.58) / 0.9)
        area_ratio = min(1.0, area / (float(height * width) * 0.65))
        rectangularity = min(1.0, area / max(1.0, warped_width * warped_height))
        score = area_ratio * 0.55 + (1.0 - aspect_penalty) * 0.25 + rectangularity * 0.20

        if score > best_score:
            best_score = score
            best_corners = ordered

    if best_score < 0.35:
        return None
    return best_corners


def _normalize_document_image(bgr: np.ndarray) -> np.ndarray | None:
    corners = _detect_document_corners(bgr)
    if corners is None:
        return None

    width_top = np.linalg.norm(corners[1] - corners[0])
    width_bottom = np.linalg.norm(corners[2] - corners[3])
    height_left = np.linalg.norm(corners[3] - corners[0])
    height_right = np.linalg.norm(corners[2] - corners[1])
    w = max(width_top, width_bottom)
    h = max(height_left, height_right)

    # 신분증은 가로가 긴 문서 — quad 좌표가 세로로 잡혔으면 w/h를 교환해서 가로로 보정
    if h > w:
        # 코너를 한 칸씩 회전시켜 가로 방향으로 재매핑
        corners = np.array([corners[3], corners[0], corners[1], corners[2]], dtype=np.float32)
        w, h = h, w

    max_width = max(1, int(round(w)))
    max_height = max(1, int(round(h)))
    destination = np.array(
        [
            [0.0, 0.0],
            [max_width - 1.0, 0.0],
            [max_width - 1.0, max_height - 1.0],
            [0.0, max_height - 1.0],
        ],
        dtype=np.float32,
    )
    matrix = cv2.getPerspectiveTransform(corners, destination)
    warped = cv2.warpPerspective(bgr, matrix, (max_width, max_height))
    if warped.size == 0:
        return None
    return warped


def _preprocess_variants_for_ocr(bgr: np.ndarray) -> list[tuple[str, np.ndarray]]:
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)

    # 변형 1: 원본 컬러
    # 변형 2: 단순 그레이스케일 정규화 (독립 경로)
    normalized = cv2.normalize(gray, None, 0, 255, cv2.NORM_MINMAX)

    # 변형 3: CLAHE 대비 강화 (독립 경로)
    clahe = cv2.createCLAHE(clipLimit=2.5, tileGridSize=(8, 8)).apply(gray)

    # 변형 4: 디노이즈 + 샤프닝 (독립 경로)
    denoised = cv2.fastNlMeansDenoising(gray, None, h=10, templateWindowSize=7, searchWindowSize=21)
    sharpened = cv2.filter2D(
        denoised,
        -1,
        np.array([[0, -1, 0], [-1, 5, -1], [0, -1, 0]], dtype=np.float32),
    )

    # 변형 5: 적응형 이진화 (그레이스케일에서 직접, 독립 경로)
    blurred_for_thresh = cv2.GaussianBlur(gray, (3, 3), 0)
    threshold = cv2.adaptiveThreshold(
        blurred_for_thresh,
        255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY,
        31,
        9,
    )

    # 변형 6: Otsu 이진화 (조명 불균일 대응, 독립 경로)
    blurred_for_otsu = cv2.GaussianBlur(gray, (5, 5), 0)
    _, otsu = cv2.threshold(blurred_for_otsu, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

    variants = [
        ("color", bgr.copy()),
        ("normalized", cv2.cvtColor(normalized, cv2.COLOR_GRAY2BGR)),
        ("clahe", cv2.cvtColor(clahe, cv2.COLOR_GRAY2BGR)),
        ("denoised_sharp", cv2.cvtColor(sharpened, cv2.COLOR_GRAY2BGR)),
        ("adaptive_thresh", cv2.cvtColor(threshold, cv2.COLOR_GRAY2BGR)),
        ("otsu", cv2.cvtColor(otsu, cv2.COLOR_GRAY2BGR)),
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
        logger.exception("PaddleOCR import failed")
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
        logger.exception("PaddleOCR initialization failed")
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


def _score_document_keywords(entries: list[dict[str, Any]], keywords: tuple[str, ...], base: float, slope: float) -> float:
    score = 0.0
    for keyword in keywords:
        normalized_keyword = _normalize_text(keyword)
        best_confidence = 0.0
        for entry in entries:
            if normalized_keyword in entry["normalized_text"]:
                best_confidence = max(best_confidence, float(entry["confidence"] or 0.0))
        if best_confidence > 0.0:
            score += base + best_confidence * slope
    return min(0.99, score)


def _entries_contain_name_label(entries: list[dict[str, Any]]) -> bool:
    return any(any(label in entry["text"] for label in NAME_LABELS) for entry in entries)


def _detect_document_type(
    entries: list[dict[str, Any]],
    *,
    resident_number_present: bool,
    name_present: bool,
) -> tuple[str | None, float]:
    scores = {document_type: 0.0 for document_type in DOCUMENT_KEYWORDS}
    header_scores = {
        document_type: _score_document_keywords(entries, keywords, 0.72, 0.22)
        for document_type, keywords in DOCUMENT_KEYWORDS.items()
    }
    hint_scores = {
        document_type: _score_document_keywords(entries, keywords, 0.16, 0.08)
        for document_type, keywords in DOCUMENT_HINT_KEYWORDS.items()
    }

    for document_type in scores:
        scores[document_type] = max(header_scores[document_type], hint_scores[document_type])
        if resident_number_present and scores[document_type] >= 0.16:
            scores[document_type] = min(0.96, scores[document_type] + 0.08)
        if name_present and scores[document_type] >= 0.16:
            scores[document_type] = min(0.96, scores[document_type] + 0.05)

    if scores["RESIDENT_ID"] <= 0.0 and resident_number_present and (_entries_contain_name_label(entries) or hint_scores["RESIDENT_ID"] > 0.0):
        scores["RESIDENT_ID"] = 0.56 if name_present else 0.48

    best_document_type = max(scores, key=scores.get, default=None)
    best_score = scores.get(best_document_type, 0.0) if best_document_type else 0.0
    if best_score <= 0.0:
        return None, 0.0
    return best_document_type, best_score


def _correct_ocr_digits(text: str) -> str:
    return _OCR_DIGIT_PATTERN.sub(lambda m: _OCR_DIGIT_REPLACEMENTS.get(m.group(), m.group()), text)


def _extract_resident_number(entries: list[dict[str, Any]]) -> tuple[str | None, str | None, float]:
    best_match: tuple[str, str, float] | None = None
    joined = " ".join(entry["text"] for entry in entries)

    # 1차: 원본 텍스트에서 매칭
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

    # 2차: OCR 오인식 문자 보정 후 매칭 (개별 entry)
    for entry in entries:
        corrected = _correct_ocr_digits(entry["text"])
        if corrected == entry["text"]:
            continue
        for pattern in RESIDENT_NUMBER_PATTERNS:
            match = re.search(pattern, corrected)
            if match:
                confidence = min(0.94, 0.55 + entry["confidence"] * 0.30)
                candidate = (match.group(1), match.group(2), confidence)
                if best_match is None or candidate[2] > best_match[2]:
                    best_match = candidate

    if best_match is not None:
        return best_match

    # 3차: 원본 joined 텍스트에서 매칭
    for pattern in RESIDENT_NUMBER_PATTERNS:
        match = re.search(pattern, joined)
        if match:
            digit_entries = [entry["confidence"] for entry in entries if re.search(r"\d", entry["text"] or "")]
            confidence = min(0.94, 0.56 + (sum(digit_entries) / len(digit_entries) if digit_entries else 0.0) * 0.3)
            return match.group(1), match.group(2), confidence

    # 4차: 보정된 joined 텍스트에서 매칭
    corrected_joined = _correct_ocr_digits(joined)
    if corrected_joined != joined:
        for pattern in RESIDENT_NUMBER_PATTERNS:
            match = re.search(pattern, corrected_joined)
            if match:
                digit_entries = [entry["confidence"] for entry in entries if re.search(r"\d", entry["text"] or "")]
                confidence = min(0.88, 0.46 + (sum(digit_entries) / len(digit_entries) if digit_entries else 0.0) * 0.3)
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

        row_tolerance = max(24.0, entry["height"] * 2.0)
        same_row = [
            other
            for other in entries[index + 1 :]
            if abs(other["center_y"] - entry["center_y"]) <= row_tolerance
            and other["left"] >= entry["right"] - 24.0
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
    resident_front6, resident_back_first1, resident_number_confidence = _extract_resident_number(entries)
    provisional_document_type, _ = _detect_document_type(
        entries,
        resident_number_present=resident_front6 is not None and resident_back_first1 is not None,
        name_present=False,
    )
    name, name_confidence = _extract_name(entries, provisional_document_type)
    document_type, document_confidence = _detect_document_type(
        entries,
        resident_number_present=resident_front6 is not None and resident_back_first1 is not None,
        name_present=name is not None,
    )
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
    document_matched = document_type in SUPPORTED_DOCUMENT_TYPES
    filtered_results = [result for result in results if result.get("document_type") == document_type]
    if not filtered_results:
        filtered_results = results

    name, name_confidence = _vote_text_field(filtered_results, "name", "name_confidence")
    name = _sanitize_name(name, document_type)
    if name is None:
        name_confidence = 0.0
    resident_number, resident_number_confidence = _vote_text_field(
        filtered_results,
        "resident_number_key",
        "resident_number_confidence",
    )

    resident_front6 = resident_number[:6] if resident_number and len(resident_number) >= 6 else None
    resident_back_first1 = resident_number[6:] if resident_number and len(resident_number) >= 7 else None

    # 부분 결과 보완: front6만 투표 결과에 있고 back1이 없으면 개별 결과에서 찾기
    if resident_front6 and not resident_back_first1:
        for result in filtered_results:
            if result.get("resident_back_first1") and result.get("resident_front6") == resident_front6:
                resident_back_first1 = result["resident_back_first1"]
                break
        if not resident_back_first1:
            for result in filtered_results:
                if result.get("resident_back_first1"):
                    resident_back_first1 = result["resident_back_first1"]
                    resident_number_confidence = min(resident_number_confidence, float(result.get("resident_number_confidence") or 0.0))
                    break

    warnings: list[str] = []
    status = SUCCESS

    if not document_matched:
        if resident_front6 or resident_back_first1 or name:
            warnings.append("문서 종류를 확실히 구분하지 못했습니다. 확인 화면에서 신분증 정보를 다시 확인해 주세요.")
            status = REVIEW_REQUIRED
        else:
            return _retake_result("주민등록증 또는 운전면허증이 가이드 안에 또렷하게 보이도록 맞춰주세요.")

    if document_confidence < 0.78:
        warnings.append("문서 종류 인식 신뢰도가 낮습니다. 신분증을 정면으로 맞춰주세요.")
        if status == SUCCESS:
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
        "documentMatched": document_matched,
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
    ocr = _get_paddle_ocr()
    parsed_results: list[dict[str, Any]] = []
    inference_failures = 0
    candidate_images: list[tuple[str, np.ndarray]] = [("full", bgr)]
    normalized_document = _normalize_document_image(bgr)
    if normalized_document is not None:
        candidate_images.insert(0, ("document", normalized_document))

    for source_name, source_image in candidate_images:
        variants = _preprocess_variants_for_ocr(source_image)
        for variant_name, image in variants:
            try:
                result = ocr.ocr(image, cls=True)
            except AIServiceError:
                raise
            except Exception:
                inference_failures += 1
                logger.exception("PaddleOCR inference failed for source=%s variant=%s", source_name, variant_name)
                continue

            entries = _flatten_ocr_entries(result)
            if not entries:
                continue

            parsed = _extract_fields_from_entries(entries)
            if parsed.get("resident_front6") and parsed.get("resident_back_first1"):
                parsed["resident_number_key"] = f'{parsed["resident_front6"]}{parsed["resident_back_first1"]}'
            elif parsed.get("resident_front6"):
                parsed["resident_number_key"] = parsed["resident_front6"]
            else:
                parsed["resident_number_key"] = None
            parsed_results.append(parsed)

    if not parsed_results:
        if inference_failures > 0:
            logger.warning(
                "ID card OCR inference failed for %s variants and produced no parsed results",
                inference_failures,
            )
            return _retake_result("신분증 인식이 불안정합니다. 신분증을 더 가까이 맞추고 그대로 유지해 주세요.")
        logger.warning("ID card OCR parsed no text entries from any preprocessing variant")
        return _retake_result("신분증에서 텍스트를 읽지 못했습니다. 신분증을 더 크게 맞추고 빛 반사를 줄여주세요.")

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

        logger.error("ID card OCR provider is not configured: provider=%s", provider)
        raise AIServiceError(
            status_code=503,
            code="OCR_UNAVAILABLE",
            message="ID card OCR provider is not configured",
        )
    finally:
        del image_raw
