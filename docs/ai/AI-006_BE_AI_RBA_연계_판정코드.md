# AI-006 BE/AI 구현 설명서

## 목적
AI-006의 목표는 얼굴 매칭 결과를 RBA 규칙에 연계해 최종 인증 판정코드(`authLevel`)를 생성하는 것이다.

## 최종 흐름
1. Front가 `POST /api/v1/face/search`로 얼굴 이미지와 금액(`amount`) 전송
2. BE가 AI 임베딩 추출 API 호출
3. BE가 저장 임베딩과 유사도 비교 후 `MATCH/AMBIGUOUS/NO_MATCH` 산출
4. BE가 `RbaEngine`으로 최종 인증 레벨(`authLevel`) 결정
5. BE가 상태/점수/RBA 결과를 한 번에 응답

## AI 변경 사항
- 파일
  - `AI/app/core/arcface.py`
  - `AI/app/api/internal_embeddings.py`
  - `AI/app/schemas/embedding.py`
- 내용
  - 임베딩 응답에 아래 필드 포함
    - `qualityScore`
    - `yaw`, `pitch`, `roll`

## BE 변경 사항
- 파일
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/service/FaceService.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/controller/FaceController.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/dto/response/SearchResponse.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/rba/service/RbaEngine.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/rba/dto/AuthLevel.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/rba/dto/RbaResult.java`
  - `naeda/src/main/resources/application.yaml`
- 내용
  - `search` API에 `amount` 파라미터 추가
  - Face 상태(`MATCH/AMBIGUOUS/NO_MATCH`) + 금액으로 RBA 판정
  - 응답에 RBA 결과 필드 추가
    - `authLevel`
    - `requiredMethods`
    - `blocked`
    - `rbaReason`

## RBA 규칙
- `NO_MATCH`
  - `authLevel=BLOCKED`
  - `requiredMethods=[]`
- `AMBIGUOUS`
  - 기본: `authLevel=FACE_PHONE`, `requiredMethods=[FACE, PHONE]`
  - 고액(`amount >= rba.high-amount`)이면 `authLevel=FACE_SIGNATURE`, `requiredMethods=[FACE, PHONE, SIGNATURE]`
- `MATCH`
  - 기본: `authLevel=FACE_ONLY`, `requiredMethods=[FACE]`
  - 고액이면 `authLevel=FACE_SIGNATURE`, `requiredMethods=[FACE, SIGNATURE]`

## 설정
- `face.threshold.match=0.7`
- `face.threshold.ambiguous=0.65`
- `rba.high-amount=50000`

## 응답 예시
```json
{
  "matched": false,
  "status": "AMBIGUOUS",
  "nextAction": "REQUIRE_SECOND_FACTOR",
  "bestUserId": "user-1001",
  "similarity": 0.68,
  "matchThreshold": 0.7,
  "ambiguousThreshold": 0.65,
  "qualityScore": 0.93,
  "yaw": 1.1,
  "pitch": -0.7,
  "roll": 0.2,
  "authLevel": "FACE_PHONE",
  "requiredMethods": ["FACE", "PHONE"],
  "blocked": false,
  "rbaReason": "애매한 매칭 구간: PHONE 2차 인증 필요 (similarity=0.68, amount=30000)",
  "candidates": [
    {"userId": "user-1001", "pose": "front1", "similarity": 0.68}
  ]
}
```

## 검증
- `naeda` 전체 테스트 통과
  - 실행: `./gradlew.bat test`
