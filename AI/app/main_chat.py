"""챗봇 전용 경량 서버 (insightface 등 무거운 의존성 없이 실행)"""

from fastapi import FastAPI

from app.api.chat import router as chat_router
from app.core.config import get_settings

settings = get_settings()
app = FastAPI(title="NAEDA Chatbot")
app.include_router(chat_router)


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok"}
