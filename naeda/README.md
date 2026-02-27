# naeda - Backend

내다(NAEDA) 백엔드 서버

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 17 |
| Spring Boot | 3.5.11 |
| Gradle | Groovy DSL |
| Database | PostgreSQL |

### 주요 의존성

- Spring Data JPA
- Spring Validation
- Spring Web
- Lombok
- Spring Boot DevTools

## 프로젝트 구조

```
naeda/
├── src/
│   ├── main/
│   │   ├── java/com/ssafy/naeda/
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
├── build.gradle
└── settings.gradle
```

## 환경 변수 설정

`src/main/resources/` 아래에 `application-secret.yaml` 파일을 생성하고 다음 내용을 작성:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/naeda
    username: {DB_USERNAME}
    password: {DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
```

> `application-secret.yaml`은 `.gitignore`에 포함되어 있어 git에 올라가지 않습니다.

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

## 포트

- 기본 포트: `8080`
