from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from sqlalchemy.orm import Session

from app.core.arcface import extract_embedding
from app.core.config import get_settings
from app.core.matcher import cosine_similarity
from app.db.repository import list_all_face_embeddings, upsert_face_embedding
from app.db.session import get_db
from app.schemas.face import (
    BatchRegisterItem,
    BatchRegisterResponse,
    Candidate,
    PoseType,
    RecognizeResponse,
    RegisterResponse,
)

router = APIRouter(prefix="/faces", tags=["faces"])


@router.post("/register", response_model=RegisterResponse)
async def register_face(
    user_id: str = Form(...),
    pose: PoseType = Form(...),
    image: UploadFile = File(...),
    db: Session = Depends(get_db),
):
    embedding = await extract_embedding(image)
    row = upsert_face_embedding(db, user_id=user_id, pose=pose, embedding=embedding)
    # Keep API response pose stable even if internal front slot is front_2/front_3.
    return RegisterResponse(id=row.id, user_id=row.user_id, pose=pose, created_at=row.created_at)


@router.post("/register/batch", response_model=BatchRegisterResponse)
async def register_face_batch(
    user_id: str = Form(...),
    front: UploadFile | None = File(default=None),
    up: UploadFile | None = File(default=None),
    down: UploadFile | None = File(default=None),
    left: UploadFile | None = File(default=None),
    right: UploadFile | None = File(default=None),
    db: Session = Depends(get_db),
):
    pose_to_file: dict[PoseType, UploadFile | None] = {
        "front": front,
        "up": up,
        "down": down,
        "left": left,
        "right": right,
    }
    selected = [(pose, file) for pose, file in pose_to_file.items() if file is not None]
    if not selected:
        raise HTTPException(status_code=400, detail="At least one pose image is required")

    items: list[BatchRegisterItem] = []
    for pose, file in selected:
        embedding = await extract_embedding(file)
        row = upsert_face_embedding(db, user_id=user_id, pose=pose, embedding=embedding)
        # Keep API response pose stable even if internal front slot is front_2/front_3.
        items.append(BatchRegisterItem(id=row.id, user_id=row.user_id, pose=pose, created_at=row.created_at))

    return BatchRegisterResponse(user_id=user_id, saved_count=len(items), items=items)


@router.post("/recognize", response_model=RecognizeResponse)
async def recognize_face(
    image: UploadFile = File(...),
    top_k: int = Form(3),
    db: Session = Depends(get_db),
):
    settings = get_settings()
    query_embedding = await extract_embedding(image)

    rows = list_all_face_embeddings(db)
    scored: list[Candidate] = []

    for row in rows:
        sim = cosine_similarity(query_embedding, row.embedding)
        scored.append(Candidate(user_id=row.user_id, pose=row.pose, similarity=sim))

    scored.sort(key=lambda x: x.similarity, reverse=True)
    top = scored[: max(1, top_k)]

    best = top[0] if top else None
    matched = bool(best and best.similarity >= settings.similarity_threshold)

    return RecognizeResponse(
        matched=matched,
        best_user_id=best.user_id if matched and best else None,
        similarity=best.similarity if best else None,
        threshold=settings.similarity_threshold,
        top_k=top,
    )
