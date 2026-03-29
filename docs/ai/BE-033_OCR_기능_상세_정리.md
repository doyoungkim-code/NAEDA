# BE-033 OCR 기능 상세 정리

## 1. 문서 목적

이 문서는 `naedafront`, `naeda`, `AI` 세 프로젝트에 걸쳐 구현된 신분증 OCR 기능을 한 번에 이해할 수 있도록 정리한 문서다.

정리 범위는 아래와 같다.

- 사용자 관점의 OCR 등록 흐름
- 프론트, 백엔드, AI의 역할 분리
- 실제 API 경로와 데이터 구조
- `mock`, `paddleocr`, `disabled` provider 차이
- 최근 발생했던 `OCR_UNAVAILABLE` 원인과 조치 내용
- 현재 실행/배포 시 필요한 설정
- 검증 결과와 남아 있는 한계

이 기능의 목표는 신분증의 진위를 판별하는 것이 아니라, 신분증 이미지에서 사용자 확인에 필요한 최소 정보만 OCR로 추출해 회원 정보와 비교하는 것이다.

## 2. 기능 개요

FacePay 등록 과정에서 얼굴 등록이 끝나면 신분증 OCR 단계로 넘어간다.

사용자 흐름은 다음과 같다.

1. 사용자가 주민등록증 또는 운전면허증을 카메라에 비춘다.
2. 프론트가 카드 영역을 일정 시간 안정적으로 인식하면 OCR 추출 API를 호출한다.
3. 백엔드가 AI 서버 내부 OCR API를 호출한다.
4. AI 서버가 OCR 결과에서 문서 종류, 이름, 주민번호 앞 6자리, 뒤 첫 1자리를 추출한다.
5. 프론트가 추출 결과를 보여주고 사용자가 수정 또는 확인한다.
6. 프론트가 최종 확인 API를 호출한다.
7. 백엔드가 로그인 사용자 정보와 비교한다.
8. 일치하면 다음 등록 단계로 진행한다.

즉 구조적으로는 `OCR 추출`과 `최종 확인`이 분리된 2단계 흐름이다.

## 3. 전체 구조

권장 구조는 아래와 같다.

`naedafront -> naeda -> AI -> PaddleOCR`

각 계층의 책임은 다음과 같다.

### 프론트

- 카메라 프리뷰 및 자동 촬영 UX 제공
- OCR 추출 조건이 만족되면 이미지 업로드
- OCR 결과를 사용자에게 보여주고 수정 입력 받기
- 최종 확인 API 호출
- 오류 메시지와 진행률 표시

관련 코드:

- `naedafront/app/src/main/java/com/example/naedafront/ui/screen/facepay/FaceRegisterFlowScreen.kt`
- `naedafront/app/src/main/java/com/example/naedafront/data/remote/FaceRegistrationRemote.kt`

### 백엔드

- 외부 공개 API 제공
- 인증/인가 처리
- 프론트 업로드 파일을 AI 내부 OCR API로 전달
- AI 오류를 서비스용 오류로 매핑
- OCR 추출 결과와 로그인 사용자 정보를 최종 비교

관련 코드:

- `naeda/src/main/java/com/ssafy/naeda/domain/identity/controller/ResidentIdVerifyController.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/identity/service/ResidentIdVerifyService.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/identity/client/ResidentIdOcrClient.java`

### AI

- 내부 OCR API 제공
- 이미지 유효성 검사
- OCR provider 분기
- PaddleOCR 실행
- 문서 종류, 이름, 주민번호 일부 후처리
- 표준 오류 코드 반환

관련 코드:

- `AI/app/api/internal_resident_id_ocr.py`
- `AI/app/core/resident_ocr.py`
- `AI/app/core/config.py`

## 4. 프론트 동작 방식

### 4.1 OCR 추출 호출

프론트는 OCR 단계에서 일정 시간 카드를 안정적으로 유지했다고 판단하면 `extract` API를 호출한다.

호출 위치:

- `FaceRegistrationRemote.extractIdCard()`
- API 경로: `POST /api/v1/identity/id-card/extract`

업로드 형식:

- `multipart/form-data`
- 파일 필드명: `image`

프론트는 응답에서 아래 값을 받는다.

- `documentType`
- `documentMatched`
- `name`
- `residentFront6`
- `residentBackFirst1`
- `provider`
- `confidence`

### 4.2 OCR 확인 화면

프론트는 추출값을 그대로 노출하고, 사용자가 직접 수정할 수 있게 한다.

확인 화면의 핵심은 다음과 같다.

- 이름 수정 가능
- 주민번호 앞 6자리 수정 가능
- 주민번호 뒤 첫 1자리 수정 가능
- 입력 형식 최소 검증 후 `confirm` API 호출

### 4.3 최종 확인

프론트는 `POST /api/v1/identity/id-card/confirm`으로 아래 데이터를 전송한다.

```json
{
  "name": "홍길동",
  "residentFront6": "900101",
  "residentBackFirst1": "1"
}
```

응답의 `verified`, `nameMatched`, `residentNoMatched`, `nextAction` 값을 바탕으로 다음 단계 진입 여부를 결정한다.

## 5. 백엔드 동작 방식

### 5.1 외부 OCR 추출 API

경로:

- `POST /api/v1/identity/id-card/extract`

컨트롤러:

- `ResidentIdVerifyController.extract(...)`

서비스 처리 흐름:

1. 프론트로부터 이미지 수신
2. `ResidentIdOcrClient.extractResidentId(image)` 호출
3. AI 내부 OCR 응답을 `ResidentIdExtractResponse`로 변환
4. 프론트에 그대로 반환

### 5.2 AI 내부 호출

백엔드는 AI 서버의 아래 내부 API를 호출한다.

- `POST /internal/v1/ocr/id-card/extract`

호출 특징:

- `X-Service-Token` 헤더로 서비스 간 인증
- `multipart/form-data` 파일 업로드
- AI 오류 코드를 백엔드 예외로 변환

대표 매핑:

- `OCR_EXTRACTION_FAILED` -> `400`
- `INVALID_IMAGE`, `EMPTY_IMAGE`, `UNSUPPORTED_IMAGE_TYPE`, `IMAGE_TOO_LARGE` -> `400`
- `OCR_UNAVAILABLE`, `AI_UNAVAILABLE`, `AI_TIMEOUT` -> 서비스 장애 계열 예외

### 5.3 최종 비교 로직

`ResidentIdVerifyService.confirm(...)`는 아래 기준으로 비교한다.

- `username == name`
- `resident_no == residentFront6 + residentBackFirst1`

여기서 `resident_no`는 회원가입 시 저장된 7자리 값이다.

즉 최종 비교는 OCR 원문이 아니라 사용자가 마지막으로 확인한 값 기준으로 수행된다.

## 6. AI OCR 동작 방식

### 6.1 내부 API

경로:

- `POST /internal/v1/ocr/id-card/extract`

라우터:

- `AI/app/api/internal_resident_id_ocr.py`

### 6.2 실행 흐름

`extract_resident_id_fields(upload_file)` 기준 실제 흐름은 다음과 같다.

1. 업로드 파일 읽기
2. 파일 크기 제한 검사
3. `resident_ocr_provider` 설정 확인
4. provider별 처리
5. 결과 딕셔너리 반환

provider 분기는 아래와 같다.

- `mock`
- `paddleocr`
- 그 외 값은 `OCR_UNAVAILABLE`

### 6.3 PaddleOCR 처리 단계

`_extract_with_paddle_provider(image_raw)` 기준 세부 단계는 다음과 같다.

1. `cv2.imdecode`로 이미지 디코딩
2. grayscale 변환
3. normalization
4. 다시 BGR 이미지로 변환
5. `PaddleOCR(...).ocr(preprocessed, cls=True)` 실행
6. OCR 결과 flatten
7. 문서 종류 판별
8. 주민번호 일부 추출
9. 이름 추출
10. confidence 계산 후 응답 반환

### 6.4 문서 종류 판별

현재 지원 문서 종류는 두 가지다.

- `RESIDENT_ID`
- `DRIVER_LICENSE`

키워드 기반 판별 규칙:

- `주민등록증` 포함 시 `RESIDENT_ID`
- `운전면허`, `운전면허증` 포함 시 `DRIVER_LICENSE`

### 6.5 이름 추출

이름 추출은 두 단계로 시도한다.

1. `성명`, `이름` 패턴에서 직접 추출
2. 남은 OCR 텍스트 중 한글 2~5자 후보 선택

제외 토큰 예:

- `주민등록증`
- `운전면허`
- `운전면허증`
- `성명`
- `이름`
- `기간`
- `경찰청`

### 6.6 주민번호 일부 추출

주민번호는 정규식 기반으로 다음 값을 추출한다.

- 앞 6자리
- 뒤 첫 1자리

현재 후처리 패턴은 아래 형태를 대상으로 한다.

- `900101-1234567`
- `900101-1******`
- `9001011`

실제 카메라 촬영 이미지의 품질에 따라 OCR 원문이 달라질 수 있으므로, 이 부분은 실제 신분증 샘플로 계속 보정이 필요하다.

## 7. Provider 모드 설명

### 7.1 mock

의미:

- 실제 OCR 엔진을 돌리지 않고 고정값을 반환

장점:

- 빠르고 안정적
- 개발 중 전체 플로우 확인용으로 유용

한계:

- 실제 카드 이미지를 읽지 않음
- 인식 정확도 검증 불가

### 7.2 paddleocr

의미:

- 실제 OCR 모델을 로드해 카드 이미지에서 텍스트 추출

장점:

- 실제 추론 가능
- 사용자 촬영 이미지 기준 검증 가능

주의:

- 패키지 버전 조합이 중요
- 모델 다운로드 경로와 쓰기 권한 필요
- 초기 구동 시 시간이 걸릴 수 있음

### 7.3 disabled

의미:

- OCR provider가 구성되지 않은 상태

동작:

- `OCR_UNAVAILABLE`
- 프론트에서는 보통 `502` 또는 서비스 장애 메시지로 보임

## 8. 최근 실제 문제와 원인

이번 작업 중 확인된 가장 큰 문제는 아래였다.

### 8.1 증상

- 신분증을 비춰도 OCR 진행률이 완료되지 않음
- 앱에 `ID card OCR provider is not configured [OCR_UNAVAILABLE] (HTTP 502)` 표시

### 8.2 직접 원인

AI 설정 기본값은 `resident_ocr_provider=disabled`인데, 실행 환경 어디에서도 실제 provider가 지정되지 않았기 때문이다.

관련 기본값:

- `AI/app/core/config.py`

즉 프론트 문제처럼 보였지만 실제로는 첫 OCR 요청이 AI 단계에서 즉시 실패하고 있었다.

### 8.3 추가로 확인된 실행 환경 문제

Windows 가상환경에서 `paddleocr==2.8.1`과 함께 설치된 `paddlepaddle==3.3.0` 조합은 실제 추론 단계에서 깨졌다.

조치:

- `paddlepaddle==2.6.2`로 고정
- 모델 캐시를 `AI/.paddle`에 두고 재사용

## 9. 현재 반영된 설정

### 9.1 Python 의존성

현재 실제 OCR 검증 기준 핵심 의존성은 아래다.

```txt
paddleocr==2.8.1
paddlepaddle==2.6.2
numpy==1.26.4
opencv-python==4.10.0.84
```

현재 저장소 반영 위치:

- `AI/requirements.txt`

### 9.2 로컬 실행 설정

현재 로컬 AI `.env` 기준 실제 OCR을 쓰려면 아래 설정이 필요하다.

```env
RESIDENT_OCR_PROVIDER=paddleocr
```

보조 설정은 기본값으로도 동작하지만 필요 시 명시적으로 둘 수 있다.

```env
RESIDENT_OCR_PADDLE_LANG=korean
RESIDENT_OCR_MIN_CONFIDENCE=0.5
RESIDENT_OCR_PADDLE_HOME=C:\SSAFY\S14P21D103\AI\.paddle
```

### 9.3 Docker 실행 설정

Docker 기준 필요한 핵심은 아래 두 가지다.

1. provider를 `paddleocr`로 설정
2. 모델 캐시 디렉터리를 컨테이너에 마운트

현재 compose 기준 반영 포인트:

- `RESIDENT_OCR_PROVIDER`
- `RESIDENT_OCR_PADDLE_HOME=/app/.paddle`
- `../AI/.paddle:/app/.paddle`

주의:

- 백엔드의 `AI_SERVICE_TOKEN`과 AI의 `INTERNAL_SERVICE_TOKEN` 값은 같아야 한다.

## 10. 검증 결과

이번 작업에서 확인한 내용은 아래와 같다.

### 10.1 AI 테스트

`AI` 프로젝트 전체 테스트 결과:

- `pytest`
- `26 passed`

확인 범위:

- OCR 테스트
- headpose 테스트
- embeddings 테스트
- upload validation 테스트

### 10.2 실제 PaddleOCR 추론 확인

실제 provider를 `paddleocr`로 두고 내부 OCR API를 호출했을 때 다음 응답을 확인했다.

```json
{
  "documentType": "RESIDENT_ID",
  "documentMatched": true,
  "name": "홍길동",
  "residentFront6": "900101",
  "residentBackFirst1": "1",
  "provider": "paddleocr",
  "confidence": 0.9923812747001648
}
```

즉 현재 상태는 적어도 다음 단계까지는 확인된 상태다.

- 모델 로드 가능
- OCR 추론 가능
- 후처리로 이름/주민번호 일부 추출 가능
- 내부 API 응답 가능

### 10.3 아직 남아 있는 검증

저장소 안에는 실제 주민등록증 또는 운전면허증 샘플 이미지가 없었다.

따라서 아직 반드시 해야 하는 최종 검증은 아래다.

- 실제 신분증 촬영 이미지로 `extract` 호출
- 이름 추출 정확도 확인
- 주민번호 일부 추출 정확도 확인
- 운전면허증 패턴 확인
- 모바일 카메라 환경의 반사광, 흐림, 기울기 상황 확인

## 11. 실행 방법

### 11.1 AI 직접 실행

작업 디렉터리:

- `C:\SSAFY\S14P21D103\AI`

실행:

```powershell
.\start.ps1
```

전제:

- `.venv`가 준비되어 있어야 함
- `.env`에서 `RESIDENT_OCR_PROVIDER=paddleocr`
- `AI/.paddle` 경로가 쓰기 가능해야 함

### 11.2 Docker 실행

작업 디렉터리:

- `C:\SSAFY\S14P21D103\infra`

실행:

```powershell
docker compose up -d --build ai-server backend
```

전제:

- Docker Desktop 또는 Docker Engine 실행 중
- `infra/.env`에서 `RESIDENT_OCR_PROVIDER=paddleocr`
- `AI/.paddle` 볼륨 마운트 가능

## 12. 장애 대응 포인트

### 12.1 `OCR_UNAVAILABLE`

점검 순서:

1. `RESIDENT_OCR_PROVIDER` 값 확인
2. `paddleocr`, `paddlepaddle` 설치 여부 확인
3. `PADDLE_HOME` 또는 `RESIDENT_OCR_PADDLE_HOME` 쓰기 권한 확인
4. AI 프로세스 재시작 여부 확인
5. Docker 사용 시 볼륨 마운트 확인

### 12.2 `PaddleOCR is not installed`

원인:

- `paddleocr` 또는 `paddlepaddle` 미설치

점검:

```powershell
python -m pip show paddleocr
python -m pip show paddlepaddle
```

### 12.3 `PaddleOCR inference failed`

원인 후보:

- 잘못된 패키지 버전 조합
- 모델 다운로드 실패
- 권한 문제
- 런타임 호환성 문제

이번 작업에서는 `paddlepaddle 3.3.0` 조합이 문제였고, `2.6.2`로 맞춘 뒤 해결됐다.

### 12.4 `OCR_EXTRACTION_FAILED`

의미:

- OCR 엔진은 동작했지만 필요한 텍스트를 후처리에서 추출하지 못함

원인 후보:

- 카드가 너무 흐림
- 반사광 또는 잘림
- 텍스트가 정규식 패턴과 다름
- 카드 종류가 지원 범위를 벗어남

이 경우는 provider 설정 문제가 아니라 실제 OCR 인식률 또는 후처리 규칙 문제로 봐야 한다.

## 13. 보안 및 운영 주의사항

- 원본 신분증 이미지를 장기 저장하지 않는 것이 바람직하다.
- 로그에 주민등록번호 전체가 남지 않도록 마스킹해야 한다.
- `AI_SERVICE_TOKEN`, `INTERNAL_SERVICE_TOKEN`은 코드가 아니라 환경 변수로 관리해야 한다.
- 운영 배포는 여전히 `Linux + Docker`를 기준으로 가져가는 것이 안정적이다.
- 로컬 Windows에서 검증이 가능해졌더라도, 최종 배포 검증은 실제 서버 환경에서 다시 해야 한다.

## 14. 현재 결론

현재 OCR 기능 상태를 한 줄로 정리하면 다음과 같다.

`기능 구조는 이미 완성되어 있고, provider 미설정 문제는 해결되었으며, 실제 PaddleOCR 추론도 확인되었다. 이제 남은 핵심은 실제 신분증 이미지 기준 정확도 검증과 배포 환경 재기동이다.`

즉 지금 단계에서 중요한 우선순위는 아래 두 가지다.

1. 실행 중인 AI를 `paddleocr` 설정으로 재시작
2. 실제 주민등록증/운전면허증 이미지로 모바일 실검증

## 15. 관련 파일 목록

### 프론트

- `naedafront/app/src/main/java/com/example/naedafront/ui/screen/facepay/FaceRegisterFlowScreen.kt`
- `naedafront/app/src/main/java/com/example/naedafront/data/remote/FaceRegistrationRemote.kt`

### 백엔드

- `naeda/src/main/java/com/ssafy/naeda/domain/identity/controller/ResidentIdVerifyController.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/identity/service/ResidentIdVerifyService.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/identity/client/ResidentIdOcrClient.java`

### AI

- `AI/app/api/internal_resident_id_ocr.py`
- `AI/app/core/resident_ocr.py`
- `AI/app/core/config.py`
- `AI/tests/test_resident_id_ocr.py`
- `AI/requirements.txt`

### 설정

- `AI/.env`
- `infra/.env`
- `infra/docker-compose.yml`

### 관련 문서

- `docs/ai/BE-033-resident-id-ocr-verification.md`
- `docs/ai/BE-033_EC2_배포_전_체크리스트.md`
- `docs/ai/BE-033_EC2_배포_후_검증_시나리오.md`
- `docs/architecture/ec2_be_ai_ocr_architecture.md`
