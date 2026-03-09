from pydantic import BaseModel, ConfigDict, Field


class ModelVersionResponse(BaseModel):
    feature_version: str = Field(alias="featureVersion")
    rule_version: str = Field(alias="ruleVersion")
    model_version: str = Field(alias="modelVersion")
    algorithm: str
    artifact_path: str = Field(alias="artifactPath")
    updated_at: str = Field(alias="updatedAt")

    model_config = ConfigDict(populate_by_name=True)
