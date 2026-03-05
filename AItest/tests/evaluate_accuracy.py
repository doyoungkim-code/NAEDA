import argparse
import csv
import json
from dataclasses import dataclass
from pathlib import Path
import sys

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from fastapi.testclient import TestClient
from sqlalchemy import delete

from app.main import app
from app.db.models import FaceEmbedding
from app.db.session import SessionLocal

IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
POSE_HINTS = ("front", "up", "down", "left", "right")


@dataclass
class ProbeResult:
    user_id: str
    image_path: str
    best_user_id: str | None
    similarity: float
    is_correct_top1: int
    error: str | None


def infer_pose(file_path: Path) -> str:
    stem = file_path.stem.lower()
    for pose in POSE_HINTS:
        if pose in stem:
            return pose
    return "front"


def list_images(root: Path) -> list[Path]:
    return sorted([p for p in root.rglob("*") if p.is_file() and p.suffix.lower() in IMAGE_EXTS])


def sweep_metrics(rows: list[ProbeResult]) -> dict:
    valid = [r for r in rows if r.error is None]
    if not valid:
        return {
            "threshold_metrics": [],
            "best_threshold_by_acc": None,
            "eer": None,
            "roc_auc": None,
        }

    thresholds = [i / 100 for i in range(0, 101)]
    scores = [r.similarity for r in valid]
    labels = [r.is_correct_top1 for r in valid]

    threshold_metrics = []
    best_acc = -1.0
    best_thr = None

    for thr in thresholds:
        tp = fp = tn = fn = 0
        for score, label in zip(scores, labels):
            accept = score >= thr
            if label == 1 and accept:
                tp += 1
            elif label == 1 and not accept:
                fn += 1
            elif label == 0 and accept:
                fp += 1
            else:
                tn += 1

        tpr = tp / (tp + fn) if (tp + fn) else 0.0
        fpr = fp / (fp + tn) if (fp + tn) else 0.0
        frr = fn / (tp + fn) if (tp + fn) else 0.0
        far = fpr
        acc = (tp + tn) / (tp + tn + fp + fn) if (tp + tn + fp + fn) else 0.0

        threshold_metrics.append(
            {
                "threshold": thr,
                "accuracy": round(acc, 6),
                "far": round(far, 6),
                "frr": round(frr, 6),
                "tpr": round(tpr, 6),
                "fpr": round(fpr, 6),
            }
        )
        if acc > best_acc:
            best_acc = acc
            best_thr = thr

    eer = None
    min_gap = 10.0
    for m in threshold_metrics:
        gap = abs(m["far"] - m["frr"])
        if gap < min_gap:
            min_gap = gap
            eer = round((m["far"] + m["frr"]) / 2, 6)

    # Trapezoidal ROC-AUC on sorted FPR
    roc_points = sorted([(m["fpr"], m["tpr"]) for m in threshold_metrics], key=lambda x: x[0])
    auc = 0.0
    for i in range(1, len(roc_points)):
        x1, y1 = roc_points[i - 1]
        x2, y2 = roc_points[i]
        auc += (x2 - x1) * (y1 + y2) * 0.5

    best_row = next((m for m in threshold_metrics if m["threshold"] == best_thr), None)
    return {
        "threshold_metrics": threshold_metrics,
        "best_threshold_by_acc": best_row,
        "eer": eer,
        "roc_auc": round(auc, 6),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate face recognition accuracy using local API handlers")
    parser.add_argument("--dataset-dir", required=True, help="Dataset root containing gallery/ and probe/")
    parser.add_argument("--output-dir", default="eval_results", help="Directory to save reports")
    parser.add_argument("--top-k", type=int, default=3)
    parser.add_argument("--keep-db", action="store_true", help="Do not clear DB before evaluation")
    args = parser.parse_args()

    dataset_dir = Path(args.dataset_dir).resolve()
    gallery_root = dataset_dir / "gallery"
    probe_root = dataset_dir / "probe"

    if not gallery_root.exists() or not probe_root.exists():
        raise SystemExit("dataset must contain 'gallery/' and 'probe/' subdirectories")

    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    if not args.keep_db:
        with SessionLocal() as db:
            db.execute(delete(FaceEmbedding))
            db.commit()

    register_total = register_success = register_fail = 0
    probe_total = 0
    results: list[ProbeResult] = []

    with TestClient(app) as client:
        # Register gallery
        for user_dir in sorted([p for p in gallery_root.iterdir() if p.is_dir()]):
            user_id = user_dir.name
            for img_path in list_images(user_dir):
                register_total += 1
                pose = infer_pose(img_path)
                with img_path.open("rb") as f:
                    res = client.post(
                        "/faces/register",
                        data={"user_id": user_id, "pose": pose},
                        files={"image": (img_path.name, f, "image/jpeg")},
                    )
                if res.status_code == 200:
                    register_success += 1
                else:
                    register_fail += 1

        # Probe
        for user_dir in sorted([p for p in probe_root.iterdir() if p.is_dir()]):
            user_id = user_dir.name
            for img_path in list_images(user_dir):
                probe_total += 1
                with img_path.open("rb") as f:
                    res = client.post(
                        "/faces/recognize",
                        data={"top_k": str(args.top_k)},
                        files={"image": (img_path.name, f, "image/jpeg")},
                    )

                if res.status_code != 200:
                    results.append(
                        ProbeResult(
                            user_id=user_id,
                            image_path=str(img_path),
                            best_user_id=None,
                            similarity=0.0,
                            is_correct_top1=0,
                            error=res.json().get("detail", "unknown error"),
                        )
                    )
                    continue

                payload = res.json()
                top_k = payload.get("top_k", [])
                best_user_id = top_k[0]["user_id"] if top_k else None
                similarity = float(top_k[0]["similarity"]) if top_k else 0.0
                is_correct = 1 if best_user_id == user_id else 0

                results.append(
                    ProbeResult(
                        user_id=user_id,
                        image_path=str(img_path),
                        best_user_id=best_user_id,
                        similarity=similarity,
                        is_correct_top1=is_correct,
                        error=None,
                    )
                )

    valid = [r for r in results if r.error is None]
    failures = [r for r in results if r.error is not None]
    top1_acc = (sum(r.is_correct_top1 for r in valid) / len(valid)) if valid else 0.0

    sweep = sweep_metrics(results)

    summary = {
        "dataset_dir": str(dataset_dir),
        "gallery_registered_total": register_total,
        "gallery_register_success": register_success,
        "gallery_register_fail": register_fail,
        "probe_total": probe_total,
        "probe_valid": len(valid),
        "probe_fail": len(failures),
        "top1_accuracy": round(top1_acc, 6),
        "best_threshold_by_acc": sweep["best_threshold_by_acc"],
        "eer": sweep["eer"],
        "roc_auc": sweep["roc_auc"],
    }

    summary_path = output_dir / "summary.json"
    detail_csv_path = output_dir / "probe_results.csv"
    threshold_csv_path = output_dir / "threshold_metrics.csv"

    summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    with detail_csv_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=["user_id", "image_path", "best_user_id", "similarity", "is_correct_top1", "error"],
        )
        writer.writeheader()
        for r in results:
            writer.writerow(
                {
                    "user_id": r.user_id,
                    "image_path": r.image_path,
                    "best_user_id": r.best_user_id,
                    "similarity": r.similarity,
                    "is_correct_top1": r.is_correct_top1,
                    "error": r.error,
                }
            )

    with threshold_csv_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=["threshold", "accuracy", "far", "frr", "tpr", "fpr"],
        )
        writer.writeheader()
        for row in sweep["threshold_metrics"]:
            writer.writerow(row)

    print("Evaluation complete")
    print(json.dumps(summary, indent=2))
    print(f"Saved: {summary_path}")
    print(f"Saved: {detail_csv_path}")
    print(f"Saved: {threshold_csv_path}")


if __name__ == "__main__":
    main()
