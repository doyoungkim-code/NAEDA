# NAEDA AI Internal Server

`AI_v1`를 기준으로 정리한 **AI 전용(임베딩 추출만)** FastAPI 프로젝트다.

- 포함: `POST /internal/v1/embeddings/extract`, `GET /health`
- 제외: DB 저장, 유사도 검색, 사용자 최종 판정
- 목표: Back에서 바로 붙여 사용할 내부 API 제공

## 1. API 계약

### `POST /internal/v1/embeddings/extract`
- Content-Type: `multipart/form-data`
- Header: `X-Service-Token: <INTERNAL_SERVICE_TOKEN>`
- Body: `image` (jpg/png)

성공 응답:
```json
{
  "embedding": [0.0123, -0.031, 0.09],
  "dim": 512,
  "model": "arcface-buffalo_l",
  "faceCount": 1
}
```

실패 응답:
```json
{
  "code": "NO_FACE",
  "message": "No face detected",
  "requestId": "req-abc123"
}
```

## 2. 에러 코드

| Code | HTTP | 설명 |
|---|---:|---|
| `EMPTY_IMAGE` | 400 | 파일이 비어 있음 |
| `INVALID_IMAGE` | 400 | 이미지 디코딩 실패/요청 포맷 오류 |
| `NO_FACE` | 400 | 얼굴 미검출 |
| `MULTIPLE_FACES` | 400 | 얼굴 2개 이상 |
| `UNAUTHORIZED` | 401 | `X-Service-Token` 누락/불일치 |
| `AI_TIMEOUT` | 504 | 추론 시간 초과 |
| `AI_UNAVAILABLE` | 503 | 모델/추론 서버 오류 |

## 3. 실행 방법

```powershell
cd C:\SSAFY\S14P21D103\AI
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
Copy-Item .env.example .env
.\start.ps1
```

- Swagger: `http://localhost:8000/docs`
- Health: `http://localhost:8000/health`
- Metrics: `http://localhost:8000/metrics`

`start.ps1`/`start.cmd`는 `.env`의 `APP_HOST`, `APP_PORT`, `APP_RELOAD`를 사용한다.

## 4. Docker 실행

```powershell
cd C:\SSAFY\S14P21D103\AI
docker compose up -d --build
```

- Health: `http://localhost:8000/health`
- Metrics: `http://localhost:8000/metrics`

## 5. Back 연동 테스트 예시

```powershell
curl.exe -X POST "http://localhost:8000/internal/v1/embeddings/extract" `
  -H "X-Service-Token: dev-internal-token" `
  -F "image=@C:/path/to/face.jpg"
```

Back 권장 정책:
- timeout: `2~5s` (기본 `5s`)
- retry: `5xx`/`timeout`에 한해 최대 1~2회, exponential backoff(예: 200ms, 500ms)

## 6. 환경 변수

| 이름 | 기본값 | 설명 |
|---|---|---|
| `APP_NAME` | `naeda-ai-internal` | FastAPI 앱 이름 |
| `APP_HOST` | `0.0.0.0` | 바인딩 호스트 |
| `APP_PORT` | `8000` | 바인딩 포트 |
| `APP_RELOAD` | `true` | 로컬 자동 리로드 |
| `ARCFACE_MODEL_NAME` | `buffalo_l` | InsightFace 모델명 |
| `ARCFACE_PROVIDER` | `CPUExecutionProvider` | 추론 provider |
| `AI_TIMEOUT_SECONDS` | `5.0` | 요청 타임아웃(초) |
| `INTERNAL_SERVICE_TOKEN` | `dev-internal-token` | 내부 호출 인증 토큰 |

입력 이미지 권장 가이드:
- 해상도: 긴 변 기준 `640~960px`
- 파일 크기: `200KB` 내외 (최대 500KB 권장)
- 포맷: JPEG/PNG

## 7. OpenAPI 파일

- 내부 API 스펙 파일: `openapi_internal_embeddings.yaml`

## 8. 운영/인수인계 문서

- 운영/배포/런북/에러매핑: `AI/docs/AI_운영_인수인계_가이드.md`
- 로컬 성능 베이스라인: `AI/docs/perf_baseline_2026-03-03.md`
- 동시요청 스모크 결과: `AI/docs/load_smoke_2026-03-03.md`
