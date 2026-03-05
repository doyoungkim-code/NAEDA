# naedafront - Android

내다(NAEDA) 사용자 앱 (Android)

## 기술 스택

| 항목 | 버전 |
|------|------|
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 9.0.1 |
| Compile SDK | 36 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |
| UI | Jetpack Compose + Material3 |

### 주요 의존성

- Jetpack Compose (BOM 2024.09.00)
- Material3
- Lifecycle Runtime KTX
- Activity Compose

## 프로젝트 구조

```
naedafront/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/naedafront/
│   │   │   └── res/
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml      # 버전 카탈로그
├── build.gradle.kts
└── settings.gradle.kts
```

## 환경 설정

### 필수 도구

- Android Studio (최신 버전 권장)
- JDK 11 이상

### 설정 방법

1. Android Studio에서 `naedafront/` 디렉토리를 Open
2. Gradle Sync 완료 대기
3. 에뮬레이터 또는 실기기 연결

## 빌드 및 실행

```bash
# 빌드
./gradlew assembleDebug

# 테스트
./gradlew test
```

또는 Android Studio에서 `Run` 버튼으로 실행
