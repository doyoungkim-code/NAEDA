from fastapi.testclient import TestClient

import app.api.internal_liveness as liveness_api
from app.main import app

client = TestClient(app)
AUTH_HEADER = {"X-Service-Token": "dev-internal-token"}


def test_headpose_success_includes_ai_processing_fields(monkeypatch):
    class FakeResult:
        expected_direction = "left"
        detected_direction = "left"
        matched = True
        yaw = -0.25
        pitch = 0.01
        confidence = 0.88
        fallback_used = True
        ai_status = "FALLBACK_APPLIED"
        message = "Head pose fallback used model pose estimation."

    async def fake_check_headpose(upload_file, expected_direction: str, timeout_seconds: float):
        assert expected_direction == "left"
        assert timeout_seconds > 0
        return FakeResult()

    monkeypatch.setattr(liveness_api, "check_headpose", fake_check_headpose)

    response = client.post(
        "/internal/v1/liveness/headpose/check",
        headers=AUTH_HEADER,
        data={"expectedDirection": "left"},
        files={"image": ("face.jpg", b"fake", "image/jpeg")},
    )

    data = response.json()
    assert response.status_code == 200
    assert data["matched"] is True
    assert data["fallbackUsed"] is True
    assert data["aiStatus"] == "FALLBACK_APPLIED"
    assert data["message"] == "Head pose fallback used model pose estimation."
