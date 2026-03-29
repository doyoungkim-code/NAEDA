from fastapi import UploadFile

from app.core.errors import AIServiceError

ALLOWED_IMAGE_TYPES = {
    "image/jpeg",
    "image/png",
    "image/webp",
}


def validate_upload_metadata(upload_file: UploadFile, max_image_bytes: int) -> None:
    content_type = (upload_file.content_type or "").lower()
    if content_type not in ALLOWED_IMAGE_TYPES:
        raise AIServiceError(
            status_code=400,
            code="UNSUPPORTED_IMAGE_TYPE",
            message="Unsupported image MIME type",
        )

    declared_size = getattr(upload_file, "size", None)
    if declared_size is not None and declared_size > max_image_bytes:
        raise AIServiceError(
            status_code=413,
            code="IMAGE_TOO_LARGE",
            message="Image size exceeds the allowed limit",
        )


def validate_image_bytes(image_raw: bytes, max_image_bytes: int) -> None:
    if not image_raw:
        raise AIServiceError(status_code=400, code="EMPTY_IMAGE", message="Image file is empty")
    if len(image_raw) > max_image_bytes:
        raise AIServiceError(
            status_code=413,
            code="IMAGE_TOO_LARGE",
            message="Image size exceeds the allowed limit",
        )
