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

## docker 실행 

- 로컬 DB 세팅 (Docker Postgres)
0) 사전 준비

- Docker Desktop 설치 및 실행

1) 환경변수 파일 생성

naeda/ 폴더에서 실행 (docker-compose.yml 있는 위치)

```copy .env.example .env```

.env는 커밋 금지(.gitignore 처리)

2) Postgres 컨테이너 실행

```docker compose up -d```
```docker ps```
3) DB 스키마 적용 (테이블 생성)

PowerShell은 < 리다이렉션이 안 되므로 파이프 방식 사용

```Get-Content db/schema.sql | docker exec -i naeda-postgres psql -U user -d naeda_db```

4) 테이블 생성 확인

```docker exec -it naeda-postgres psql -U user -d naeda_db -c "\dt"```
5) (선택) DB 초기화가 필요할 때

- 컨테이너만 내리고 다시 올리기(데이터 유지됨)

```docker compose down```
```docker compose up -d```

- 데이터까지 완전 초기화(주의: DB 데이터 삭제)

```docker compose down -v```
```docker compose up -d```
