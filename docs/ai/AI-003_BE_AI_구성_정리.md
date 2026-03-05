# AI-003 BE/AI 구성 정리 (Front 제외)

## 1. 이번에 추가한 기능

### AI 서버
- 신규 내부 API 추가
  - `POST /internal/v1/liveness/headpose/check`
  - 입력: `multipart/form-data`
    - `expectedDirection` (`front|left|right|up|down`)
    - `image` (단일 이미지)
  - 출력:
    - `expectedDirection`
    - `detectedDirection`
    - `matched`
    - `yaw`
    - `pitch`
    - `confidence`
- 구현 파일
  - `AI/app/api/internal_liveness.py`
  - `AI/app/core/headpose.py`
  - `AI/app/schemas/headpose.py`
  - `AI/app/main.py` (router 등록)

### BE 서버
- 신규 외부 API 추가
  - `POST /api/v1/face/liveness/headpose/check`
  - 입력: `multipart/form-data`
    - `expectedDirection`
    - `image`
  - 동작: `BE -> AI (/internal/v1/liveness/headpose/check)` 전달 후 결과 반환
- AI client 확장
  - headpose 검증 호출 메서드 추가
  - `INVALID_DIRECTION` 에러 매핑 추가
- 구현 파일
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/controller/FaceController.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/service/FaceService.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/client/AiClient.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/client/dto/AiHeadPoseResponse.java`
  - `naeda/src/main/java/com/ssafy/naeda/domain/face/dto/response/HeadPoseCheckResponse.java`

## 2. Front가 추가해야 할 내용

- 카메라 프레임 수집/샘플링
  - 예: 1~2fps로 프레임 업로드, 또는 10장 배치 업로드 정책 확정
- 챌린지 UI
  - `left -> right -> up -> down -> front` 순서 안내
- BE API 호출
  - `POST /api/v1/face/liveness/headpose/check`
- 판정 처리
  - `matched=true`이면 다음 단계 진행(캡처/매칭)
  - 실패 시 재시도 UI 및 제한 횟수 적용

## 3. BE 추가 개선 권장

- 폴백 전략 (AI-018 연계)
  - 타임아웃/장애 시 재시도 + degrade 응답
- 정책 엔진화
  - 연속 N회 성공 시 통과 등 규칙을 서버 정책으로 분리
- 감사 로그
  - 요청 traceId, user/session, 결과값(`yaw/pitch/matched`) 기록
- 보안
  - 업로드 파일 크기/포맷 검증 강화, rate limit 적용

## 4. AI 추가 개선 권장

- 현재 방향 판정은 키포인트 기반 휴리스틱
  - 추후 Mediapipe/전용 Head Pose 모델로 정확도 개선 필요
- `confidence` 보정
  - 실제 데이터 기반 threshold 튜닝
- 다중 프레임 판정 지원
  - 단일 이미지가 아닌 짧은 시퀀스 기반 안정화
- 에러코드 세분화
  - `LOW_CONFIDENCE`, `LANDMARK_UNSTABLE` 등 운영 친화 코드 추가

## 5. 통합 흐름 (Front 제외 관점)

1. BE가 Front에서 이미지+기대방향 수신
2. BE가 AI 내부 API 호출
3. AI가 방향 판정 후 결과 반환
4. BE가 정책 적용 후 Front에 최종 응답 반환

