# AI 동시 요청 스모크 테스트 (로컬)

측정일: 2026-03-03  
환경: 로컬 Windows + CPUExecutionProvider  
API: `POST /internal/v1/embeddings/extract`

## 1. 시나리오
- warm-up 1회 후 동시 요청 10개 전송
- 호출 방식: `httpx.AsyncClient` + `ASGITransport` (인프로세스)
- 입력 이미지: `AI_v1/sample_single_face.jpg`

## 2. 결과
- 동시성: `10`
- total elapsed: `1364.28 ms`
- success(200): `10`
- error: `0`
- per-request mean: `1223.70 ms`
- per-request p95: `1353.41 ms`
- per-request max: `1360.21 ms`

## 3. 해석
- 동시 10요청 스모크에서 오류 없이 처리됨
- CPU 단일 환경이라 동시성 구간에서 개별 지연이 증가함
- 운영 환경에서는 워커 수/리소스/인프라를 포함한 부하테스트 추가 필요
