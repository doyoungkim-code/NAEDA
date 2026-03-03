# AItestFront + AI 작업 정리

## 1) 목표
- 테스트용 Android Compose 앱(`AItestFront`)에서
  - 얼굴 등록
  - 얼굴 찾기(인식)
  - 결과 확인
  흐름을 빠르게 검증할 수 있도록 구현.

## 2) 프론트엔드(앱) 구현 내용

### 메인 화면
- 이름 입력
- `얼굴 등록하기` 버튼
- `얼굴 찾기` 버튼

### 얼굴 등록 화면
- 카메라 기반 자동 인식/자동 촬영/자동 저장
- 현재 등록 순서:
  1. 정면1
  2. 정면2
  3. 정면3
  4. 좌
  5. 우
  6. 상
  7. 하
- 포즈가 맞아도 즉시 촬영하지 않고 **0.5초 유지 후 촬영**
- 환경/가림 안내 및 촬영 차단:
  - 어두움: `밝은곳에서 촬영해주세요.`
  - 마스크 감지: `마스크를 제거해주세요.`
  - 안경 감지: `안경을 벗어주세요.`
- 상단 고정 안내:
  - `안경/마스크 등 악세사리를 제거하고 밝은 환경에서 촬영해주세요.`
- 오버레이 가이드:
  - 세로로 긴 얼굴형 타원
  - 타원 크기 확대(화면 대비 비율 상향)
  - 가로/세로 십자선
  - 정면은 중앙 십자선
  - 좌/우/상/하에서는 선 끝점은 타원 경계에 고정된 상태로 곡선 휨
  - 곡선 휨 강도 추가 상향(방향별 시인성 강화)

### 얼굴 찾기 화면
- Passive liveness 통과 후 인식 진행
- 초당 최대 10회(100ms 간격) 인식 요청
- 임계치(threshold) 통과 시 즉시 결과 화면 전환

### 결과 화면
- 이름
- 유사도
- threshold
- 보조 벡터 유사도(포즈별)
  - front / left / right / up / down
- `뒤로가기` 버튼

## 3) 백엔드(AI 서버) 변경 내용

### front 3장 저장
- 기존: `(user_id, pose)` 1개 upsert(덮어쓰기)
- 변경:
  - `pose=front` 등록 시 내부 슬롯 3개 사용
  - `front`, `front_2`, `front_3` 순으로 저장
  - 이미 3개가 있으면 가장 오래된 슬롯 교체
- API 응답은 호환성 유지:
  - 내부 슬롯이 `front_2`, `front_3`여도 응답 pose는 `front`로 반환

### threshold
- 현재 threshold: **0.73**
  - `.env`: `SIMILARITY_THRESHOLD=0.73`
  - `app/core/config.py`: `similarity_threshold: float = 0.73`

## 4) 연결/실행 관련 정리
- 앱 기본 서버 주소는 현재 `AI_BASE_URL = "http://10.92.75.87:8000"`로 설정됨.
- 핫스팟/와이파이 환경에서 PC IP가 바뀌면 `AI_BASE_URL`도 같이 변경 필요.
- 에뮬레이터는 `10.0.2.2`, USB reverse 방식은 `127.0.0.1` 사용 가능.

## 5) 안정화 이슈 처리
- 카메라 화면 전환 시 발생하던 앱 크래시(`RejectedExecutionException`) 수정:
  - ML Kit 비동기 콜백 체인 대신 `Tasks.await(...)` 방식으로 분석 처리
  - 해제 시 executor `shutdownNow()` 적용

## 6) 주요 수정 파일
- `AItestFront/app/src/main/java/com/example/naedafront/FaceTestApp.kt`
- `AItestFront/app/src/main/AndroidManifest.xml`
- `AItestFront/app/build.gradle.kts`
- `AItestFront/gradle/libs.versions.toml`
- `AI/app/db/repository.py`
- `AI/app/api/face.py`
- `AI/app/core/config.py`
- `AI/.env`
- `AI/.env.example`

## 7) 검증
- Android 앱: `:app:assembleDebug` 빌드 통과
- AI 서버 파이썬 코드: `python -m compileall app` 통과
- front 3장 저장: DB에 `front/front_2/front_3` 저장 확인
