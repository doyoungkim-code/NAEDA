from collections.abc import Sequence

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.models import FaceEmbedding

FRONT_POSE_SLOTS = ("front", "front_2", "front_3")


def _upsert_front_embedding(db: Session, user_id: str, embedding: list[float]) -> FaceEmbedding:
    rows = db.execute(
        select(FaceEmbedding).where(
            FaceEmbedding.user_id == user_id,
            FaceEmbedding.pose.in_(FRONT_POSE_SLOTS),
        )
    ).scalars().all()

    by_pose = {row.pose: row for row in rows}
    for slot in FRONT_POSE_SLOTS:
        if slot not in by_pose:
            row = FaceEmbedding(user_id=user_id, pose=slot, embedding=embedding)
            db.add(row)
            db.commit()
            db.refresh(row)
            return row

    # Keep exactly 3 front vectors by replacing the oldest updated slot.
    row = min(rows, key=lambda item: item.updated_at or item.created_at)
    row.embedding = embedding
    db.commit()
    db.refresh(row)
    return row


def upsert_face_embedding(db: Session, user_id: str, pose: str, embedding: list[float]) -> FaceEmbedding:
    if pose == "front":
        return _upsert_front_embedding(db, user_id, embedding)

    row = db.execute(
        select(FaceEmbedding).where(FaceEmbedding.user_id == user_id, FaceEmbedding.pose == pose)
    ).scalar_one_or_none()

    if row is None:
        row = FaceEmbedding(user_id=user_id, pose=pose, embedding=embedding)
        db.add(row)
    else:
        row.embedding = embedding

    db.commit()
    db.refresh(row)
    return row


def list_all_face_embeddings(db: Session) -> Sequence[FaceEmbedding]:
    return db.execute(select(FaceEmbedding)).scalars().all()
