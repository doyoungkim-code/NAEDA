# Face Match API Front Usage Guide (BE+AI)

## 1) 개요
- Front는 이미지(또는 프레임 캡처 결과)를 BE `POST /api/v1/face/search`로 전송한다.
- BE는 AI 임베딩 추출 후 저장된 얼굴 벡터와 전수 비교하여 상태를 반환한다.
- 상태 기준:
  - `MATCH`: similarity >= 0.70
  - `AMBIGUOUS`: 0.65 <= similarity < 0.70 (2차 인증 필요)
  - `NO_MATCH`: similarity < 0.65

## 2) 요청
- Endpoint: `POST /api/v1/face/search`
- Content-Type: `multipart/form-data`
- Parts:
  - `image` (required): 얼굴 이미지 파일
  - `topK` (optional): 후보 개수, 미입력 시 3

예시:
```bash
curl -X POST http://localhost:8080/api/v1/face/search \
  -F "image=@./face.jpg" \
  -F "topK=3"
```

## 3) 응답 필드
- `status`: `MATCH | AMBIGUOUS | NO_MATCH`
- `nextAction`:
  - `PASS` (결제/인증 통과)
  - `REQUIRE_SECOND_FACTOR` (2차 인증 진행)
  - `RETRY_CAPTURE` (재촬영 유도)
- `bestUserId`: 최고 유사 사용자 (NO_MATCH면 null)
- `similarity`: 최고 유사도
- `matchThreshold`: 0.70
- `ambiguousThreshold`: 0.65
- `qualityScore`: AI 품질 점수(0~1)
- `yaw/pitch/roll`: AI 추정 얼굴 각도
- `candidates`: 상위 후보 목록

응답 예시:
```json
{
  "matched": false,
  "status": "AMBIGUOUS",
  "nextAction": "REQUIRE_SECOND_FACTOR",
  "bestUserId": "user-1001",
  "similarity": 0.684,
  "matchThreshold": 0.7,
  "ambiguousThreshold": 0.65,
  "qualityScore": 0.93,
  "yaw": 1.1,
  "pitch": -0.7,
  "roll": 0.2,
  "candidates": [
    {"userId": "user-1001", "pose": "front1", "similarity": 0.684}
  ]
}
```

## 4) Front 처리 규칙
- `status=MATCH`: 즉시 통과 처리.
- `status=AMBIGUOUS`: 2차 인증 화면(전화번호/PIN/추가 본인확인)으로 이동.
- `status=NO_MATCH`: "얼굴을 인식할 수 없습니다" 메시지와 함께 재촬영 유도.

## 5) 에러 처리
- `NO_FACE`: 얼굴 미검출
- `MULTIPLE_FACES`: 복수 얼굴 검출
- `EMPTY_IMAGE`, `INVALID_IMAGE`: 입력 이미지 오류
- `AI_TIMEOUT`, `AI_UNAVAILABLE`: AI 장애/타임아웃
