# NAEDA AI Server (ArcFace + PostgreSQL)

MVP server for face embedding registration and recognition.

## 1. Setup

```bash
cd AI
docker compose up -d
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
```

## 2. Run

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Swagger: `http://localhost:8000/docs`
Health: `http://localhost:8000/health`

## 3. API

- `POST /faces/register`
  - form-data: `user_id`, `pose(front/up/down/left/right)`, `image`
  - behavior: creates ArcFace embedding and upserts one row per `(user_id, pose)`

- `POST /faces/register/batch`
  - form-data: `user_id` and any of `front`, `up`, `down`, `left`, `right` image files
  - behavior: creates embeddings for provided poses and upserts rows per `(user_id, pose)`

- `POST /faces/recognize`
  - form-data: `image`, optional `top_k`
  - behavior: compares query embedding with all registered embeddings and returns best candidate

## 4. Quick Test (PowerShell)

```powershell
curl.exe -X POST "http://localhost:8000/faces/register" `
  -F "user_id=user1" `
  -F "pose=front" `
  -F "image=@C:/path/to/front.jpg"

curl.exe -X POST "http://localhost:8000/faces/register/batch" `
  -F "user_id=user1" `
  -F "front=@C:/path/to/front.jpg" `
  -F "up=@C:/path/to/up.jpg" `
  -F "down=@C:/path/to/down.jpg" `
  -F "left=@C:/path/to/left.jpg" `
  -F "right=@C:/path/to/right.jpg"

curl.exe -X POST "http://localhost:8000/faces/register" `
  -F "user_id=user1" `
  -F "pose=left" `
  -F "image=@C:/path/to/left.jpg"

curl.exe -X POST "http://localhost:8000/faces/recognize" `
  -F "image=@C:/path/to/probe.jpg" `
  -F "top_k=3"
```

## Notes

- Current recognition is brute-force over all rows (good for MVP).
- Current embedding storage type is PostgreSQL `float[]` for local compatibility.
- For production scale, install `pgvector` extension and switch to vector similarity query + ANN index (HNSW or IVF).
- This server stores embeddings only, not raw images.

## Accuracy Evaluation

Use this folder layout:

```text
dataset/
  gallery/
    user_001/
      front.jpg
      left.jpg
      right.jpg
  probe/
    user_001/
      p1.jpg
      p2.jpg
```

Run:

```bash
python tests/evaluate_accuracy.py --dataset-dir /path/to/dataset --output-dir eval_results
```

Outputs:
- `summary.json`: top1 accuracy, EER, ROC-AUC, register/probe success stats
- `probe_results.csv`: per-probe prediction detail
- `threshold_metrics.csv`: threshold sweep metrics (accuracy/FAR/FRR/TPR/FPR)
