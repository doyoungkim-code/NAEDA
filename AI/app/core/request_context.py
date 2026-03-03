from uuid import uuid4

from fastapi import Request


def ensure_request_id(request: Request) -> str:
    request_id = request.headers.get("X-Request-Id") or request.headers.get("X-Request-ID")
    if not request_id:
        request_id = f"req-{uuid4().hex[:12]}"
    request.state.request_id = request_id
    return request_id


def get_request_id(request: Request) -> str:
    existing = getattr(request.state, "request_id", None)
    if existing:
        return existing
    return ensure_request_id(request)
