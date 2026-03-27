from fastapi import APIRouter, Depends, File, UploadFile

from app.core.resident_ocr import extract_resident_id_fields
from app.core.security import verify_internal_service_token
from app.schemas.resident_id_ocr import ResidentIdOcrResponse

router = APIRouter(prefix="/internal/v1/ocr/id-card", tags=["internal-id-card-ocr"])


@router.post(
    "/extract",
    response_model=ResidentIdOcrResponse,
    responses={
        400: {"description": "Invalid image or OCR extraction failed"},
        401: {"description": "Invalid service token"},
        503: {"description": "OCR service unavailable"},
    },
)
async def extract_resident_id(
    image: UploadFile = File(...),
    _: None = Depends(verify_internal_service_token),
) -> ResidentIdOcrResponse:
    result = await extract_resident_id_fields(image)
    return ResidentIdOcrResponse(
        document_type=result["documentType"],
        document_matched=result["documentMatched"],
        name=result["name"],
        resident_front6=result["residentFront6"],
        resident_back_first1=result["residentBackFirst1"],
        provider=result["provider"],
        confidence=result["confidence"],
        document_confidence=result["documentConfidence"],
        name_confidence=result["nameConfidence"],
        resident_number_confidence=result["residentNumberConfidence"],
        extraction_status=result["extractionStatus"],
        warnings=result["warnings"],
    )
