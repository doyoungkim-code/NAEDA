from pydantic import BaseModel, ConfigDict, Field


class ResidentIdOcrResponse(BaseModel):
    document_type: str | None = Field(alias="documentType", default=None)
    document_matched: bool = Field(alias="documentMatched")
    name: str | None = None
    resident_front6: str | None = Field(alias="residentFront6", default=None)
    resident_back_first1: str | None = Field(alias="residentBackFirst1", default=None)
    provider: str
    confidence: float
    document_confidence: float = Field(alias="documentConfidence")
    name_confidence: float = Field(alias="nameConfidence")
    resident_number_confidence: float = Field(alias="residentNumberConfidence")
    extraction_status: str = Field(alias="extractionStatus")
    warnings: list[str] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)
