from pydantic import BaseModel, ConfigDict, Field


class ResidentIdOcrResponse(BaseModel):
    name: str
    resident_front6: str = Field(alias="residentFront6")
    resident_back_first1: str = Field(alias="residentBackFirst1")
    provider: str

    model_config = ConfigDict(populate_by_name=True)
