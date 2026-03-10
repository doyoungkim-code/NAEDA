# BE-033 주민등록증 OCR 확인 API

## 목적

- 주민등록증 이미지에서 OCR로 이름과 주민등록번호 일부를 추출한다.
- 추출값을 사용자 확인 화면에 그대로 노출한다.
- 사용자가 확인 또는 수정한 값이 회원 정보와 일치하면 다음 등록 단계로 진행한다.

## 설계 방향

이번 기능은 주민등록증의 진위를 판별하는 기능이 아니다.

- 카메라가 주민등록증을 인식하면 자동 촬영
- OCR로 `이름`, `주민번호 앞 6자리`, `뒤 첫 1자리` 추출
- 프론트에서 사용자가 값을 직접 확인하거나 수정
- 사용자가 확인 버튼을 누르면 BE가 회원 정보와 최종 비교

즉 `OCR 추출`과 `최종 확인`을 분리한 2단계 흐름으로 구현한다.

## 비교 기준

현재 `User` 테이블에 아래 정보가 존재한다.

- `username`
- `resident_no` (`주민번호 앞 6자리 + 뒤 첫 1자리`, 총 7자리)

따라서 최종 확인 시 아래 기준으로 비교한다.

- `username == name`
- `resident_no == residentFront6 + residentBackFirst1`

## 구현 구조

### AI

내부 OCR API:

- `POST /internal/v1/ocr/resident-id/extract`

응답:

```json
{
  "name": "홍길동",
  "residentFront6": "900101",
  "residentBackFirst1": "1",
  "provider": "mock"
}
```

현재 로컬 환경에는 실제 OCR 엔진이 설치되어 있지 않으므로 AI는 아래 두 모드를 지원한다.

- `resident_ocr_provider=mock`
  - 설정값 기반 mock OCR 결과 반환
- `resident_ocr_provider=disabled`
  - OCR 미구성 상태로 503 반환

실제 OCR을 붙일 때는 AI 내부 구현만 교체하면 되고, API 스키마는 유지한다.

### BE

외부 API는 2개다.

#### 1. OCR 추출 API

- `POST /api/v1/identity/resident-id/extract`

입력:

- `multipart/form-data`
  - `image`

역할:

1. 이미지 유효성 검사
2. AI OCR API 호출
3. 추출값을 프론트에 그대로 반환

응답 예시:

```json
{
  "name": "홍길동",
  "residentFront6": "900101",
  "residentBackFirst1": "1",
  "provider": "mock"
}
```

#### 2. 최종 확인 API

- `POST /api/v1/identity/resident-id/confirm`

입력:

```json
{
  "name": "홍길동",
  "residentFront6": "900101",
  "residentBackFirst1": "1"
}
```

역할:

1. JWT에서 로그인 사용자 ID 확인
2. `UserRepository.findByUserId()`로 사용자 조회
3. 사용자가 확인 또는 수정한 값을 회원 정보와 비교
4. 일치 여부 반환

성공 응답:

```json
{
  "verified": true,
  "nameMatched": true,
  "residentNoMatched": true,
  "nextAction": "CONTINUE_FACEPAY_REGISTRATION"
}
```

실패 응답:

```json
{
  "verified": false,
  "nameMatched": true,
  "residentNoMatched": false,
  "nextAction": "RETRY_CONFIRM"
}
```

## 전체 흐름

1. 프론트가 주민등록증 자동 촬영
2. 프론트가 `extract` API 호출
3. BE가 AI OCR 결과를 받아 추출값 반환
4. 프론트가 확인 화면에서 이름/주민번호를 표시
5. 사용자가 값 수정 후 확인 버튼 클릭
6. 프론트가 `confirm` API 호출
7. BE가 회원 정보와 비교
8. 일치 시 다음 단계로 이동

## 환경 설정

AI `.env` 예시:

```env
RESIDENT_OCR_PROVIDER=mock
RESIDENT_OCR_MOCK_NAME=홍길동
RESIDENT_OCR_MOCK_FRONT6=900101
RESIDENT_OCR_MOCK_BACK1=1
```

## 한계

- 현재 로컬 환경에는 OCR 라이브러리/엔진이 없어 실제 주민등록증 이미지 OCR은 아직 수행하지 않는다.
- 이번 작업은 AI/BE 간 OCR 추출 API 계약과 사용자 확인 기반 본인확인 흐름을 먼저 구현한 단계다.
- 실제 OCR 도입 시 AI 내부 구현만 교체하면 BE API 구조는 그대로 사용할 수 있다.
