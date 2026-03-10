import sys
import types

import cv2
import numpy as np
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
AUTH_HEADER = {"X-Service-Token": "dev-internal-token"}


def valid_png_bytes() -> bytes:
    image = np.zeros((8, 8, 3), dtype=np.uint8)
    success, encoded = cv2.imencode(".png", image)
    assert success
    return encoded.tobytes()


def test_resident_id_ocr_requires_service_token():
    response = client.post(
        "/internal/v1/ocr/id-card/extract",
        files={"image": ("card.jpg", b"fake", "image/jpeg")},
    )

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_resident_id_ocr_returns_mock_result(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "mock")
    monkeypatch.setenv("RESIDENT_OCR_MOCK_DOCUMENT_TYPE", "DRIVER_LICENSE")
    monkeypatch.setenv("RESIDENT_OCR_MOCK_NAME", "홍길동")
    monkeypatch.setenv("RESIDENT_OCR_MOCK_FRONT6", "900101")
    monkeypatch.setenv("RESIDENT_OCR_MOCK_BACK1", "1")
    monkeypatch.setenv("RESIDENT_OCR_MOCK_CONFIDENCE", "0.91")

    from app.core.config import get_settings

    get_settings.cache_clear()
    try:
        response = client.post(
            "/internal/v1/ocr/id-card/extract",
            headers=AUTH_HEADER,
            files={"image": ("card.png", valid_png_bytes(), "image/png")},
        )
    finally:
        get_settings.cache_clear()

    assert response.status_code == 200
    data = response.json()
    assert data["documentType"] == "DRIVER_LICENSE"
    assert data["documentMatched"] is True
    assert data["name"] == "홍길동"
    assert data["residentFront6"] == "900101"
    assert data["residentBackFirst1"] == "1"
    assert data["provider"] == "mock"
    assert data["confidence"] == 0.91


def test_resident_id_ocr_returns_paddleocr_result(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")
    monkeypatch.setenv("RESIDENT_OCR_MIN_CONFIDENCE", "0.5")

    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def ocr(self, image, cls=True):
            return [[
                [[[0, 0], [1, 0], [1, 1], [0, 1]], ("운전면허증", 0.99)],
                [[[0, 0], [1, 0], [1, 1], [0, 1]], ("성명 홍길동", 0.95)],
                [[[0, 0], [1, 0], [1, 1], [0, 1]], ("900101-1******", 0.88)],
            ]]

    fake_module.PaddleOCR = FakePaddleOCR
    monkeypatch.setitem(sys.modules, "paddleocr", fake_module)

    from app.core.config import get_settings
    from app.core import resident_ocr

    get_settings.cache_clear()
    resident_ocr._get_paddle_ocr.cache_clear()
    try:
        response = client.post(
            "/internal/v1/ocr/id-card/extract",
            headers=AUTH_HEADER,
            files={"image": ("card.png", valid_png_bytes(), "image/png")},
        )
    finally:
        get_settings.cache_clear()
        resident_ocr._get_paddle_ocr.cache_clear()
        sys.modules.pop("paddleocr", None)

    assert response.status_code == 200
    data = response.json()
    assert data["documentType"] == "DRIVER_LICENSE"
    assert data["name"] == "홍길동"
    assert data["residentFront6"] == "900101"
    assert data["residentBackFirst1"] == "1"
    assert data["provider"] == "paddleocr"
    assert data["confidence"] >= 0.5
