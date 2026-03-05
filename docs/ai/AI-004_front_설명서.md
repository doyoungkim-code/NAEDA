# AI-004 front 설명서

## 1. 목적
- AI-004 실시간 얼굴 매칭 기능에서 Front가 구현해야 할 동작을 정의한다.
- Front는 AI를 직접 호출하지 않고, Backend(BE) API만 호출한다.

## 2. 전체 흐름
1. 카메라 프리뷰 시작
2. 모바일 센서(자이로/가속도)로 기기 자세 판정
3. 자세가 허용될 때만 얼굴 프레임 캡처
4. BE `POST /api/v1/face/search` 호출
5. 응답 상태(`MATCH/AMBIGUOUS/NO_MATCH`)에 따라 화면 분기

## 3. Front 구현 기능

### 3.1 촬영 제어
- 카메라 프리뷰 표시
- 캡처 버튼 또는 자동 캡처 트리거 제공
- 허용 자세가 아닐 때는 캡처/전송 비활성화

### 3.2 모바일 센서 기반 자세 필터
- 자이로/가속도계로 단말 기울기 계산
- `세워짐` 상태만 촬영 허용, `누움` 상태는 촬영 차단
- 비허용 시 가이드 문구 표시
  - 예: "휴대폰을 세워서 정면을 바라봐 주세요"

### 3.3 프레임 품질 게이트(클라이언트 1차)
- 가능하면 얼굴 위치/크기(중앙, 최소 크기) 확인
- 흔들림/블러가 큰 프레임은 전송하지 않음
- 저품질 시 재촬영 안내

### 3.4 BE API 연동
- Endpoint: `POST /api/v1/face/search`
- Content-Type: `multipart/form-data`
- 전송 필드:
  - `image` (required)
  - `topK` (optional, 미입력 시 BE 기본값 사용)

요청 예시:
```bash
curl -X POST http://localhost:8080/api/v1/face/search \
  -F "image=@./face.jpg" \
  -F "topK=3"
```

### 3.5 응답 상태 분기
- `MATCH`:
  - 인증/결제 통과 화면으로 이동
- `AMBIGUOUS`:
  - 2차 인증 화면 이동 (PIN/전화번호/추가 본인확인)
- `NO_MATCH`:
  - "얼굴을 인식할 수 없습니다" 메시지 + 재촬영 유도

### 3.6 응답 필드 활용
- `status`: 최종 판정 상태
- `nextAction`: FE 후속 액션
  - `PASS`, `REQUIRE_SECOND_FACTOR`, `RETRY_CAPTURE`
- `similarity`: 최고 유사도
- `matchThreshold`: 0.70
- `ambiguousThreshold`: 0.65
- `qualityScore`: AI 품질 점수(0~1)
- `yaw/pitch/roll`: 얼굴 각도 정보
- `candidates`: 상위 후보 목록

### 3.7 에러 처리
- 코드별 안내 메시지 분기:
  - `NO_FACE`: 얼굴 미검출
  - `MULTIPLE_FACES`: 복수 얼굴
  - `EMPTY_IMAGE`, `INVALID_IMAGE`: 입력 오류
  - `AI_TIMEOUT`, `AI_UNAVAILABLE`: 서버/AI 지연 및 장애
- 네트워크 오류 시 재시도 버튼 제공

### 3.8 보안/운영 가이드
- 촬영 이미지 장기 저장 금지
- 로그에 이미지/민감 정보 출력 금지
- 중복 요청 방지(디바운스/로딩 잠금)
- 타임아웃 및 취소 처리

## 4. 권장 UI 문구
- 자세 불가: "휴대폰을 세워주세요"
- 얼굴 미검출: "얼굴이 화면에 보이도록 맞춰주세요"
- 애매한 매칭: "추가 인증이 필요합니다"
- 인식 실패: "얼굴을 인식할 수 없습니다. 다시 시도해주세요"

## 5. 수용 기준(체크리스트)
- [ ] AI 직접 호출 없이 BE API만 사용한다.
- [ ] 센서 기반 자세 필터가 적용된다.
- [ ] 비허용 자세에서 캡처/전송이 차단된다.
- [ ] `MATCH/AMBIGUOUS/NO_MATCH` 상태별 화면 분기가 동작한다.
- [ ] 에러코드별 사용자 메시지가 제공된다.
- [ ] 민감 데이터가 로그/로컬 저장소에 남지 않는다.
