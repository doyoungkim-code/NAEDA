# AI-016 추론 성능 모니터링

## 목적

- AI 추론 요청의 지연 시간, 실패, 폴백 사용 여부를 운영 중에 확인할 수 있게 한다.
- AI 서버와 BE 모두에서 추론 성능을 관측할 수 있는 최소 모니터링 구조를 만든다.

## 이번 작업 범위

### AI

- 기존 `/metrics`에 추론 모니터링 메트릭 확장
- 에러 코드별 추론 실패 카운터 추가
- 엔드포인트별 추론 상태 카운터 추가
- 엔드포인트별 폴백 사용 카운터 추가

### BE

- AI 호출 결과를 메모리 기반으로 집계하는 모니터링 서비스 추가
- 내부 모니터링 조회 API 추가

## AI 메트릭

기존 메트릭:

- `ai_requests_total`
- `ai_request_duration_seconds`

추가 메트릭:

- `ai_inference_errors_total{endpoint, code}`
- `ai_inference_fallback_total{endpoint}`
- `ai_inference_status_total{endpoint, status}`

### 수집 기준

- 성공 응답 시
  - `aiStatus` 값을 상태 카운터에 기록
  - `fallbackUsed=true`면 폴백 카운터 증가
- 오류 응답 시
  - `AI_TIMEOUT`, `AI_UNAVAILABLE`, `NO_FACE`, `MULTIPLE_FACES` 등 오류 코드를 에러 카운터에 기록

## BE 모니터링

BE는 AI 호출 결과를 메모리에서 집계한다.

수집 항목:

- 총 호출 수
- 성공 호출 수
- 실패 호출 수
- 평균 지연 시간(ms)
- 엔드포인트별 호출 수
- 에러 코드별 실패 수
- 폴백 사용 수

내부 조회 API:

- `GET /api/internal/fds/monitoring/ai`

응답 예시:

```json
{
  "totalCalls": 3,
  "successCalls": 2,
  "failureCalls": 1,
  "averageLatencyMs": 200.0,
  "endpointCalls": {
    "embeddings_extract": 2,
    "headpose_check": 1
  },
  "errorCounts": {
    "headpose_check:AI_TIMEOUT": 1
  },
  "fallbackCounts": {
    "embeddings_extract": 1
  }
}
```

## 기대 효과

- AI 응답 지연 증가 여부를 빠르게 확인 가능
- 특정 오류 코드 급증 여부를 추적 가능
- 폴백 사용률이 높아지는지 운영 중 확인 가능
- FE 없이도 BE/AI만으로 추론 성능 점검 가능

## 후속 확장

- Prometheus/Grafana 연동
- p95/p99 지연 시간 시각화
- BE 모니터링 값 영속화
- FDS 경고/차단 이벤트와 성능 지표 연계
