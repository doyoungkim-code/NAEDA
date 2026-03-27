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


def _make_fake_paddle(ocr_return):
    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            pass

        def ocr(self, image, cls=True):
            return ocr_return

    fake_module.PaddleOCR = FakePaddleOCR
    return fake_module


def _setup(monkeypatch, ocr_return):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")
    fake = _make_fake_paddle(ocr_return)
    monkeypatch.setitem(sys.modules, "paddleocr", fake)

    from app.core.config import get_settings
    from app.core import resident_ocr

    get_settings.cache_clear()
    resident_ocr._get_paddle_ocr.cache_clear()
    return get_settings, resident_ocr


def _teardown(get_settings, resident_ocr):
    get_settings.cache_clear()
    resident_ocr._get_paddle_ocr.cache_clear()
    sys.modules.pop("paddleocr", None)


def _post_extract():
    return client.post(
        "/internal/v1/ocr/id-card/extract",
        headers=AUTH_HEADER,
        files={"image": ("card.png", valid_png_bytes(), "image/png")},
    )


def test_requires_service_token():
    response = client.post(
        "/internal/v1/ocr/id-card/extract",
        files={"image": ("card.jpg", b"fake", "image/jpeg")},
    )
    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_rejects_mock_provider(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "mock")
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
    assert response.status_code == 503
    assert response.json()["code"] == "OCR_UNAVAILABLE"


def test_driver_license_full_extraction(monkeypatch):
    gs, ro = _setup(monkeypatch, [[
        [[[0, 0], [40, 0], [40, 10], [0, 10]], ("운전면허증", 0.99)],
        [[[0, 20], [60, 20], [60, 30], [0, 30]], ("성명 홍길동", 0.95)],
        [[[0, 40], [80, 40], [80, 50], [0, 50]], ("900101-1******", 0.88)],
    ]])
    try:
        response = _post_extract()
    finally:
        _teardown(gs, ro)

    assert response.status_code == 200
    data = response.json()
    assert data["documentType"] == "DRIVER_LICENSE"
    assert data["name"] == "홍길동"
    assert data["residentFront6"] == "900101"
    assert data["residentBackFirst1"] == "1"
    assert data["provider"] == "paddleocr"
    assert data["confidence"] > 0.0
    assert data["documentConfidence"] > 0.0
    assert data["nameConfidence"] > 0.0
    assert data["residentNumberConfidence"] > 0.0


def test_driver_license_number_does_not_override_resident_number(monkeypatch):
    gs, ro = _setup(monkeypatch, [[
        [[[0, 0], [40, 0], [40, 10], [0, 10]], ("운전면허증", 0.99)],
        [[[0, 20], [90, 20], [90, 30], [0, 30]], ("면허번호 11-12-123456-12", 0.97)],
        [[[0, 40], [60, 40], [60, 50], [0, 50]], ("성명 홍길동", 0.95)],
        [[[0, 60], [90, 60], [90, 70], [0, 70]], ("900101-1******", 0.92)],
    ]])
    try:
        response = _post_extract()
    finally:
        _teardown(gs, ro)

    assert response.status_code == 200
    data = response.json()
    assert data["documentType"] == "DRIVER_LICENSE"
    assert data["name"] == "홍길동"
    assert data["residentFront6"] == "900101"
    assert data["residentBackFirst1"] == "1"


def test_resident_id_split_name(monkeypatch):
    gs, ro = _setup(monkeypatch, [[
        [[[0, 0], [70, 0], [70, 10], [0, 10]], ("주민등록증", 0.99)],
        [[[0, 20], [20, 20], [20, 30], [0, 30]], ("성명", 0.97)],
        [[[24, 20], [36, 20], [36, 30], [24, 30]], ("홍", 0.95)],
        [[[40, 20], [70, 20], [70, 30], [40, 30]], ("길동", 0.95)],
        [[[0, 40], [90, 40], [90, 50], [0, 50]], ("900101-1******", 0.96)],
    ]])
    try:
        response = _post_extract()
    finally:
        _teardown(gs, ro)

    assert response.status_code == 200
    data = response.json()
    assert data["documentType"] == "RESIDENT_ID"
    assert data["name"] == "홍길동"
    assert data["residentFront6"] == "900101"
    assert data["residentBackFirst1"] == "1"


def test_prefers_name_over_region(monkeypatch):
    gs, ro = _setup(monkeypatch, [[
        [[[0, 0], [70, 0], [70, 10], [0, 10]], ("주민등록증", 0.99)],
        [[[0, 18], [80, 18], [80, 28], [0, 28]], ("서울특별시", 0.94)],
        [[[0, 36], [20, 36], [20, 46], [0, 46]], ("성명", 0.96)],
        [[[24, 36], [70, 36], [70, 46], [24, 46]], ("김민수", 0.95)],
        [[[0, 56], [90, 56], [90, 66], [0, 66]], ("900101-1******", 0.96)],
    ]])
    try:
        response = _post_extract()
    finally:
        _teardown(gs, ro)

    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "김민수"


def test_no_name_returns_review(monkeypatch):
    gs, ro = _setup(monkeypatch, [[
        [[[0, 0], [60, 0], [60, 10], [0, 10]], ("주민등록", 0.99)],
        [[[0, 22], [90, 22], [90, 32], [0, 32]], ("900101-1******", 0.96)],
    ]])
    try:
        response = _post_extract()
    finally:
        _teardown(gs, ro)

    assert response.status_code == 200
    data = response.json()
    assert data["name"] is None
    assert data["residentFront6"] == "900101"
    assert data["residentBackFirst1"] == "1"
    assert data["warnings"]


def test_no_text_returns_retake(monkeypatch):
    gs, ro = _setup(monkeypatch, [])
    try:
        response = _post_extract()
    finally:
        _teardown(gs, ro)

    assert response.status_code == 200
    data = response.json()
    assert data["extractionStatus"] == "RETAKE_REQUIRED"
    assert data["warnings"]


def test_inference_error_returns_retake(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")
    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            pass

        def ocr(self, image, cls=True):
            raise RuntimeError("inference failure")

    fake_module.PaddleOCR = FakePaddleOCR
    monkeypatch.setitem(sys.modules, "paddleocr", fake_module)

    from app.core.config import get_settings
    from app.core import resident_ocr

    get_settings.cache_clear()
    resident_ocr._get_paddle_ocr.cache_clear()
    try:
        response = _post_extract()
    finally:
        get_settings.cache_clear()
        resident_ocr._get_paddle_ocr.cache_clear()
        sys.modules.pop("paddleocr", None)

    assert response.status_code == 200
    data = response.json()
    assert data["extractionStatus"] == "RETAKE_REQUIRED"


def test_unknown_doc_type_with_fields_returns_review(monkeypatch):
    gs, ro = _setup(monkeypatch, [[
        [[[0, 0], [20, 0], [20, 10], [0, 10]], ("성명", 0.97)],
        [[[24, 0], [70, 0], [70, 10], [24, 10]], ("김민수", 0.94)],
        [[[0, 24], [90, 24], [90, 34], [0, 34]], ("900101-1******", 0.95)],
    ]])
    try:
        response = _post_extract()
    finally:
        _teardown(gs, ro)

    assert response.status_code == 200
    data = response.json()
    assert data["documentType"] is None
    assert data["name"] == "김민수"
    assert data["residentFront6"] == "900101"
    assert data["extractionStatus"] == "REVIEW_REQUIRED"
