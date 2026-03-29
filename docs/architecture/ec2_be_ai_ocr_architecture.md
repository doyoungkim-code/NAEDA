# EC2 BE-AI OCR Architecture

## 1. 목적

이 문서는 `EC2 Linux` 환경에서 `BE(Spring)`와 `AI(FastAPI + PaddleOCR)`를 어떻게 배치하고 운영할지 정리한 설계 문서다.

현재 결론은 다음과 같다.

- `PaddleOCR`는 `AI` 서비스에 둔다.
- `BE`는 OCR 추론을 직접 수행하지 않고 `AI` 내부 API를 호출한다.
- `EC2`에서는 `Linux + Docker` 기반으로 `PaddleOCR`를 재검증하고 운영한다.

## 2. 최종 권장 구조

### 구조 요약

`FE -> BE -> AI -> PaddleOCR`

- `FE`
  - 신분증 이미지를 `BE`에 업로드
- `BE`
  - 인증/인가
  - 이미지 기본 검증
  - `AI` OCR API 호출
  - OCR 결과를 사용자 정보와 비교
  - 최종 응답 반환
- `AI`
  - 이미지 디코딩
  - OCR 전처리
  - `PaddleOCR` 추론
  - 문서 종류 판별
  - 이름, 주민번호 일부 추출
  - 추론 실패/장애 코드 표준화

## 3. 왜 OCR을 AI에 두는가

### 권장 이유

- `PaddleOCR`는 Python 런타임, 모델 파일, OpenCV, 추론 환경 의존성이 크다.
- `BE`는 도메인 로직과 트랜잭션 처리에 집중하고, 모델 추론은 `AI`에 위임하는 편이 책임 분리가 명확하다.
- `PaddleOCR`가 실패하거나 외부 OCR API로 교체되어도 `BE`는 같은 내부 인터페이스만 유지하면 된다.
- 추후 `GPU`, 별도 `AI` 인스턴스, 외부 OCR 전환 시 변경 범위를 `AI` 서비스 내부로 제한할 수 있다.

### 비권장 구조

`BE(Spring)`에 `PaddleOCR` 또는 외부 OCR SDK를 직접 넣는 방식은 현재 프로젝트 기준으로 권장하지 않는다.

이유:

- Java 서비스가 모델 추론 런타임까지 떠안게 된다.
- OCR 교체 비용이 커진다.
- 얼굴 임베딩, Liveness, Headpose와 OCR의 추론 계층이 분리되지 않아 운영 복잡도가 증가한다.

## 4. 현재 코드 기준 책임 분리

현재 코드도 이미 아래와 같은 방향으로 구성되어 있다.

- `BE`
  - `ResidentIdVerifyService`: OCR 결과를 사용자 정보와 비교
  - `ResidentIdOcrClient`: `AI` 내부 OCR API 호출
- `AI`
  - `internal_resident_id_ocr.py`: 내부 OCR 엔드포인트
  - `resident_ocr.py`: provider 분기, 전처리, OCR 추론, 결과 정규화

즉 이번 `EC2` 설계는 현재 코드 구조를 유지한 채 배포 환경만 `Linux`에 맞게 안정화하는 방향이 가장 적절하다.

## 5. EC2 배포 권장안

### 1안. 단일 EC2 내 분리 컨테이너 운영

초기 배포는 아래 구성을 권장한다.

- EC2 1대
- `nginx` 또는 외부 ALB
- `BE` 컨테이너 1개
- `AI` 컨테이너 1개
- `PostgreSQL`은 별도 RDS 또는 별도 컨테이너

장점:

- 구현이 가장 빠르다.
- `BE`와 `AI`를 논리적으로 분리하면서도 운영 복잡도는 낮다.
- 내부 네트워크로 `BE -> AI` 호출이 가능하다.

권장 대상:

- 현재 프로젝트 단계
- 트래픽이 크지 않은 시점
- 빠른 데모 및 1차 운영

### 2안. BE/AI 분리 EC2 운영

트래픽이 증가하거나 OCR 부하가 커지면 아래 구조로 확장한다.

- `BE EC2`
- `AI EC2`
- `RDS`
- 필요 시 `Redis`, 모니터링 서버 별도 구성

장점:

- OCR 부하가 `BE` 응답성에 직접 영향을 덜 준다.
- `AI`만 별도 스케일링하기 쉽다.
- OCR 추론 튜닝, 배포, 롤백을 독립적으로 수행할 수 있다.

현재 단계에서는 1안으로 시작하고, 추후 2안으로 확장하는 것이 현실적이다.

## 6. 네트워크 구성 권장안

### 외부 노출

- 외부 공개: `BE`만 공개
- 내부 통신: `BE -> AI`만 허용
- `AI`는 가능하면 퍼블릭 오픈하지 않는다

### 보안 그룹 권장

- `80/443`
  - ALB 또는 Nginx용
- `BE` 포트
  - 외부 또는 프록시만 접근
- `AI` 포트 `8000`
  - 같은 EC2 내 Docker network 또는 private subnet만 허용

### 인증

`BE -> AI` 호출에는 현재처럼 `X-Service-Token` 헤더를 유지한다.

- `BE` 환경 변수: `ai.service-token`
- `AI` 환경 변수: `INTERNAL_SERVICE_TOKEN`

## 7. 요청 흐름

### OCR 추출

1. `FE`가 신분증 이미지를 `BE`에 업로드
2. `BE`가 파일 크기와 형식을 1차 검증
3. `BE`가 `AI /internal/v1/ocr/id-card/extract` 호출
4. `AI`가 이미지 전처리 후 `PaddleOCR` 수행
5. `AI`가 문서 타입, 이름, 주민번호 일부, confidence 반환
6. `BE`가 응답을 사용자 확인 화면용 DTO로 변환
7. 사용자가 수정/확인 후 `BE`가 최종 검증 수행

### 실패 처리

- OCR 인식 실패
  - `400 OCR_EXTRACTION_FAILED`
- 이미지 문제
  - `400 EMPTY_IMAGE`, `INVALID_IMAGE`, `UNSUPPORTED_IMAGE_TYPE`, `IMAGE_TOO_LARGE`
- AI 장애 또는 추론 장애
  - `503 OCR_UNAVAILABLE`
- AI 응답 지연
  - `504 AI_TIMEOUT` 또는 `OCR_TIMEOUT` 성격으로 처리

## 8. 컨테이너 배포 권장 방식

### AI 컨테이너

`AI`는 별도 Docker 이미지로 운영한다.

기본 원칙:

- 베이스 이미지: `python:3.11-slim`
- 런타임: `Linux`
- OCR 모델 캐시 디렉터리: 컨테이너 내부 임시 경로가 아니라 볼륨 마운트 권장
- `PADDLE_HOME` 계열 경로는 영속 볼륨으로 유지

권장 볼륨:

- `/app/.paddle`

이유:

- 컨테이너 재시작 시 모델 재다운로드를 줄일 수 있다.
- 초기 부팅 시간을 줄일 수 있다.

### BE 컨테이너

`BE`는 현재처럼 `AI base-url`만 바라보도록 유지한다.

권장 환경 변수:

- `AI_BASE_URL=http://ai-internal:8000`
- `AI_SERVICE_TOKEN=<secure-token>`
- `AI_TIMEOUT_SECONDS=5`

### 단일 EC2 Docker Compose 예시

```yaml
services:
  be:
    image: naeda-be:latest
    env_file:
      - .env
    environment:
      AI_BASE_URL: http://ai-internal:8000
      AI_SERVICE_TOKEN: ${AI_SERVICE_TOKEN}
      AI_TIMEOUT_SECONDS: 5
    depends_on:
      - ai-internal

  ai-internal:
    image: naeda-ai:latest
    env_file:
      - .env
    environment:
      APP_HOST: 0.0.0.0
      APP_PORT: 8000
      APP_RELOAD: "false"
      INTERNAL_SERVICE_TOKEN: ${AI_SERVICE_TOKEN}
      RESIDENT_OCR_PROVIDER: paddleocr
      RESIDENT_OCR_PADDLE_HOME: /app/.paddle
      AI_TIMEOUT_SECONDS: 5
    volumes:
      - paddle_data:/app/.paddle

volumes:
  paddle_data:
```

## 9. 환경 변수 권장값

### BE

```env
AI_BASE_URL=http://ai-internal:8000
AI_SERVICE_TOKEN=change-this-token
AI_TIMEOUT_SECONDS=5
```

Spring 설정은 아래 의미로 연결되면 된다.

- `ai.base-url`
- `ai.service-token`
- `ai.timeout-seconds`

### AI

```env
APP_HOST=0.0.0.0
APP_PORT=8000
APP_RELOAD=false
INTERNAL_SERVICE_TOKEN=change-this-token
RESIDENT_OCR_PROVIDER=paddleocr
RESIDENT_OCR_PADDLE_LANG=korean
RESIDENT_OCR_MIN_CONFIDENCE=0.5
RESIDENT_OCR_PADDLE_HOME=/app/.paddle
AI_TIMEOUT_SECONDS=5
```

### 로컬 개발

로컬 Windows에서는 아래 설정으로 개발하는 것을 권장한다.

```env
RESIDENT_OCR_PROVIDER=mock
```

즉:

- 로컬: `mock`
- EC2 Linux: `paddleocr`

## 10. 운영 정책

### 타임아웃

- `BE -> AI` 호출 타임아웃: `5초` 시작
- OCR이 느리면 `7초`까지는 검토 가능
- 그 이상은 사용자 경험이 급격히 나빠질 수 있으므로 신중히 조정

### 재시도

- `BE -> AI OCR`은 자동 재시도를 강하게 권장하지 않음
- OCR은 CPU 사용량이 크고, 같은 이미지 재시도가 성공률을 크게 높이지 않을 수 있음
- 사용자 재촬영 유도가 더 낫다

### 로깅

- `BE`
  - 요청 ID
  - 사용자 ID
  - AI 응답 코드
  - AI 응답 시간
- `AI`
  - 요청 ID
  - provider
  - OCR 소요 시간
  - 추론 성공/실패 코드

주의:

- 원본 신분증 이미지 전체를 장기 보관하지 않는 것을 권장
- 로그에 주민번호 전체가 남지 않도록 마스킹 필요

## 11. 장애 대응 전략

### AI 장애 시

- `BE`는 `503 OCR_UNAVAILABLE` 계열로 사용자에게 안내
- 프런트 메시지는 "신분증 인식이 일시적으로 불안정합니다. 다시 시도해주세요." 수준으로 단순화

### PaddleOCR 장애 시

`AI` 내부 provider 분기를 유지한다.

예:

- `mock`
- `paddleocr`
- 추후 `clova`

이렇게 두면 나중에 `PaddleOCR` 대신 외부 OCR API를 붙이더라도 `BE` 수정 없이 `AI` 내부 교체만으로 대응 가능하다.

## 12. 확장 로드맵

### 1단계

- 단일 EC2
- `BE` 컨테이너
- `AI` 컨테이너
- `PaddleOCR` on Linux

### 2단계

- `AI` 전용 EC2 분리
- OCR/Face 추론 부하 분산

### 3단계

- `AI` 내부에서 provider 다중화
- `paddleocr` 실패 시 외부 OCR API fallback 검토

단, 주민등록증 OCR은 보안과 비용 이슈가 있어 fallback을 무조건 자동화하기보다 운영 정책을 먼저 정하는 것이 낫다.

## 13. 최종 결론

현재 프로젝트의 `EC2` 배포 기준 최적 구조는 다음과 같다.

- `OCR은 AI 서비스에 둔다`
- `BE는 OCR 결과를 소비하고 사용자 정보 검증만 담당한다`
- `초기 배포는 단일 EC2 내 BE/AI 분리 컨테이너로 간다`
- `AI는 Linux Docker 환경에서 PaddleOCR를 사용한다`
- `로컬 Windows에서는 mock provider로 개발하고, 실제 OCR은 EC2에서 검증한다`

이 구조가 가장 빠르고, 현재 코드와도 잘 맞고, 추후 외부 OCR API 또는 별도 AI 서버로 확장하기도 쉽다.
