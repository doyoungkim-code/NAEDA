# Back 담당자 체크리스트

## 0. 역할 정의
- [ ] Back 책임 범위를 팀에 공유했다.
- [ ] Back 범위를 `인증/인가 + AI 호출 + 임베딩 저장/검색 + 최종 판정`으로 확정했다.
- [ ] AI/Front와 경계(역할 분리)를 합의했다.

## 1. 착수 전 합의
### 1.1 API 계약 (Front <-> Back)
- [ ] 공개 API 경로를 확정했다.
- [ ] 등록 API 요청 필드를 확정했다. (`userId`, `pose`, `image`)
- [ ] 검색 API 요청 필드를 확정했다. (`image`, `topK`)
- [ ] 응답 스키마를 확정했다. (`matched`, `bestUserId`, `similarity`, `threshold`, `candidates`)
- [ ] 에러 응답 표준을 확정했다. (`code`, `message`, `requestId`)

### 1.2 내부 API 계약 (Back <-> AI)
- [ ] 내부 경로를 확정했다. (`POST /internal/v1/embeddings/extract`)
- [ ] AI 에러코드 매핑 규칙을 확정했다.
- [ ] timeout/retry/circuit breaker 정책을 합의했다.

### 1.3 보안/개인정보
- [ ] 원본 이미지 비저장 정책을 확정했다.
- [ ] Back 로그에 이미지 본문이 남지 않도록 정책을 확정했다.
- [ ] 내부 API 인증(서비스 토큰/mTLS) 방식을 확정했다.

## 2. API 구현
### 2.1 Controller
- [ ] `GET /api/v1/face/health` 구현
- [ ] `POST /api/v1/face/enroll` 구현
- [ ] `POST /api/v1/face/search` 구현
- [ ] multipart 요청 파싱과 입력 검증을 구현했다.

### 2.2 입력 검증
- [ ] `userId` 길이/문자 검증을 구현했다.
- [ ] `pose` 허용값 검증을 구현했다. (`front|left|right|up|down`)
- [ ] 이미지 파일 크기 제한을 구현했다.
- [ ] 이미지 MIME/확장자 검증을 구현했다.

### 2.3 에러 처리
- [ ] 공통 예외 핸들러를 구현했다.
- [ ] AI 오류를 도메인 오류로 변환한다.
- [ ] 4xx/5xx 상태코드를 일관되게 반환한다.

## 3. AI 연동 구현
- [ ] AI 클라이언트(WebClient/Feign)를 구현했다.
- [ ] connect/read timeout을 설정했다.
- [ ] 단기 재시도(1~2회) 정책을 적용했다.
- [ ] circuit breaker를 적용했다.
- [ ] 내부 인증 헤더를 추가한다. (`X-Service-Token` 등)
- [ ] AI 응답 임베딩 길이(512)를 검증한다.

## 4. DB 설계 및 저장
### 4.1 테이블
- [ ] `face_embeddings` 테이블을 생성했다.
- [ ] 컬럼을 정의했다. (`user_id`, `pose`, `embedding`, `created_at`, `updated_at`)
- [ ] `(user_id, pose)` 유니크 제약을 적용했다.

### 4.2 저장 정책
- [ ] 등록 시 upsert 동작을 구현했다.
- [ ] 정면 다중 슬롯(front/front_2/front_3) 정책을 확정했다.
- [ ] 원본 이미지를 저장하지 않음을 코드/운영에서 확인했다.

### 4.3 검색 정책
- [ ] 유사도 계산 방식(코사인)을 확정했다.
- [ ] topK 조회 로직을 구현했다.
- [ ] threshold 판정 로직(현재 0.7)을 구현했다.

## 5. 성능 최적화
- [ ] DB 인덱스를 적용했다.
- [ ] 데이터 증가 대비 `pgvector` 전환 계획을 작성했다.
- [ ] 검색 쿼리 p95 latency를 측정했다.
- [ ] burst 요청 시 안정 동작을 확인했다.

## 6. 보안/개인정보 점검
- [ ] TLS 적용 상태를 확인했다.
- [ ] 민감 로그 마스킹을 적용했다.
- [ ] 임베딩 테이블 최소권한(RBAC)을 적용했다.
- [ ] 감사로그(누가/언제/어떤 API) 추적이 가능하다.
- [ ] 보존기간/삭제 정책(탈퇴 시 삭제 등)을 정의했다.

## 7. 테스트
### 7.1 단위 테스트
- [ ] 입력 검증 테스트
- [ ] 에러 매핑 테스트
- [ ] threshold 판정 테스트

### 7.2 통합 테스트
- [ ] Back -> AI 연동 테스트
- [ ] 등록/검색 E2E 테스트
- [ ] AI 장애/timeout 시 graceful 처리 테스트

### 7.3 부하 테스트
- [ ] 동시 요청 시 응답시간/오류율 측정
- [ ] 목표 SLA 충족 여부 확인

## 8. 배포/운영
- [ ] 환경변수 분리(dev/stage/prod) 적용
- [ ] 헬스체크/레디니스 설정
- [ ] 모니터링 지표 구성(요청수, 오류율, latency)
- [ ] 알람 정책 구성(5xx 급증, timeout 증가)
- [ ] 롤백 절차 문서화

## 9. 문서화/인수인계
- [ ] OpenAPI 최신화
- [ ] 에러코드 문서 최신화
- [ ] 운영 가이드 최신화
- [ ] 장애 대응 Runbook 공유

## 10. 완료 기준 (Definition of Done)
- [ ] Front에서 등록/검색 API를 정상 사용한다.
- [ ] AI 연동 장애 시에도 오류 응답이 일관된다.
- [ ] 원본 이미지 비저장 정책이 검증되었다.
- [ ] 성능/SLA와 보안 기준을 충족한다.

