from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class ConsumptionCategoryClassificationItem(BaseModel):
    transaction_id: str = Field(alias="transactionId")
    merchant_name: str | None = Field(default=None, alias="merchantName")
    raw_category: str | None = Field(default=None, alias="rawCategory")
    memo: str | None = None
    amount: int | None = None
    transacted_at: datetime | None = Field(default=None, alias="transactedAt")

    model_config = ConfigDict(populate_by_name=True)


class ConsumptionCategoryClassificationBatchRequest(BaseModel):
    transactions: list[ConsumptionCategoryClassificationItem] = Field(min_length=1)

    model_config = ConfigDict(populate_by_name=True)


class ConsumptionCategoryClassificationResult(BaseModel):
    transaction_id: str = Field(alias="transactionId")
    ai_category: str = Field(alias="aiCategory")
    confidence: float
    fallback_used: bool = Field(alias="fallbackUsed")

    model_config = ConfigDict(populate_by_name=True)


class ConsumptionCategoryClassificationBatchResponse(BaseModel):
    results: list[ConsumptionCategoryClassificationResult]
    model_version: str = Field(alias="modelVersion")

    model_config = ConfigDict(populate_by_name=True)
