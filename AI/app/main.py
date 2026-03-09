from time import perf_counter

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, Response

from app.api.internal_embeddings import router as internal_embeddings_router
from app.api.internal_liveness import router as internal_liveness_router
from app.api.internal_model_version import router as internal_model_version_router
from app.core.config import get_settings
from app.core.errors import AIServiceError
from app.core.metrics import REQUEST_DURATION_SECONDS, REQUESTS_TOTAL, render_metrics
from app.core.request_context import ensure_request_id, get_request_id

settings = get_settings()
app = FastAPI(title=settings.app_name)
app.include_router(internal_embeddings_router)
app.include_router(internal_liveness_router)
app.include_router(internal_model_version_router)


@app.middleware("http")
async def request_id_middleware(request: Request, call_next):
    request_id = ensure_request_id(request)
    response = await call_next(request)
    response.headers["X-Request-Id"] = request_id
    return response


@app.middleware("http")
async def metrics_middleware(request: Request, call_next):
    method = request.method
    path = request.url.path
    started = perf_counter()
    response = await call_next(request)
    elapsed = perf_counter() - started

    REQUESTS_TOTAL.labels(method=method, path=path, status=str(response.status_code)).inc()
    REQUEST_DURATION_SECONDS.labels(method=method, path=path).observe(elapsed)
    return response


@app.exception_handler(AIServiceError)
async def handle_ai_service_error(request: Request, exc: AIServiceError):
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "code": exc.code,
            "message": exc.message,
            "requestId": get_request_id(request),
        },
    )


@app.exception_handler(RequestValidationError)
async def handle_validation_error(request: Request, _: RequestValidationError):
    return JSONResponse(
        status_code=400,
        content={
            "code": "INVALID_IMAGE",
            "message": "Invalid request payload",
            "requestId": get_request_id(request),
        },
    )


@app.exception_handler(Exception)
async def handle_unexpected_error(request: Request, _: Exception):
    return JSONResponse(
        status_code=503,
        content={
            "code": "AI_UNAVAILABLE",
            "message": "AI service unavailable",
            "requestId": get_request_id(request),
        },
    )


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/metrics", include_in_schema=False)
def metrics():
    payload, content_type = render_metrics()
    return Response(content=payload, media_type=content_type)
