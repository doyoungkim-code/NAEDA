# AI-018 AI 서버 폴백 전략

## 목적

AI 서버 일시 장애나 지연이 발생해도 얼굴 인식 기능이 완전히 깨지지 않도록 AI와 BE에 공통 폴백 규칙을 추가한다.

이번 작업 범위:

- AI 서버: 재시도와 제한적 fallback 수행
- BE: AI 처리 상태를 서비스 응답으로 정리
- FE: 이번 작업에서 수정하지 않지만, 바로 사용할 수 있는 응답 필드 제공

## 설계 요약

### 1. AI 서버

AI 서버는 `1차 복구`를 담당한다.

- 임베딩 추출:
  - 일시적인 `AI_UNAVAILABLE`, `AI_TIMEOUT` 상황이면 설정값 기준으로 재시도한다.
  - 재시도 전에 FaceAnalyzer 캐시를 초기화해서 모델 상태 꼬임을 줄인다.
- 헤드포즈 검증:
  - 일시 장애 시 재시도한다.
  - 기존 랜드마크 기반 계산이 불가능하면 `face.pose` 기반 방향 추정으로 fallback 한다.

성공 응답에는 아래 메타데이터를 같이 포함한다.

- `fallbackUsed`: fallback 또는 재시도 복구가 사용되었는지
- `aiStatus`: `COMPLETED` 또는 `FALLBACK_APPLIED`
- `message`: 사람이 읽을 수 있는 처리 메시지

### 2. BE

BE는 `2차 안정화`를 담당한다.

- AI 성공 응답의 메타데이터를 `aiProcessing`으로 정리해서 FE에 전달한다.
- AI timeout/unavailable 예외는 FE가 바로 분기할 수 있게 확장된 오류 응답으로 반환한다.
- 오류 응답에는 `aiStatus`, `retryable`를 포함한다.

즉 BE는 AI 내부 상태를 서비스 API 형식으로 번역하는 레이어다.

## 실제 적용 내용

### AI 성공 응답

#### 얼굴 임베딩 추출

`POST /internal/v1/embeddings/extract`

```json
{
  "embedding": [0.1, 0.2],
  "dim": 512,
  "model": "arcface-buffalo_l",
  "faceCount": 1,
  "qualityScore": 0.97,
  "yaw": 0.01,
  "pitch": -0.02,
  "roll": 0.0,
  "fallbackUsed": false,
  "aiStatus": "COMPLETED",
  "message": "Primary inference succeeded."
}
```

#### 헤드포즈 검증

`POST /internal/v1/liveness/headpose/check`

```json
{
  "expectedDirection": "left",
  "detectedDirection": "left",
  "matched": true,
  "yaw": -0.25,
  "pitch": 0.01,
  "confidence": 0.88,
  "fallbackUsed": true,
  "aiStatus": "FALLBACK_APPLIED",
  "message": "Head pose fallback used model pose estimation."
}
```

### BE 성공 응답

#### 얼굴 검색 예시

`POST /api/v1/face/search`

```json
{
  "matched": true,
  "status": "MATCH",
  "nextAction": "PASS",
  "bestUserId": "user-1001",
  "similarity": 0.91,
  "matchThreshold": 0.7,
  "ambiguousThreshold": 0.65,
  "qualityScore": 0.93,
  "yaw": 0.02,
  "pitch": -0.01,
  "roll": 0.0,
  "authLevel": "FACE_ONLY",
  "requiredMethods": ["FACE"],
  "blocked": false,
  "rbaReason": "ok",
  "candidates": [],
  "aiProcessing": {
    "aiStatus": "COMPLETED",
    "fallbackUsed": false,
    "retryable": false,
    "message": "Primary inference succeeded."
  }
}
```

#### 얼굴 등록 예시

`POST /api/v1/face/enroll`

```json
{
  "success": true,
  "userId": "user-1001",
  "pose": "front1",
  "savedAt": "2026-03-06T11:00:00",
  "aiProcessing": {
    "aiStatus": "FALLBACK_APPLIED",
    "fallbackUsed": true,
    "retryable": false,
    "message": "Primary inference recovered after retry."
  }
}
```

#### 헤드포즈 검증 예시

`POST /api/v1/face/liveness/headpose/check`

```json
{
  "expectedDirection": "left",
  "detectedDirection": "left",
  "matched": true,
  "yaw": -0.25,
  "pitch": 0.01,
  "confidence": 0.88,
  "aiProcessing": {
    "aiStatus": "FALLBACK_APPLIED",
    "fallbackUsed": true,
    "retryable": false,
    "message": "Head pose fallback used model pose estimation."
  }
}
```

### BE 오류 응답

AI 지연 또는 장애 시:

```json
{
  "code": "AI_TIMEOUT",
  "message": "AI 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.",
  "aiStatus": "FAILED_RETRYABLE",
  "retryable": true
}
```

또는

```json
{
  "code": "AI_UNAVAILABLE",
  "message": "AI 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.",
  "aiStatus": "FAILED_RETRYABLE",
  "retryable": true
}
```

## FE 사용 가이드

FE는 AI 내부 로직을 알 필요 없이 아래 필드만 보고 분기하면 된다.

### 성공 응답

- `aiProcessing.aiStatus === "COMPLETED"`
  - 정상 결과 표시
- `aiProcessing.aiStatus === "FALLBACK_APPLIED"`
  - 결과는 표시하되, 보조 문구로 대체 처리 여부 안내
- `aiProcessing.fallbackUsed === true`
  - "일부 대체 로직이 적용된 결과입니다" 같은 안내 가능

### 오류 응답

- `retryable === true`
  - 다시 시도 버튼 노출
- `aiStatus === "FAILED_RETRYABLE"`
  - "지연되고 있습니다" 또는 "잠시 후 다시 시도" UX 노출

## FE 권장 UX

- 얼굴 검색 성공 + `FALLBACK_APPLIED`
  - 결과는 먼저 보여주고, 작은 안내 문구만 추가
- 헤드포즈 검증 실패 + `retryable=true`
  - 현재 단계 유지
  - 안내 문구 노출
  - 다시 촬영 버튼 활성화
- 등록/검색 timeout
  - "AI 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요."
  - 재시도 버튼 제공

## 설정값

AI 서버 설정:

- `AI_TIMEOUT_SECONDS`
- `AI_RETRY_COUNT`
- `AI_RETRY_BACKOFF_MS`

기본값:

- timeout: `5s`
- retry count: `1`
- retry backoff: `150ms`

## 파일 변경 위치

- `AI/app/core/arcface.py`
- `AI/app/core/headpose.py`
- `AI/app/api/internal_embeddings.py`
- `AI/app/api/internal_liveness.py`
- `naeda/src/main/java/com/ssafy/naeda/domain/face/client/AiClient.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/face/service/FaceService.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/face/dto/response/*`
- `naeda/src/main/java/com/ssafy/naeda/global/exception/*`

## 검증 결과

- AI pytest 통과
- BE Gradle 테스트 통과
