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

# ── 상수 ──────────────────────────────────────────────────────────────
SUPPORTED_DOCUMENT_TYPES = {"RESIDENT_ID", "DRIVER_LICENSE"}
DOCUMENT_KEYWORDS = {
    "RESIDENT_ID": ("주민등록증", "주민등록", "민등록증"),
    "DRIVER_LICENSE": ("운전면허증", "운전면허", "면허증"),
}
DOCUMENT_HINT_KEYWORDS = {
    "RESIDENT_ID": ("현주소", "등록기준지", "세대주", "세대원", "주소지"),
    "DRIVER_LICENSE": ("면허번호", "적성검사", "경찰청", "1종", "2종", "보통", "대형"),
}
NAME_LABELS = ("성명", "이름")
NAME_STOPWORDS = (
    "주민등록증", "운전면허", "운전면허증", "성명", "이름", "기간", "경찰청",
    "면허", "번호", "특별시", "광역시", "자치시", "자치도",
    "경기도", "강원도", "충청", "전라", "경상", "제주",
    "대한민국", "발급", "주소", "현주소", "시장", "청장", "구청장",
    "세대주", "세대원", "보통", "대형", "소형", "특수", "원동기",
    "종보통", "종대형", "종소형", "면허번호", "적성검사",
)
RESIDENT_NUMBER_PATTERNS = (
    r"(\d{6})\s*[-\-ㅡ—~]?\s*([1-4])[\d\*xX●•Oo]{6}",
    r"(\d{6})\s*[-\-ㅡ—~]?\s*([1-4])",
)
NAME_DIRECT_RE = re.compile(r"(?:성명|이름)\s*[:：]?\s*([가-힣\s]{2,10})")
SUCCESS = "SUCCESS"
REVIEW_REQUIRED = "REVIEW_REQUIRED"
RETAKE_REQUIRED = "RETAKE_REQUIRED"


# ── 유틸 ──────────────────────────────────────────────────────────────
def _strip(value: str | None) -> str:
    return re.sub(r"\s+", "", value or "")


def _clean_hangul(value: str | None) -> str:
    return _strip(re.sub(r"[^가-힣\s]", " ", value or ""))


def _is_name(value: str) -> bool:
    return bool(re.fullmatch(r"[가-힣]{2,5}", value)) and not any(s in value for s in NAME_STOPWORDS)


def _is_name_piece(value: str) -> bool:
    return bool(re.fullmatch(r"[가-힣]{1,5}", value)) and not any(s in value for s in NAME_STOPWORDS)


def _has_resident_number(text: str) -> bool:
    return any(re.search(p, text or "") for p in RESIDENT_NUMBER_PATTERNS)


# ── 이미지 처리 ───────────────────────────────────────────────────────
def _decode_image(image_raw: bytes) -> np.ndarray:
    arr = np.frombuffer(image_raw, dtype=np.uint8)
    bgr = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if bgr is None:
        raise AIServiceError(status_code=400, code="INVALID_IMAGE", message="Invalid image format")
    return bgr


def _resize(image: np.ndarray, target: int = 1200) -> np.ndarray:
    h, w = image.shape[:2]
    longest = max(h, w)
    if longest < 600 or longest > 3000:
        scale = target / float(longest)
        return cv2.resize(image, (max(1, int(w * scale)), max(1, int(h * scale))),
                          interpolation=cv2.INTER_CUBIC if scale > 1 else cv2.INTER_AREA)
    return image


def _preprocess_variants(bgr: np.ndarray) -> list[np.ndarray]:
    """원본 + CLAHE + 적응형 이진화 총 3가지 변형 생성."""
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)

    clahe = cv2.createCLAHE(clipLimit=2.5, tileGridSize=(8, 8)).apply(gray)

    blurred = cv2.GaussianBlur(gray, (3, 3), 0)
    thresh = cv2.adaptiveThreshold(blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                   cv2.THRESH_BINARY, 31, 9)

    return [
        _resize(bgr),
        _resize(cv2.cvtColor(clahe, cv2.COLOR_GRAY2BGR)),
        _resize(cv2.cvtColor(thresh, cv2.COLOR_GRAY2BGR)),
    ]


# ── PaddleOCR 초기화 ──────────────────────────────────────────────────
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
        raise AIServiceError(status_code=503, code="OCR_UNAVAILABLE",
                             message="PaddleOCR is not installed") from exc
    try:
        return PaddleOCR(use_angle_cls=True, lang=settings.resident_ocr_paddle_lang, show_log=False)
    except Exception as exc:
        logger.exception("PaddleOCR initialization failed")
        raise AIServiceError(status_code=503, code="OCR_UNAVAILABLE",
                             message="PaddleOCR initialization failed") from exc


# ── OCR 결과 파싱 ─────────────────────────────────────────────────────
def _flatten_ocr(result) -> list[dict[str, Any]]:
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
            conf = float(line[1][1] or 0.0)
            if not text:
                continue
            box = line[0] if line[0] else []
            xs = [float(p[0]) for p in box if isinstance(p, (list, tuple)) and len(p) >= 2]
            ys = [float(p[1]) for p in box if isinstance(p, (list, tuple)) and len(p) >= 2]
            entries.append({
                "text": text,
                "conf": conf,
                "left": min(xs) if xs else 0.0,
                "right": max(xs) if xs else 0.0,
                "top": min(ys) if ys else 0.0,
                "bottom": max(ys) if ys else 0.0,
                "cy": ((min(ys) + max(ys)) / 2.0) if ys else 0.0,
                "h": (max(ys) - min(ys)) if ys else 0.0,
            })
    entries.sort(key=lambda e: (round(e["top"] / 10.0), e["left"]))
    return entries


# ── 문서 유형 감지 ────────────────────────────────────────────────────
def _detect_doc_type(entries: list[dict[str, Any]]) -> tuple[str | None, float]:
    joined = " ".join(e["text"] for e in entries)
    best_type, best_conf = None, 0.0

    for doc_type, keywords in DOCUMENT_KEYWORDS.items():
        for kw in keywords:
            if kw in joined:
                confs = [e["conf"] for e in entries if kw in e["text"]]
                c = max(confs) if confs else 0.5
                score = min(0.99, 0.7 + c * 0.25)
                if score > best_conf:
                    best_type, best_conf = doc_type, score

    if best_type is None:
        for doc_type, hints in DOCUMENT_HINT_KEYWORDS.items():
            hits = sum(1 for h in hints if h in joined)
            if hits >= 2:
                score = min(0.8, 0.3 + hits * 0.1)
                if score > best_conf:
                    best_type, best_conf = doc_type, score

    return best_type, best_conf


# ── 주민번호 추출 ─────────────────────────────────────────────────────
def _extract_resident_number(entries: list[dict[str, Any]]) -> tuple[str | None, str | None, float]:
    # 개별 entry에서 먼저 시도
    for entry in entries:
        for pattern in RESIDENT_NUMBER_PATTERNS:
            m = re.search(pattern, entry["text"])
            if m:
                return m.group(1), m.group(2), min(0.99, 0.6 + entry["conf"] * 0.35)

    # 전체 텍스트 합쳐서 시도
    joined = " ".join(e["text"] for e in entries)
    for pattern in RESIDENT_NUMBER_PATTERNS:
        m = re.search(pattern, joined)
        if m:
            avg_conf = sum(e["conf"] for e in entries) / max(1, len(entries))
            return m.group(1), m.group(2), min(0.9, 0.5 + avg_conf * 0.3)

    return None, None, 0.0


# ── 이름 추출 ─────────────────────────────────────────────────────────
def _extract_name(entries: list[dict[str, Any]]) -> tuple[str | None, float]:
    # 1. "성명: 홍길동" 직접 패턴
    for e in entries:
        m = NAME_DIRECT_RE.search(e["text"])
        if m:
            name = _clean_hangul(m.group(1))
            if _is_name(name):
                return name, min(0.99, 0.5 + e["conf"] * 0.4)

    # 2. "성명" 라벨 옆 같은 줄에서 추출
    for i, e in enumerate(entries):
        if not any(label in e["text"] for label in NAME_LABELS):
            continue
        row_tol = max(20.0, e["h"] * 2.0)
        same_row = [
            o for o in entries[i + 1:]
            if abs(o["cy"] - e["cy"]) <= row_tol
            and o["left"] >= e["right"] - 20.0
            and not _has_resident_number(o["text"])
        ]
        # 개별 조각 시도
        for o in same_row[:4]:
            name = _clean_hangul(o["text"])
            if _is_name(name):
                return name, min(0.99, 0.45 + o["conf"] * 0.4)
        # 조각 합치기
        pieces = [_clean_hangul(o["text"]) for o in same_row[:3] if _is_name_piece(_clean_hangul(o["text"]))]
        if pieces:
            joined = "".join(pieces)
            if _is_name(joined):
                avg = sum(o["conf"] for o in same_row[:len(pieces)]) / len(pieces)
                return joined, min(0.95, 0.4 + avg * 0.35)

    # 3. 주민번호 위쪽 근처에서 찾기
    rn_idx = next((i for i, e in enumerate(entries) if _has_resident_number(e["text"])), None)
    if rn_idx is not None:
        for e in reversed(entries[max(0, rn_idx - 4):rn_idx]):
            name = _clean_hangul(e["text"])
            if _is_name(name):
                return name, min(0.9, 0.3 + e["conf"] * 0.35)

    # 4. 전체 entry 스캔
    for e in entries:
        name = _clean_hangul(e["text"])
        if _is_name(name):
            return name, min(0.8, 0.2 + e["conf"] * 0.3)

    return None, 0.0


# ── 단일 이미지 OCR 실행 및 필드 추출 ─────────────────────────────────
def _ocr_and_extract(ocr, image: np.ndarray) -> dict[str, Any] | None:
    try:
        result = ocr.ocr(image, cls=True)
    except AIServiceError:
        raise
    except Exception:
        logger.exception("PaddleOCR inference failed")
        return None

    entries = _flatten_ocr(result)
    if not entries:
        return None

    doc_type, doc_conf = _detect_doc_type(entries)
    front6, back1, rn_conf = _extract_resident_number(entries)
    name, name_conf = _extract_name(entries)

    return {
        "doc_type": doc_type,
        "doc_conf": doc_conf,
        "front6": front6,
        "back1": back1,
        "rn_conf": rn_conf,
        "name": name,
        "name_conf": name_conf,
    }


# ── 최적 결과 선택 ────────────────────────────────────────────────────
def _score_result(r: dict[str, Any]) -> float:
    s = 0.0
    if r.get("doc_type") in SUPPORTED_DOCUMENT_TYPES:
        s += 3.0
    if r.get("front6"):
        s += 2.5
    if r.get("back1"):
        s += 1.5
    if r.get("name"):
        s += 2.0
    s += (r.get("doc_conf") or 0) + (r.get("rn_conf") or 0) + (r.get("name_conf") or 0)
    return s


def _best_result(results: list[dict[str, Any]]) -> dict[str, Any]:
    return max(results, key=_score_result)


# ── 최종 응답 구성 ────────────────────────────────────────────────────
def _build_response(r: dict[str, Any]) -> dict[str, Any]:
    doc_type = r.get("doc_type")
    doc_conf = r.get("doc_conf") or 0.0
    doc_matched = doc_type in SUPPORTED_DOCUMENT_TYPES
    front6 = r.get("front6")
    back1 = r.get("back1")
    rn_conf = r.get("rn_conf") or 0.0
    name = r.get("name")
    name_conf = r.get("name_conf") or 0.0

    # 이름 최종 검증
    if name and not _is_name(name):
        name, name_conf = None, 0.0

    warnings: list[str] = []
    status = SUCCESS

    if not doc_matched:
        if front6 or back1 or name:
            warnings.append("문서 종류를 확실히 구분하지 못했습니다. 확인 화면에서 신분증 정보를 다시 확인해 주세요.")
            status = REVIEW_REQUIRED
        else:
            return {
                "documentType": None, "documentMatched": False,
                "name": None, "residentFront6": None, "residentBackFirst1": None,
                "provider": "paddleocr", "confidence": 0.0,
                "documentConfidence": 0.0, "nameConfidence": 0.0,
                "residentNumberConfidence": 0.0,
                "extractionStatus": RETAKE_REQUIRED,
                "warnings": ["주민등록증 또는 운전면허증이 가이드 안에 또렷하게 보이도록 맞춰주세요."],
            }

    if front6 is None or back1 is None:
        warnings.append("주민등록번호를 인식하지 못했습니다. 다시 촬영해 주세요.")
        status = RETAKE_REQUIRED
    elif rn_conf < 0.7:
        warnings.append("주민등록번호 인식이 불안정합니다. 확인 화면에서 다시 확인해 주세요.")
        if status == SUCCESS:
            status = REVIEW_REQUIRED

    if not name:
        warnings.append("이름을 인식하지 못했습니다. 확인 화면에서 직접 입력해 주세요.")
        if status == SUCCESS:
            status = REVIEW_REQUIRED
    elif name_conf < 0.7:
        warnings.append("이름 인식 신뢰도가 낮습니다. 확인 화면에서 확인해 주세요.")
        if status == SUCCESS:
            status = REVIEW_REQUIRED

    confs = [c for c in [doc_conf, rn_conf, name_conf if name else None] if c is not None and c > 0]
    confidence = sum(confs) / len(confs) if confs else 0.0

    return {
        "documentType": doc_type,
        "documentMatched": doc_matched,
        "name": name,
        "residentFront6": front6,
        "residentBackFirst1": back1,
        "provider": "paddleocr",
        "confidence": round(confidence, 4),
        "documentConfidence": round(doc_conf, 4),
        "nameConfidence": round(name_conf, 4),
        "residentNumberConfidence": round(rn_conf, 4),
        "extractionStatus": status,
        "warnings": warnings,
    }


# ── 메인 파이프라인 ───────────────────────────────────────────────────
def _extract_with_paddle(image_raw: bytes) -> dict[str, Any]:
    bgr = _decode_image(image_raw)
    ocr = _get_paddle_ocr()
    all_results: list[dict[str, Any]] = []

    # 원본 이미지 + 전처리 변형 3종
    for variant in _preprocess_variants(bgr):
        parsed = _ocr_and_extract(ocr, variant)
        if parsed:
            all_results.append(parsed)

    # 세로 이미지(폰 세로 촬영, 신분증 가로)면 90도 회전해서도 시도
    h, w = bgr.shape[:2]
    if h > w:
        rotated = cv2.rotate(bgr, cv2.ROTATE_90_CLOCKWISE)
        for variant in _preprocess_variants(rotated):
            parsed = _ocr_and_extract(ocr, variant)
            if parsed:
                all_results.append(parsed)

    if not all_results:
        return {
            "documentType": None, "documentMatched": False,
            "name": None, "residentFront6": None, "residentBackFirst1": None,
            "provider": "paddleocr", "confidence": 0.0,
            "documentConfidence": 0.0, "nameConfidence": 0.0,
            "residentNumberConfidence": 0.0,
            "extractionStatus": RETAKE_REQUIRED,
            "warnings": ["신분증에서 텍스트를 읽지 못했습니다. 신분증을 더 크게 맞추고 빛 반사를 줄여주세요."],
        }

    best = _best_result(all_results)
    return _build_response(best)


# ── 진입점 ────────────────────────────────────────────────────────────
async def extract_resident_id_fields(upload_file: UploadFile) -> dict[str, Any]:
    settings = get_settings()
    image_raw = await upload_file.read()
    await upload_file.close()
    validate_image_bytes(image_raw, settings.ai_max_image_bytes)

    try:
        provider = settings.resident_ocr_provider.lower()
        if provider == "mock":
            raise AIServiceError(
                status_code=503, code="OCR_UNAVAILABLE",
                message="Mock OCR provider is disabled. Set RESIDENT_OCR_PROVIDER to paddleocr",
            )
        if provider == "paddleocr":
            return _extract_with_paddle(image_raw)

        logger.error("ID card OCR provider is not configured: provider=%s", provider)
        raise AIServiceError(
            status_code=503, code="OCR_UNAVAILABLE",
            message="ID card OCR provider is not configured",
        )
    finally:
        del image_raw
