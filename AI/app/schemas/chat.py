from enum import Enum
from typing import Literal

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: Literal["user", "assistant"] = Field(..., description="메시지 역할 (user / assistant)")
    content: str = Field(..., description="메시지 내용")


class ChatRequest(BaseModel):
    user_no: int = Field(..., description="사용자 번호")
    message: str = Field(..., min_length=1, description="사용자 메시지")
    history: list[ChatMessage] = Field(default_factory=list, description="이전 대화 이력")


class ChatResponse(BaseModel):
    reply: str = Field(..., description="챗봇 응답")
    referenced_stores: list[int] = Field(default_factory=list, description="참조된 매장 ID 목록")
    referenced_festivals: list[int] = Field(default_factory=list, description="참조된 축제 ID 목록")
