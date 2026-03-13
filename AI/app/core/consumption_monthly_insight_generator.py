from __future__ import annotations

from dataclasses import dataclass


_CATEGORY_ADVICE = {
    "카페": {
        "dominant": "이번 달 카페 지출이 많았어요. 커피 한두 잔만 줄여도 절약 효과가 분명해요.",
        "increase": "이번 달 카페 지출이 지난달보다 늘었어요. 자주 가는 커피 소비를 한 번 점검해보세요.",
    },
    "식비": {
        "dominant": "이번 달 식비 비중이 높았어요. 외식 횟수를 조금만 줄여도 부담을 낮출 수 있어요.",
        "increase": "이번 달 식비가 지난달보다 늘었어요. 배달이나 외식 빈도를 한 번 돌아보세요.",
    },
    "교통": {
        "dominant": "이번 달 교통비가 크게 나갔어요. 반복되는 이동 비용을 한 번 점검해보세요.",
        "increase": "이번 달 교통비가 지난달보다 늘었어요. 고정 이동비를 줄일 여지가 있는지 살펴보세요.",
    },
    "쇼핑": {
        "dominant": "이번 달 쇼핑 지출이 많았어요. 다음 구매 전에는 꼭 필요한 지출인지 한 번만 더 확인해보세요.",
        "increase": "이번 달 쇼핑 지출이 지난달보다 늘었어요. 계획 구매만 해도 소비 흐름이 훨씬 안정돼요.",
    },
    "마트": {
        "dominant": "이번 달 마트 지출이 가장 컸어요. 장보기 전에 목록을 정하면 불필요한 지출을 줄이기 좋아요.",
        "increase": "이번 달 마트 지출이 지난달보다 늘었어요. 정기 구매 품목을 한 번 정리해보세요.",
    },
}

_GENERIC_DOMINANT_TEMPLATE = "이번 달 {category} 지출이 가장 컸어요. 자주 반복되는 소비인지 한 번 점검해보세요."
_GENERIC_INCREASE_TEMPLATE = "이번 달 {category} 지출이 지난달보다 늘었어요. 반복 지출을 한 번 돌아보면 좋아요."
_GENERIC_TOTAL_INCREASE = "이번 달 전체 소비가 지난달보다 늘었어요. 큰 지출부터 먼저 점검해보세요."
_NO_SPENDING_MESSAGE = "이번 달 카드 소비 내역이 거의 없었어요. 다음 달 데이터가 쌓이면 더 정확한 조언을 드릴게요."


@dataclass(frozen=True)
class MonthlyInsightResult:
    insights: list[str]


def generate_monthly_insight(
    total_spending: int | None,
    category_breakdown: dict[str, int] | None,
    previous_total_spending: int | None,
    previous_category_breakdown: dict[str, int] | None,
) -> MonthlyInsightResult:
    current_total = max(0, int(total_spending or 0))
    current_breakdown = {
        category: int(amount)
        for category, amount in (category_breakdown or {}).items()
        if category and int(amount or 0) > 0
    }
    previous_total = max(0, int(previous_total_spending or 0))
    previous_breakdown = {
        category: int(amount)
        for category, amount in (previous_category_breakdown or {}).items()
        if category and int(amount or 0) > 0
    }

    if current_total <= 0 or not current_breakdown:
        return MonthlyInsightResult(insights=[_NO_SPENDING_MESSAGE])

    top_category, top_amount = max(current_breakdown.items(), key=lambda item: item[1])
    top_ratio = top_amount / current_total
    previous_top_amount = previous_breakdown.get(top_category, 0)

    if previous_top_amount > 0 and top_amount >= int(previous_top_amount * 1.2):
        return MonthlyInsightResult(insights=[_category_message(top_category, "increase")])

    if top_ratio >= 0.32:
        return MonthlyInsightResult(insights=[_category_message(top_category, "dominant")])

    if previous_total > 0 and current_total >= int(previous_total * 1.15):
        return MonthlyInsightResult(insights=[_GENERIC_TOTAL_INCREASE])

    return MonthlyInsightResult(insights=[_category_message(top_category, "dominant")])


def _category_message(category: str, mode: str) -> str:
    templates = _CATEGORY_ADVICE.get(category)
    if templates:
        return templates[mode]
    if mode == "increase":
        return _GENERIC_INCREASE_TEMPLATE.format(category=category)
    return _GENERIC_DOMINANT_TEMPLATE.format(category=category)
