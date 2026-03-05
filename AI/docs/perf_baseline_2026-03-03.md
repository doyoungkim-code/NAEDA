# AI 성능 베이스라인 (로컬)

측정일: 2026-03-03  
환경: 로컬 Windows + CPUExecutionProvider  
API: `POST /internal/v1/embeddings/extract`  
샘플 이미지: `AI_v1/sample_single_face.jpg`

## 1. 측정 방법
- warm-up 1회 수행 후 연속 20회 요청
- 측정 지표: 평균 지연, p95 지연, max 지연, timeout 비율
- 호출 방식: FastAPI `TestClient` 인프로세스 호출

## 2. 결과
- requests: `20`
- success(200): `20`
- timeout(504): `0`
- 5xx error: `0`
- mean latency: `303.27 ms`
- p95 latency: `358.50 ms`
- max latency: `380.33 ms`
- timeout rate: `0.0%`

## 3. 해석
- 임시 목표(p95 <= 800ms, timeout < 1%) 범위 내 동작
- 단, 실제 운영 지연은 네트워크/Back 재시도/동시성에 따라 달라질 수 있음

## 4. 한계
- Back/Front 통합 경로가 없어 E2E 수치 아님
- 단일 샘플 이미지 기반이라 데이터 다양성 부족
- 동시 부하(고RPS) 조건 측정은 별도 필요
