from pydantic import BaseModel, ConfigDict, Field


class EmbeddingExtractResponse(BaseModel):
    embedding: list[float]
    dim: int
    model: str
    face_count: int = Field(alias="faceCount")
    model_config = ConfigDict(populate_by_name=True)


class ErrorResponse(BaseModel):
    code: str
    message: str
    request_id: str = Field(alias="requestId")
    model_config = ConfigDict(populate_by_name=True)
