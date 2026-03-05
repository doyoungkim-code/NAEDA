from pydantic import BaseModel, ConfigDict, Field


class EmbeddingExtractResponse(BaseModel):
    embedding: list[float]
    dim: int
    model: str
    face_count: int = Field(alias="faceCount")
<<<<<<< HEAD
=======
    quality_score: float = Field(alias="qualityScore")
    yaw: float
    pitch: float
    roll: float
>>>>>>> b041ecc5c38cb905e9bbb0e660a2c3b3d41da422
    model_config = ConfigDict(populate_by_name=True)


class ErrorResponse(BaseModel):
    code: str
    message: str
    request_id: str = Field(alias="requestId")
    model_config = ConfigDict(populate_by_name=True)
