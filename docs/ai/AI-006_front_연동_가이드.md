# AI-006 Front 연동 가이드 (구현 제외)

## 원칙
- Front는 RBA 판정 로직을 직접 계산하지 않는다.
- BE의 `/api/v1/face/search` 응답을 받아 화면 분기만 수행한다.

## 호출 API
- `POST /api/v1/face/search`
- Content-Type: `multipart/form-data`
- 요청 파트
  - `image` (필수)
  - `topK` (선택, 기본 3)
  - `amount` (선택, 기본 0)

예시:
```bash
curl -X POST http://localhost:8080/api/v1/face/search \
  -F "image=@./face.jpg" \
  -F "topK=3" \
  -F "amount=30000"
```

## Front 분기 기준
- `blocked=true` 또는 `authLevel=BLOCKED`
  - 차단 UI 노출, 재시도 또는 상담 유도
- `authLevel=FACE_ONLY`
  - 즉시 통과
- `authLevel=FACE_PHONE`
  - 전화 인증 화면 이동
- `authLevel=FACE_PIN`
  - PIN 인증 화면 이동
- `authLevel=FACE_SIGNATURE`
  - 전자서명 화면 이동

## 함께 참고할 필드
- `status` (`MATCH/AMBIGUOUS/NO_MATCH`): 얼굴 매칭 상태
- `nextAction`: 기본 후속 액션 힌트
- `similarity`: 유사도
- `qualityScore`, `yaw/pitch/roll`: 품질/자세 참고 정보
- `rbaReason`: 운영 로그/디버깅용 판정 사유

## 권장 처리
1. 우선순위는 `blocked/authLevel`로 분기
2. `status`는 보조 UI 메시지(예: 매칭 애매)로 활용
3. 응답 지연/오류 시 재시도 버튼 제공
4. 민감 정보(이미지, 원시 벡터)는 로그에 남기지 않음
