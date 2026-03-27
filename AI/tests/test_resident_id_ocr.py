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


def test_resident_id_ocr_rejects_mock_provider(monkeypatch):
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
    data = response.json()
    assert data["code"] == "OCR_UNAVAILABLE"
    assert "Mock OCR provider is disabled" in data["message"]


def test_resident_id_ocr_returns_paddleocr_result(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")
    monkeypatch.setenv("RESIDENT_OCR_MIN_CONFIDENCE", "0.5")

    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def ocr(self, image, cls=True):
            return [[
                [[[0, 0], [40, 0], [40, 10], [0, 10]], ("운전면허증", 0.99)],
                [[[0, 20], [60, 20], [60, 30], [0, 30]], ("성명 홍길동", 0.95)],
                [[[0, 40], [80, 40], [80, 50], [0, 50]], ("900101-1******", 0.88)],
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
    assert data["confidence"] > 0.0
    assert data["documentConfidence"] > 0.0
    assert data["nameConfidence"] > 0.0
    assert data["residentNumberConfidence"] > 0.0
    assert data["extractionStatus"] == "SUCCESS"
    assert data["warnings"] == []


def test_resident_id_ocr_extracts_name_from_split_resident_id_entries(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")

    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def ocr(self, image, cls=True):
            return [[
                [[[0, 0], [70, 0], [70, 10], [0, 10]], ("주민등록증", 0.99)],
                [[[0, 20], [20, 20], [20, 30], [0, 30]], ("성명", 0.97)],
                [[[24, 20], [36, 20], [36, 30], [24, 30]], ("홍", 0.95)],
                [[[40, 20], [70, 20], [70, 30], [40, 30]], ("길동", 0.95)],
                [[[0, 40], [90, 40], [90, 50], [0, 50]], ("900101-1******", 0.96)],
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
    assert data["documentType"] == "RESIDENT_ID"
    assert data["name"] == "홍길동"
    assert data["residentFront6"] == "900101"
    assert data["residentBackFirst1"] == "1"
    assert data["extractionStatus"] == "SUCCESS"


def test_resident_id_ocr_prefers_name_over_region_text(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")

    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def ocr(self, image, cls=True):
            return [[
                [[[0, 0], [70, 0], [70, 10], [0, 10]], ("주민등록증", 0.99)],
                [[[0, 18], [80, 18], [80, 28], [0, 28]], ("서울특별시", 0.94)],
                [[[0, 36], [20, 36], [20, 46], [0, 46]], ("성명", 0.96)],
                [[[24, 36], [70, 36], [70, 46], [24, 46]], ("김민수", 0.95)],
                [[[0, 56], [90, 56], [90, 66], [0, 66]], ("900101-1******", 0.96)],
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
    assert data["documentType"] == "RESIDENT_ID"
    assert data["name"] == "김민수"
    assert data["warnings"] == []


def test_resident_id_ocr_does_not_use_document_header_as_name(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")

    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def ocr(self, image, cls=True):
            return [[
                [[[0, 0], [60, 0], [60, 10], [0, 10]], ("주민등록", 0.99)],
                [[[0, 22], [90, 22], [90, 32], [0, 32]], ("900101-1******", 0.96)],
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
    assert data["documentType"] == "RESIDENT_ID"
    assert data["name"] is None
    assert data["residentFront6"] == "900101"
    assert data["residentBackFirst1"] == "1"
    assert data["extractionStatus"] == "REVIEW_REQUIRED"
    assert data["warnings"]


def test_resident_id_ocr_ignores_driver_license_class_text_for_name(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")

    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def ocr(self, image, cls=True):
            return [[
                [[[0, 0], [70, 0], [70, 10], [0, 10]], ("운전면허증", 0.99)],
                [[[0, 20], [40, 20], [40, 30], [0, 30]], ("1종보통", 0.97)],
                [[[44, 20], [82, 20], [82, 30], [44, 30]], ("성경훈", 0.95)],
                [[[0, 42], [90, 42], [90, 52], [0, 52]], ("900101-1******", 0.96)],
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
    assert data["name"] == "성경훈"
    assert data["extractionStatus"] == "REVIEW_REQUIRED"
    assert data["warnings"]


def test_resident_id_ocr_returns_retake_result_when_no_text_detected(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")

    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def ocr(self, image, cls=True):
            return []

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
    assert data["documentMatched"] is False
    assert data["documentType"] is None
    assert data["extractionStatus"] == "RETAKE_REQUIRED"
    assert data["warnings"]


def test_resident_id_ocr_returns_retake_when_all_variants_raise_inference_error(monkeypatch):
    monkeypatch.setenv("RESIDENT_OCR_PROVIDER", "paddleocr")

    fake_module = types.ModuleType("paddleocr")

    class FakePaddleOCR:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def ocr(self, image, cls=True):
            raise RuntimeError("temporary inference failure")

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
    assert data["documentMatched"] is False
    assert data["extractionStatus"] == "RETAKE_REQUIRED"
    assert data["warnings"]
