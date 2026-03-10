# BE-033 신분증 OCR 확인 API

## 목적

- 주민등록증 또는 운전면허증 이미지에서 OCR로 이름과 주민등록번호 일부를 추출한다.
- 추출값을 사용자 확인 화면에 그대로 노출한다.
- 사용자가 확인 또는 수정한 값이 회원 정보와 일치하면 다음 등록 단계로 진행한다.

## 설계 방향

이번 기능은 신분증의 진위를 판별하는 기능이 아니다.

- 카메라가 주민등록증 또는 운전면허증을 인식하면 자동 촬영
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

- `POST /internal/v1/ocr/id-card/extract`

응답:

```json
{
  "documentType": "RESIDENT_ID",
  "documentMatched": true,
  "name": "홍길동",
  "residentFront6": "900101",
  "residentBackFirst1": "1",
  "provider": "mock",
  "confidence": 0.95
}
```

AI는 아래 두 모드를 지원한다.

- `resident_ocr_provider=mock`
  - 설정값 기반 mock OCR 결과 반환
  - `RESIDENT_ID`, `DRIVER_LICENSE` 두 종류를 mock 응답으로 설정 가능
- `resident_ocr_provider=paddleocr`
  - PaddleOCR로 주민등록증/운전면허증 OCR 수행
  - 문서 종류 키워드, 이름, 주민번호 일부를 후처리로 추출
- `resident_ocr_provider=disabled`
  - OCR 미구성 상태로 503 반환

현재 코드에는 PaddleOCR provider 구현이 포함되어 있지만, 실행 환경에 `paddleocr` 패키지와 관련 런타임이 설치되어 있어야 실제 OCR이 동작한다.

### BE

외부 API는 2개다.

#### 1. OCR 추출 API

- `POST /api/v1/identity/id-card/extract`

입력:

- `multipart/form-data`
  - `image`

역할:

1. 이미지 유효성 검사
2. AI OCR API 호출
3. 추출값과 문서 종류를 프론트에 그대로 반환

응답 예시:

```json
{
  "documentType": "DRIVER_LICENSE",
  "documentMatched": true,
  "name": "홍길동",
  "residentFront6": "900101",
  "residentBackFirst1": "1",
  "provider": "mock",
  "confidence": 0.91
}
```

#### 2. 최종 확인 API

- `POST /api/v1/identity/id-card/confirm`

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

1. 프론트가 주민등록증 또는 운전면허증 자동 촬영
2. 프론트가 `extract` API 호출
3. BE가 AI OCR 결과를 받아 문서 종류와 추출값 반환
4. 프론트가 확인 화면에서 신분증 종류, 이름, 주민번호를 표시
5. 사용자가 값 수정 후 확인 버튼 클릭
6. 프론트가 `confirm` API 호출
7. BE가 회원 정보와 비교
8. 일치 시 다음 단계로 이동

## 환경 설정

AI `.env` 예시:

```env
RESIDENT_OCR_PROVIDER=paddleocr
RESIDENT_OCR_PADDLE_LANG=korean
RESIDENT_OCR_MIN_CONFIDENCE=0.5
```

mock 테스트가 필요하면 아래 설정을 사용한다.

```env
RESIDENT_OCR_PROVIDER=mock
RESIDENT_OCR_MOCK_DOCUMENT_TYPE=DRIVER_LICENSE
RESIDENT_OCR_MOCK_NAME=홍길동
RESIDENT_OCR_MOCK_FRONT6=900101
RESIDENT_OCR_MOCK_BACK1=1
RESIDENT_OCR_MOCK_CONFIDENCE=0.91
```

## 한계

- PaddleOCR는 코드에 반영했지만, 패키지와 런타임이 없는 환경에서는 `OCR_UNAVAILABLE`가 반환될 수 있다.
- 현재 후처리 규칙은 주민등록증/운전면허증의 일반적인 텍스트 패턴을 기준으로 작성되어 있어 실제 샘플 이미지로 보정이 필요하다.

## 관련 문서

- `EC2 배포 전 체크리스트`: `docs/BE-033_EC2_배포_전_체크리스트.md`
- `EC2 배포 후 검증 시나리오`: `docs/BE-033_EC2_배포_후_검증_시나리오.md`
- `EC2 아키텍처 설계`: `docs/architecture/ec2_be_ai_ocr_architecture.md`
