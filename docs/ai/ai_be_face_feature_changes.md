# AI/BE Face Match Feature Changes

## 적용 목적
- 실시간 얼굴 매칭 결과를 3단계로 분기한다.
  - `MATCH` (>= 0.70)
  - `AMBIGUOUS` (0.65 ~ 0.70 미만)
  - `NO_MATCH` (< 0.65)
- AI 응답에 품질/자세 정보를 추가해 Front/BE가 후속 처리에 활용할 수 있게 한다.
- BE 저장 벡터에 AES-GCM 암복호화(환경키 기반)를 적용한다.

## AI 변경사항
- 파일:
  - `AI/app/core/arcface.py`
  - `AI/app/api/internal_embeddings.py`
  - `AI/app/schemas/embedding.py`
- 변경 내용:
  - 임베딩 추출 시 `qualityScore`, `yaw`, `pitch`, `roll` 함께 반환
  - 기존 `embedding/dim/model/faceCount` 유지

## BE 변경사항
- 파일:
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/service/FaceService.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/dto/response/SearchResponse.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/dto/response/FaceMatchStatus.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/client/AiClient.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/client/dto/AiEmbeddingResponse.java`
  - `naeda/src/main/resources/application.yaml`
- 변경 내용:
  - 임계치 분기 추가:
    - `face.threshold.match` (기본 0.7)
    - `face.threshold.ambiguous` (기본 0.65)
  - 검색 응답에 추가:
    - `status`, `nextAction`, `matchThreshold`, `ambiguousThreshold`
    - `qualityScore`, `yaw`, `pitch`, `roll`
  - `nextAction` 규칙:
    - MATCH -> `PASS`
    - AMBIGUOUS -> `REQUIRE_SECOND_FACTOR`
    - NO_MATCH -> `RETRY_CAPTURE`

## 벡터 암복호화 변경
- 파일:
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/entity/FloatArrayConverter.java`
- 변경 내용:
  - DB 저장 시 `FACE_EMBEDDING_AES_KEY`가 설정되어 있으면 AES-GCM으로 암호화 저장
  - 저장 포맷: `ENCv1:<base64_iv>:<base64_ciphertext>`
  - 조회 시 암호문이면 복호화 후 float[] 변환
  - 레거시 평문(JSON 배열) 데이터도 읽기 호환

## 환경변수
- `FACE_EMBEDDING_AES_KEY`
  - 길이 16/24/32 바이트 문자열 사용
  - 미설정 시 로컬 호환을 위해 평문(JSON) 저장

## 검증 결과
- BE: `./gradlew.bat compileJava` 성공
- BE 전체 테스트: 기존 테스트 1건 실패(`PointServiceTest`, face 변경과 무관)
