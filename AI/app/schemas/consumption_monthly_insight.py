from datetime import date

from pydantic import BaseModel, ConfigDict, Field


class MonthlyInsightRequest(BaseModel):
    period_start: date = Field(alias="periodStart")
    period_end: date = Field(alias="periodEnd")
    total_spending: int = Field(alias="totalSpending", ge=0)
    category_breakdown: dict[str, int] = Field(default_factory=dict, alias="categoryBreakdown")
    previous_total_spending: int | None = Field(default=None, alias="previousTotalSpending", ge=0)
    previous_category_breakdown: dict[str, int] | None = Field(default=None, alias="previousCategoryBreakdown")

    model_config = ConfigDict(populate_by_name=True)


class MonthlyInsightResponse(BaseModel):
    insights: list[str]
    model_version: str = Field(alias="modelVersion")

    model_config = ConfigDict(populate_by_name=True)
