import numpy as np

from app.core import arcface
from app.core.errors import AIServiceError


class _DummyFace:
    def __init__(self, embedding: np.ndarray | None):
        self.embedding = embedding


class _DummyAnalyzer:
    def __init__(self, faces):
        self._faces = faces

    def get(self, _):
        return self._faces


def test_extract_embedding_from_bytes_normalizes_to_unit_length(monkeypatch):
    monkeypatch.setattr(arcface.cv2, "imdecode", lambda *_: np.zeros((8, 8, 3), dtype=np.uint8))
    embedding = np.ones(512, dtype=np.float32)
    monkeypatch.setattr(arcface, "get_face_analyzer", lambda: _DummyAnalyzer([_DummyFace(embedding)]))

    result = arcface._extract_embedding_from_bytes(b"fake-image-binary")
    result_arr = np.asarray(result["embedding"], dtype=np.float32)

    assert len(result["embedding"]) == 512
    assert np.isclose(np.linalg.norm(result_arr), 1.0, atol=1e-5)


def test_extract_embedding_from_bytes_raises_no_face(monkeypatch):
    monkeypatch.setattr(arcface.cv2, "imdecode", lambda *_: np.zeros((8, 8, 3), dtype=np.uint8))
    monkeypatch.setattr(arcface, "get_face_analyzer", lambda: _DummyAnalyzer([]))

    try:
        arcface._extract_embedding_from_bytes(b"fake-image-binary")
        assert False, "Expected NO_FACE error"
    except AIServiceError as exc:
        assert exc.code == "NO_FACE"
        assert exc.status_code == 400


def test_extract_embedding_from_bytes_raises_multiple_faces(monkeypatch):
    monkeypatch.setattr(arcface.cv2, "imdecode", lambda *_: np.zeros((8, 8, 3), dtype=np.uint8))
    faces = [_DummyFace(np.ones(512, dtype=np.float32)), _DummyFace(np.ones(512, dtype=np.float32))]
    monkeypatch.setattr(arcface, "get_face_analyzer", lambda: _DummyAnalyzer(faces))

    try:
        arcface._extract_embedding_from_bytes(b"fake-image-binary")
        assert False, "Expected MULTIPLE_FACES error"
    except AIServiceError as exc:
        assert exc.code == "MULTIPLE_FACES"
        assert exc.status_code == 400


def test_extract_embedding_from_bytes_rejects_dimension_mismatch(monkeypatch):
    monkeypatch.setattr(arcface.cv2, "imdecode", lambda *_: np.zeros((8, 8, 3), dtype=np.uint8))
    monkeypatch.setattr(
        arcface, "get_face_analyzer", lambda: _DummyAnalyzer([_DummyFace(np.ones(256, dtype=np.float32))])
    )

    try:
        arcface._extract_embedding_from_bytes(b"fake-image-binary")
        assert False, "Expected AI_UNAVAILABLE error"
    except AIServiceError as exc:
        assert exc.code == "AI_UNAVAILABLE"
        assert exc.status_code == 503
