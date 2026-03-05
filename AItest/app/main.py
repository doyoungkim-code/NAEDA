from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.face import router as face_router
from app.core.config import get_settings
from app.db.session import Base, engine


@asynccontextmanager
async def lifespan(_: FastAPI):
    # MVP convenience: create tables on startup.
    Base.metadata.create_all(bind=engine)
    yield


settings = get_settings()
app = FastAPI(title=settings.app_name, lifespan=lifespan)

app.include_router(face_router)


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok"}
