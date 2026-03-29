from fastapi import APIRouter, Depends

from app.core.config import Settings, get_settings
from app.core.consumption_monthly_insight_generator import generate_monthly_insight
from app.core.metrics import INFERENCE_STATUS_TOTAL
from app.core.security import verify_internal_service_token
from app.schemas.consumption_monthly_insight import MonthlyInsightRequest, MonthlyInsightResponse

router = APIRouter(
    prefix="/internal/v1/consumption/reports/monthly",
    tags=["internal-consumption-monthly-insights"],
)


@router.post(
    "/insights",
    response_model=MonthlyInsightResponse,
    responses={
        400: {"description": "Invalid request payload"},
        401: {"description": "Invalid service token"},
        503: {"description": "AI service unavailable"},
    },
)
async def generate_consumption_monthly_insights(
    request: MonthlyInsightRequest,
    _: None = Depends(verify_internal_service_token),
    settings: Settings = Depends(get_settings),
) -> MonthlyInsightResponse:
    result = generate_monthly_insight(
        total_spending=request.total_spending,
        category_breakdown=request.category_breakdown,
        previous_total_spending=request.previous_total_spending,
        previous_category_breakdown=request.previous_category_breakdown,
    )
    INFERENCE_STATUS_TOTAL.labels(
        endpoint="consumption_monthly_insights",
        status="COMPLETED",
    ).inc()
    return MonthlyInsightResponse(
        insights=result.insights,
        model_version=settings.consumption_monthly_insight_model_version,
    )
