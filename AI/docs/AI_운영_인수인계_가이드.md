# AI 운영/인수인계 가이드

기준일: 2026-03-03  
대상: `C:\SSAFY\S14P21D103\AI` (FastAPI 내부 임베딩 API)

## 1. 서비스 범위
- AI 책임: `이미지 -> 얼굴 임베딩(512)` 추출
- Back 책임: 등록/저장/유사도 검색/최종 판정
- 원본 이미지: 디스크 저장 금지 (요청 처리 중 메모리에서만 사용)

## 2. Back 연동 기준

### 2.1 API
- Endpoint: `POST /internal/v1/embeddings/extract`
- Content-Type: `multipart/form-data`
- Body: `image`
- Header: `X-Service-Token`

### 2.2 에러 매핑 가이드 (AI -> Back 도메인)
| AI code | HTTP | Back 권장 매핑 |
|---|---:|---|
| `EMPTY_IMAGE` | 400 | `FACE_INVALID_INPUT` |
| `INVALID_IMAGE` | 400 | `FACE_INVALID_INPUT` |
| `NO_FACE` | 400 | `FACE_NOT_FOUND` |
| `MULTIPLE_FACES` | 400 | `FACE_NOT_SINGLE` |
| `UNAUTHORIZED` | 401 | `INTERNAL_AUTH_FAILED` |
| `AI_TIMEOUT` | 504 | `FACE_AI_TIMEOUT` |
| `AI_UNAVAILABLE` | 503 | `FACE_AI_UNAVAILABLE` |

### 2.3 Timeout/Retry 권장값
- AI timeout: `2~5s` (현재 기본값 `5s`)
- retry 대상: `503`, `504`만 재시도
- retry 횟수: 최대 `1~2회`
- backoff: `200ms -> 500ms` exponential
- circuit breaker: 연속 실패 시 짧은 open 상태 권장

## 3. 성능/SLA 기준 (임시안)
Front/Back 미구현 상태이므로 아래는 임시 운영 기준값이다.
- p95 latency 목표: `<= 800ms` (warm 상태, 단일 얼굴 입력)
- timeout rate 목표: `< 1%`
- error rate 목표: `< 0.5%` (입력오류 제외)
- 초기 처리량 가이드: `5~10 RPS`부터 시작해 단계적으로 상향

## 4. 입력 이미지 가이드
- 포맷: JPEG/PNG
- 권장 해상도: 긴 변 `640~960px`
- 권장 용량: `200KB` 내외 (최대 `500KB` 권장)
- 너무 큰 이미지는 지연 증가/타임아웃 위험이 커짐

## 5. 병목 포인트
- 디코딩: 고해상도 이미지일수록 `cv2.imdecode` 비용 증가
- 추론: CPUExecutionProvider에서 동시 요청 시 지연 급증 가능
- 네트워크: Back 재시도/대용량 multipart 전송 시 end-to-end 지연 증가

## 6. 보안/개인정보 원칙
- 원본 이미지 파일 저장 금지
- 임베딩/원본 이미지 payload 로그 금지
- 내부 API 인증: `X-Service-Token` 필수
- 운영 환경 권장: 내부망 + TLS (가능하면 mTLS)
- 시크릿(`INTERNAL_SERVICE_TOKEN`)은 배포 환경 시크릿 스토어 사용 권장

## 7. 헬스체크/레디니스 기준
- Liveness: `GET /health`가 200이면 프로세스 생존
- Readiness:
  - `/health` 200
  - 인증/추론 경로 기본 동작 확인 (`/internal/v1/embeddings/extract` 샘플 요청)
  - 모델 로딩 실패 로그가 없을 것

## 8. 배포/롤백 절차

### 8.1 Docker 배포
1. `docker compose up -d --build`
2. `GET /health` 확인
3. 샘플 이미지로 `POST /internal/v1/embeddings/extract` 확인

### 8.2 롤백
1. 직전 이미지 태그로 컨테이너 재기동
2. `/health` 및 샘플 추론 재검증
3. 장애 시간대의 에러코드/지연 지표 검토

## 9. 모델 버전 규칙
- 응답 `model` 필드 형식: `arcface-<model_name>`
- 예: `arcface-buffalo_l`
- 모델 교체 시:
  - OpenAPI/README 업데이트
  - 정확도/지연 비교 리포트 첨부
  - 배포 태그에 모델 버전 포함 권장

## 10. 모니터링/알람 기준
현재 `/metrics` 엔드포인트 제공.
- 핵심 지표:
  - `ai_requests_total`
  - `ai_request_duration_seconds` (히스토그램)
- 권장 알람:
  - 5분 에러율(`5xx`) > 2%
  - 5분 p95 latency > 2s
  - timeout(`AI_TIMEOUT`) 급증

## 11. 에러 대응 Runbook
- `EMPTY_IMAGE`/`INVALID_IMAGE`: Back 입력 검증 로직 및 Front 인코딩 확인
- `NO_FACE`: 촬영 거리/각도/조도 재안내, 프레임 드롭 정책 점검
- `MULTIPLE_FACES`: 단일 얼굴 프레임 유도 UI 적용
- `UNAUTHORIZED`: 서비스 토큰 회전/주입 상태 확인
- `AI_TIMEOUT`: 이미지 크기 축소, timeout 상향 여부 검토, 재시도 정책 적용
- `AI_UNAVAILABLE`: 모델 파일/런타임/리소스(CPU, 메모리) 점검 후 재기동

## 12. 인수인계 산출물
- OpenAPI: `openapi_internal_embeddings.yaml`
- 환경 변수 예시: `.env.example`
- 실행 스크립트: `start.ps1`, `start.cmd`, `docker-compose.yml`
- 운영 가이드/Runbook: 본 문서

## 13. 현재 한계 (미구현 영역)
- Front/Back 미구현으로 E2E(등록/검색) 검증 불가
- 실제 평가셋 기반 FAR/FRR/TPR/FPR 리포트 미작성
- 조직 단위 RBAC/감사로그/TLS 운영 검증은 통합 단계에서 수행 필요
