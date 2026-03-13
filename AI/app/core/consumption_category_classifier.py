from __future__ import annotations

import re
from collections import defaultdict
from dataclasses import dataclass


_WHITESPACE_PATTERN = re.compile(r"\s+")
_TEXT_CLEAN_PATTERN = re.compile(r"[^0-9a-zA-Z가-힣/]+")

_RAW_CATEGORY_MAP = {
    "대형마트": "마트",
    "마트": "마트",
    "편의점": "생활",
    "카페": "카페",
    "디저트": "카페",
    "식비": "식비",
    "음식": "식비",
    "외식": "식비",
    "배달": "식비",
    "교통": "교통",
    "주유": "교통",
    "택시": "교통",
    "쇼핑": "쇼핑",
    "패션": "쇼핑",
    "뷰티": "쇼핑",
    "생활": "생활",
    "교육": "교육",
    "교육/육아": "교육",
    "의료": "의료",
    "병원": "의료",
    "약국": "의료",
    "통신": "주거/통신",
    "공과금": "주거/통신",
    "주거": "주거/통신",
    "문화": "문화/여가",
    "영화": "문화/여가",
    "도서": "문화/여가",
    "레저": "문화/여가",
    "숙박": "여행/숙박",
    "여행": "여행/숙박",
    "항공": "여행/숙박",
    "해외": "여행/숙박",
    "금융": "금융/보험",
    "보험": "금융/보험",
}

_CATEGORY_KEYWORDS = {
    "카페": {
        "스타벅스": 0.38,
        "투썸": 0.38,
        "이디야": 0.38,
        "메가커피": 0.38,
        "빽다방": 0.38,
        "할리스": 0.38,
        "커피빈": 0.38,
        "컴포즈": 0.38,
        "paik": 0.32,
        "coffee": 0.24,
        "cafe": 0.24,
        "카페": 0.26,
        "라떼": 0.2,
    },
    "식비": {
        "배달의민족": 0.42,
        "요기요": 0.42,
        "쿠팡이츠": 0.42,
        "맥도날드": 0.34,
        "버거킹": 0.34,
        "롯데리아": 0.34,
        "서브웨이": 0.34,
        "김밥천국": 0.3,
        "한솥": 0.3,
        "식당": 0.22,
        "치킨": 0.24,
        "피자": 0.24,
        "버거": 0.24,
        "분식": 0.24,
        "restaurant": 0.2,
    },
    "마트": {
        "이마트": 0.38,
        "홈플러스": 0.38,
        "롯데마트": 0.38,
        "코스트코": 0.38,
        "농협하나로": 0.34,
        "하나로마트": 0.34,
        "mart": 0.22,
        "마트": 0.24,
    },
    "생활": {
        "gs25": 0.34,
        "cu ": 0.26,
        "cu편의점": 0.34,
        "세븐일레븐": 0.34,
        "emart24": 0.34,
        "올리브영": 0.32,
        "다이소": 0.32,
        "편의점": 0.24,
        "생활": 0.2,
    },
    "교통": {
        "주유": 0.28,
        "s-oil": 0.34,
        "sk에너지": 0.34,
        "gs칼텍스": 0.34,
        "현대오일뱅크": 0.34,
        "카카오t": 0.34,
        "카카오택시": 0.34,
        "우버": 0.3,
        "택시": 0.26,
        "버스": 0.24,
        "지하철": 0.24,
        "철도": 0.24,
        "교통": 0.24,
    },
    "쇼핑": {
        "쿠팡": 0.34,
        "11번가": 0.34,
        "지마켓": 0.34,
        "무신사": 0.34,
        "에이블리": 0.34,
        "올리브영몰": 0.32,
        "쇼핑": 0.24,
        "fashion": 0.2,
    },
    "교육": {
        "학원": 0.32,
        "교보문고": 0.28,
        "yes24": 0.28,
        "유치원": 0.28,
        "어린이집": 0.28,
        "교육": 0.24,
        "육아": 0.24,
    },
    "의료": {
        "병원": 0.34,
        "의원": 0.34,
        "약국": 0.34,
        "치과": 0.34,
        "한의원": 0.34,
        "의료": 0.24,
    },
    "주거/통신": {
        "통신": 0.28,
        "skt": 0.34,
        "kt ": 0.26,
        "lg u+": 0.34,
        "관리비": 0.3,
        "전기": 0.26,
        "가스": 0.26,
        "수도": 0.26,
        "월세": 0.3,
    },
    "문화/여가": {
        "cgv": 0.34,
        "메가박스": 0.34,
        "롯데시네마": 0.34,
        "넷플릭스": 0.34,
        "spotify": 0.3,
        "yes24티켓": 0.32,
        "영화": 0.24,
        "도서": 0.24,
        "공연": 0.24,
        "여가": 0.22,
    },
    "여행/숙박": {
        "야놀자": 0.38,
        "여기어때": 0.38,
        "agoda": 0.34,
        "booking": 0.34,
        "항공": 0.28,
        "숙박": 0.28,
        "여행": 0.28,
        "해외": 0.24,
    },
    "금융/보험": {
        "보험": 0.3,
        "현대해상": 0.34,
        "삼성화재": 0.34,
        "kb손해보험": 0.34,
        "이자": 0.24,
        "수수료": 0.22,
    },
}

_CATEGORY_PRIORITY = [
    "카페",
    "식비",
    "마트",
    "생활",
    "교통",
    "쇼핑",
    "교육",
    "의료",
    "주거/통신",
    "문화/여가",
    "여행/숙박",
    "금융/보험",
    "기타",
]

_CATEGORY_RANK = {category: index for index, category in enumerate(_CATEGORY_PRIORITY)}


@dataclass(frozen=True)
class ClassifiedConsumptionTransaction:
    transaction_id: str
    ai_category: str
    confidence: float
    fallback_used: bool


def _normalize_text(*values: str | None) -> str:
    text = " ".join(value for value in values if value).strip().lower()
    text = _TEXT_CLEAN_PATTERN.sub(" ", text)
    return _WHITESPACE_PATTERN.sub(" ", text).strip()


def _normalize_category_key(raw_category: str | None) -> str:
    return _WHITESPACE_PATTERN.sub(" ", (raw_category or "").strip().lower())


def _map_raw_category(raw_category: str | None) -> str | None:
    mapped = _RAW_CATEGORY_MAP.get((raw_category or "").strip())
    if mapped:
        return mapped

    normalized_key = _normalize_category_key(raw_category)
    for source, target in _RAW_CATEGORY_MAP.items():
        if normalized_key == source.lower():
            return target
    return None


def classify_consumption_transaction(
    transaction_id: str,
    merchant_name: str | None,
    raw_category: str | None,
    memo: str | None,
    amount: int | None,
) -> ClassifiedConsumptionTransaction:
    del amount

    normalized_text = _normalize_text(merchant_name, memo)
    raw_category_match = _map_raw_category(raw_category)

    scores: dict[str, float] = defaultdict(float)
    matched_keyword_counts: dict[str, int] = defaultdict(int)

    if raw_category_match:
        scores[raw_category_match] += 0.34

    for category, keyword_weights in _CATEGORY_KEYWORDS.items():
        for keyword, weight in keyword_weights.items():
            if keyword.lower() in normalized_text:
                scores[category] += weight
                matched_keyword_counts[category] += 1

    if not scores:
        return ClassifiedConsumptionTransaction(
            transaction_id=transaction_id,
            ai_category="기타",
            confidence=0.31,
            fallback_used=True,
        )

    best_category, best_score = max(
        scores.items(),
        key=lambda item: (item[1], -_CATEGORY_RANK.get(item[0], len(_CATEGORY_PRIORITY))),
    )
    fallback_used = matched_keyword_counts.get(best_category, 0) == 0
    confidence_floor = 0.25 if fallback_used else 0.35
    confidence = round(min(0.98, confidence_floor + best_score), 2)

    return ClassifiedConsumptionTransaction(
        transaction_id=transaction_id,
        ai_category=best_category,
        confidence=confidence,
        fallback_used=fallback_used,
    )
