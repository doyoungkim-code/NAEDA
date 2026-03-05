from pydantic import BaseModel, Field


class HeadPoseCheckResponse(BaseModel):
    expected_direction: str = Field(..., alias="expectedDirection")
    detected_direction: str = Field(..., alias="detectedDirection")
    matched: bool
    yaw: float
    pitch: float
    confidence: float

