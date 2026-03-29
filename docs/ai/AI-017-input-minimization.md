# AI-017 AI 입력 데이터 최소화

## 목적

- AI 추론에 불필요한 입력값을 줄인다.
- 원본 이미지와 파일 메타데이터가 시스템 내부에 오래 남지 않게 한다.
- BE와 AI 모두에서 동일한 입력 제한 규칙을 적용한다.

## 이번 변경 범위

### BE

- 얼굴 등록 API에서 `userId`를 multipart 요청값으로 받지 않고 인증 사용자 ID를 사용하도록 변경
- 업로드 이미지 검증기 추가
  - 허용 MIME 타입: `image/jpeg`, `image/png`, `image/webp`
  - 최대 크기: `3MB`
- AI 서버로 전달하는 multipart 파일명을 원본 파일명 대신 고정값 `image.jpg`로 통일
- AI 호출 이후 메모리에 남아 있는 이미지 바이트 배열을 즉시 `0`으로 덮어쓰기

### AI

- 업로드 메타데이터 검증 추가
  - 허용 MIME 타입: `image/jpeg`, `image/png`, `image/webp`
  - 최대 크기: `3MB`
- 업로드 바이트 길이 검증 추가
- `UploadFile.read()` 이후 `UploadFile.close()` 호출
- 원본 이미지 바이트는 메모리에서만 처리하고 임시 파일로 저장하지 않음

## 변경 파일

### BE

- `naeda/src/main/java/com/ssafy/naeda/domain/face/controller/FaceController.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/face/service/FaceService.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/face/service/FaceInputValidator.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/face/client/AiClient.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/face/exception/FaceErrorCode.java`
- `naeda/src/main/resources/application.yaml`

### AI

- `AI/app/api/internal_embeddings.py`
- `AI/app/api/internal_liveness.py`
- `AI/app/core/arcface.py`
- `AI/app/core/headpose.py`
- `AI/app/core/upload_validation.py`
- `AI/app/core/config.py`

## 동작 방식

### 1. 등록 API 입력 최소화

기존:

- `userId`
- `pose`
- `image`

변경 후:

- `pose`
- `image`

`userId`는 JWT 인증 컨텍스트의 사용자 ID를 사용한다.

### 2. 이미지 입력 검증

BE와 AI 모두에서 아래 규칙을 적용한다.

- 빈 파일 거부
- 3MB 초과 파일 거부
- 허용되지 않은 MIME 타입 거부

이중 검증으로 구성한 이유:

- BE에서 빠르게 차단
- AI에서 방어선 한 겹 추가

### 3. 메타데이터 최소화

원본 파일명은 AI에 전달하지 않는다.

- 기존: 업로드된 파일명 전달
- 변경: 항상 `image.jpg`

이렇게 하면 기기명, 사용자 로컬 파일명, 촬영 흔적 같은 불필요한 메타데이터 누출을 줄일 수 있다.

### 4. 원본 이미지 비보관

- BE는 원본 이미지를 DB에 저장하지 않음
- AI는 원본 이미지를 메모리에서만 처리
- AI 입력 완료 후 `UploadFile`을 닫음
- BE는 AI 호출 후 메모리 바이트 배열을 덮어씀

## 설정값

### BE

- `face.max-image-bytes: 3145728`

### AI

- `ai_max_image_bytes = 3145728`

둘 다 3MB 기준이다.

## 기대 효과

- AI 입력 필드 축소
- 원본 파일 메타데이터 전달 차단
- 대용량 이미지 요청 조기 차단
- 원본 이미지 메모리 잔존 시간 단축

## 남은 과제

- FE에서도 업로드 전 재인코딩으로 EXIF 제거
- AI 응답 필드(`yaw`, `pitch`, `roll`, `message`)를 실제 필요 기준으로 한 번 더 줄일지 검토
- OpenAPI/README에도 새 입력 계약과 에러 코드를 반영
