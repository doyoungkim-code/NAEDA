from datetime import datetime
from typing import Literal

from pydantic import BaseModel


PoseType = Literal["front", "up", "down", "left", "right"]


class RegisterResponse(BaseModel):
    id: int
    user_id: str
    pose: PoseType
    created_at: datetime


class BatchRegisterItem(BaseModel):
    id: int
    user_id: str
    pose: PoseType
    created_at: datetime


class BatchRegisterResponse(BaseModel):
    user_id: str
    saved_count: int
    items: list[BatchRegisterItem]


class Candidate(BaseModel):
    user_id: str
    pose: str
    similarity: float


class RecognizeResponse(BaseModel):
    matched: bool
    best_user_id: str | None
    similarity: float | None
    threshold: float
    top_k: list[Candidate]
