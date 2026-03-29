from pydantic import BaseModel, ConfigDict, Field


class HeadPoseCheckResponse(BaseModel):
    expected_direction: str = Field(..., alias="expectedDirection")
    detected_direction: str = Field(..., alias="detectedDirection")
    matched: bool
    yaw: float
    pitch: float
    confidence: float
    fallback_used: bool = Field(alias="fallbackUsed")
    ai_status: str = Field(alias="aiStatus")
    message: str
    model_config = ConfigDict(populate_by_name=True)

