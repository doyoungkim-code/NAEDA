from fastapi import UploadFile
from starlette.datastructures import Headers

from app.core.errors import AIServiceError
from app.core.upload_validation import validate_image_bytes, validate_upload_metadata


def test_validate_upload_metadata_rejects_unsupported_type():
    upload = UploadFile(filename="face.gif", file=None, headers=Headers({"content-type": "image/gif"}))

    try:
        validate_upload_metadata(upload, 1024)
        assert False, "Expected UNSUPPORTED_IMAGE_TYPE"
    except AIServiceError as exc:
        assert exc.code == "UNSUPPORTED_IMAGE_TYPE"


def test_validate_image_bytes_rejects_oversized_payload():
    try:
        validate_image_bytes(b"12345", 4)
        assert False, "Expected IMAGE_TOO_LARGE"
    except AIServiceError as exc:
        assert exc.code == "IMAGE_TOO_LARGE"
