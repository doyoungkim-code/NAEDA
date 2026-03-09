from fastapi import APIRouter, Depends

from app.core.config import Settings, get_settings
from app.core.consumption_category_classifier import classify_consumption_transaction
from app.core.metrics import INFERENCE_FALLBACK_TOTAL, INFERENCE_STATUS_TOTAL
from app.core.security import verify_internal_service_token
from app.schemas.consumption_category import (
    ConsumptionCategoryClassificationBatchRequest,
    ConsumptionCategoryClassificationBatchResponse,
    ConsumptionCategoryClassificationResult,
)

router = APIRouter(
    prefix="/internal/v1/consumption/categories",
    tags=["internal-consumption-categories"],
)


@router.post(
    "/classify",
    response_model=ConsumptionCategoryClassificationBatchResponse,
    responses={
        400: {"description": "Invalid request payload"},
        401: {"description": "Invalid service token"},
        503: {"description": "AI service unavailable"},
    },
)
async def classify_consumption_categories(
    request: ConsumptionCategoryClassificationBatchRequest,
    _: None = Depends(verify_internal_service_token),
    settings: Settings = Depends(get_settings),
) -> ConsumptionCategoryClassificationBatchResponse:
    results: list[ConsumptionCategoryClassificationResult] = []

    for transaction in request.transactions:
        classified = classify_consumption_transaction(
            transaction_id=transaction.transaction_id,
            merchant_name=transaction.merchant_name,
            raw_category=transaction.raw_category,
            memo=transaction.memo,
            amount=transaction.amount,
        )
        if classified.fallback_used:
            INFERENCE_FALLBACK_TOTAL.labels(endpoint="consumption_categories_classify").inc()
        results.append(
            ConsumptionCategoryClassificationResult(
                transaction_id=classified.transaction_id,
                ai_category=classified.ai_category,
                confidence=classified.confidence,
                fallback_used=classified.fallback_used,
            )
        )

    INFERENCE_STATUS_TOTAL.labels(
        endpoint="consumption_categories_classify",
        status="COMPLETED",
    ).inc()

    return ConsumptionCategoryClassificationBatchResponse(
        results=results,
        model_version=settings.consumption_category_model_version,
    )
