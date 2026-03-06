from fastapi.testclient import TestClient

import app.api.internal_embeddings as embeddings_api
from app.core.errors import AIServiceError
from app.main import app

client = TestClient(app)
AUTH_HEADER = {"X-Service-Token": "dev-internal-token"}


def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_extract_requires_service_token():
    response = client.post(
        "/internal/v1/embeddings/extract",
        files={"image": ("face.jpg", b"fake", "image/jpeg")},
    )

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_extract_rejects_invalid_service_token():
    response = client.post(
        "/internal/v1/embeddings/extract",
        headers={"X-Service-Token": "wrong-token"},
        files={"image": ("face.jpg", b"fake", "image/jpeg")},
    )

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_extract_empty_image_payload():
    response = client.post(
        "/internal/v1/embeddings/extract",
        headers=AUTH_HEADER,
        files={"image": ("face.jpg", b"", "image/jpeg")},
    )

    assert response.status_code == 400
    assert response.json()["code"] == "EMPTY_IMAGE"


def test_extract_invalid_image_format():
    response = client.post(
        "/internal/v1/embeddings/extract",
        headers=AUTH_HEADER,
        files={"image": ("face.jpg", b"not-a-valid-image", "image/jpeg")},
    )

    assert response.status_code == 400
    assert response.json()["code"] == "INVALID_IMAGE"


def test_extract_success(monkeypatch):
    async def fake_extract_embedding(_, timeout_seconds: float) -> dict:
        assert timeout_seconds > 0
        return {
            "embedding": [0.1] * 512,
            "quality_score": 0.97,
            "yaw": 0.01,
            "pitch": -0.02,
            "roll": 0.0,
            "fallback_used": False,
            "ai_status": "COMPLETED",
            "message": "Primary inference succeeded.",
        }

    monkeypatch.setattr(embeddings_api, "extract_embedding", fake_extract_embedding)

    response = client.post(
        "/internal/v1/embeddings/extract",
        headers=AUTH_HEADER,
        files={"image": ("face.jpg", b"fake", "image/jpeg")},
    )

    data = response.json()
    assert response.status_code == 200
    assert data["dim"] == 512
    assert data["faceCount"] == 1
    assert data["model"] == "arcface-buffalo_l"
    assert len(data["embedding"]) == 512
    assert data["fallbackUsed"] is False
    assert data["aiStatus"] == "COMPLETED"
    assert data["message"] == "Primary inference succeeded."


def test_extract_propagates_ai_error(monkeypatch):
    async def fake_extract_embedding(_, timeout_seconds: float) -> list[float]:
        assert timeout_seconds > 0
        raise AIServiceError(status_code=400, code="NO_FACE", message="No face detected")

    monkeypatch.setattr(embeddings_api, "extract_embedding", fake_extract_embedding)

    response = client.post(
        "/internal/v1/embeddings/extract",
        headers=AUTH_HEADER,
        files={"image": ("face.jpg", b"fake", "image/jpeg")},
    )

    data = response.json()
    assert response.status_code == 400
    assert data["code"] == "NO_FACE"
    assert data["message"] == "No face detected"
    assert data["requestId"].startswith("req-")


def test_extract_propagates_multiple_faces_error(monkeypatch):
    async def fake_extract_embedding(_, timeout_seconds: float) -> list[float]:
        assert timeout_seconds > 0
        raise AIServiceError(status_code=400, code="MULTIPLE_FACES", message="Multiple faces detected")

    monkeypatch.setattr(embeddings_api, "extract_embedding", fake_extract_embedding)

    response = client.post(
        "/internal/v1/embeddings/extract",
        headers=AUTH_HEADER,
        files={"image": ("face.jpg", b"fake", "image/jpeg")},
    )

    assert response.status_code == 400
    assert response.json()["code"] == "MULTIPLE_FACES"


def test_extract_propagates_timeout_error(monkeypatch):
    async def fake_extract_embedding(_, timeout_seconds: float) -> list[float]:
        assert timeout_seconds > 0
        raise AIServiceError(status_code=504, code="AI_TIMEOUT", message="AI request timeout")

    monkeypatch.setattr(embeddings_api, "extract_embedding", fake_extract_embedding)

    response = client.post(
        "/internal/v1/embeddings/extract",
        headers=AUTH_HEADER,
        files={"image": ("face.jpg", b"fake", "image/jpeg")},
    )

    assert response.status_code == 504
    assert response.json()["code"] == "AI_TIMEOUT"


def test_extract_invalid_request_payload():
    response = client.post(
        "/internal/v1/embeddings/extract",
        headers=AUTH_HEADER,
    )

    data = response.json()
    assert response.status_code == 400
    assert data["code"] == "INVALID_IMAGE"


def test_request_id_is_echoed_in_header_and_body():
    request_id = "req-custom-header-1"
    response = client.post(
        "/internal/v1/embeddings/extract",
        headers={"X-Service-Token": "wrong-token", "X-Request-Id": request_id},
        files={"image": ("face.jpg", b"fake", "image/jpeg")},
    )

    assert response.status_code == 401
    assert response.headers["X-Request-Id"] == request_id
    assert response.json()["requestId"] == request_id


def test_metrics_endpoint_exposes_prometheus_metrics():
    response = client.get("/metrics")
    assert response.status_code == 200
    assert "ai_requests_total" in response.text
    assert "ai_request_duration_seconds" in response.text
