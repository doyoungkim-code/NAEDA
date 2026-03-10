from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
AUTH_HEADER = {"X-Service-Token": "dev-internal-token"}


def test_resident_id_ocr_requires_service_token():
    response = client.post(
        "/internal/v1/ocr/resident-id/extract",
        files={"image": ("card.jpg", b"fake", "image/jpeg")},
    )

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_resident_id_ocr_returns_mock_result(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "mock")
    monkeypatch.setenv("RESIDENT_OCR_MOCK_NAME", "홍길동")
    monkeypatch.setenv("RESIDENT_OCR_MOCK_FRONT6", "900101")
    monkeypatch.setenv("RESIDENT_OCR_MOCK_BACK1", "1")

    from app.core.config import get_settings

    get_settings.cache_clear()
    try:
        response = client.post(
            "/internal/v1/ocr/resident-id/extract",
            headers=AUTH_HEADER,
            files={"image": ("card.jpg", b"fake-image", "image/jpeg")},
        )
    finally:
        get_settings.cache_clear()

    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "홍길동"
    assert data["residentFront6"] == "900101"
    assert data["residentBackFirst1"] == "1"
    assert data["provider"] == "mock"
