# 🐳 NAEDA 로컬 Docker 환경 가이드

> **개발 환경 구성:** PostgreSQL + Redis + AI 서버를 Docker로 실행
> Spring 백엔드는 **각자 IntelliJ에서 직접 실행**합니다.
>
> 다음 주 EC2 발급 후에는 Docker를 EC2에 올려 팀 전체가 같은 자원을 공유하며 개발합니다.
> 그 전까지는 이 가이드에 따라 **각자 로컬에서 Docker를 실행**합니다.

---

## 📁 프로젝트 구조

```
S14P21D103/
├── infra/                     ← 인프라 설정 (여기서 docker compose 실행)
│   ├── docker-compose.yml
│   ├── .env.example           ← 이걸 복사해서 .env 생성
│   ├── .env                   ← 직접 생성 (Git 제외 - 절대 커밋 금지)
│   └── DOCKER_GUIDE.md
├── naeda/                     ← Spring 백엔드
│   └── db/
│       ├── schema.sql         ← 테이블/ENUM/FK DDL (최초 1회 자동 실행)
│       ├── add_dummy_user.sql ← 더미 유저 데이터
│       └── add_face_embeddings.sql
├── naedafront/                ← 프론트엔드
└── AI/                        ← AI 서버 (FastAPI)
    └── Dockerfile
```

---

## ⚡ 빠른 시작 (요약)

```bash
cd S14P21D103/infra
cp .env.example .env      # .env 생성 후 SSAFY_API_KEY, FACE_ENCRYPT_KEY 값 채우기
docker compose up -d      # 3개 서비스 실행
docker compose ps         # 상태 확인 (모두 healthy 확인)
```

그 다음 IntelliJ에서 Spring 실행하면 끝!

> ⚠️ **Windows 사용자 필독:** 로컬에 PostgreSQL이 설치되어 있으면 포트 충돌로 Spring이 Docker DB가 아닌 로컬 DB에 접속합니다. 반드시 [트러블슈팅 → 로컬 PostgreSQL 포트 충돌](#-로컬-postgresql-포트-충돌-windows-필독) 항목을 먼저 확인하세요.

---

## 📋 목차

1. [사전 준비](#사전-준비)
2. [최초 설정](#최초-설정)
3. [Docker 실행 및 확인](#docker-실행-및-확인)
4. [Spring 백엔드 실행](#spring-백엔드-실행)
5. [DB 초기화 안내](#db-초기화-안내)
6. [종료 방법](#종료-방법)
7. [자주 쓰는 명령어](#자주-쓰는-명령어)
8. [트러블슈팅](#트러블슈팅)

---

## 사전 준비

### Docker Desktop 설치
- [Docker Desktop 다운로드](https://www.docker.com/products/docker-desktop/)
- 설치 후 **Docker Desktop 앱 실행** (상단 바 고래 아이콘 확인)

### 설치 확인
```bash
docker --version
docker compose version
```

### ⚠️ 로컬 PostgreSQL 확인 (중요)

SSAFY 교육용 PC 등에 PostgreSQL이 이미 설치되어 있으면 Docker와 같은 5432 포트를 사용하여 충돌이 발생합니다. Docker 실행 전에 반드시 확인하세요.

```bash
# Windows
netstat -ano | findstr :5432

# Mac/Linux
lsof -i :5432
```

결과가 나오면 로컬 PostgreSQL이 실행 중입니다. [트러블슈팅 → 로컬 PostgreSQL 포트 충돌](#-로컬-postgresql-포트-충돌-windows-필독) 항목을 참고하여 해결하세요.

---

## 최초 설정

### 1. .env 파일 생성

```bash
cd S14P21D103/infra

# Mac/Linux
cp .env.example .env

# Windows (PowerShell)
Copy-Item .env.example .env
```

### 2. .env 파일 수정

`.env` 파일을 열어서 아래 두 항목을 팀 공유 자료의 실제 값으로 교체:

```
SSAFY_API_KEY=팀에서_공유한_키_입력
FACE_ENCRYPT_KEY=팀에서_공유한_키_입력
```

> ⚠️ `.env` 파일은 절대 Git에 커밋하지 마세요!

### 3. .gitignore 확인

루트 `.gitignore`에 아래 내용이 있는지 확인 (없으면 추가):
```
infra/.env
```

---

## Docker 실행 및 확인

> 모든 명령어는 **`infra/` 폴더 안에서** 실행합니다.

### 실행

```bash
cd S14P21D103/infra
docker compose up -d
```

### 상태 확인

```bash
docker compose ps
```

아래처럼 3개 모두 **healthy** 상태여야 합니다:

```
NAME             STATUS
naeda-postgres   running (healthy)
naeda-redis      running (healthy)
naeda-ai         running (healthy)
```

> `health: starting` 상태면 30초 정도 기다린 후 다시 확인하세요.

### 서비스 접속 정보

| 서비스 | 주소 | 비고 |
|--------|------|------|
| PostgreSQL | `localhost:5432` | DB: naeda, User: naedauser |
| Redis | `localhost:6379` | 패스워드: redis1234! |
| AI 서버 | `http://localhost:8000` | FastAPI |
| AI Swagger | `http://localhost:8000/docs` | API 문서 |

### AI 서버 동작 확인

```bash
curl http://localhost:8000/health
# → {"status":"ok"} 가 나오면 정상
```

---

## Spring 백엔드 실행

Docker 3개 서비스가 모두 **healthy** 상태인 것을 확인한 후 IntelliJ에서 실행합니다.

### IntelliJ EnvFile 플러그인 설정 (권장)

1. `Settings → Plugins` 에서 **EnvFile** 검색 후 설치 및 재시작
2. 상단 Run/Debug Configurations 열기 (실행 버튼 옆 드롭다운 → Edit Configurations)
3. `EnvFile` 탭 → `Enable EnvFile` 체크
4. `+` 버튼 → `infra/.env` 파일 선택
5. Spring 실행 ▶️

### application.yml 주의사항

비밀번호에 `!` 특수문자가 포함되어 있으므로 YAML에서 반드시 **따옴표로 감싸야** 합니다:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/naeda
    username: naedauser
    password: "naeda1234!"    # ← 따옴표 필수 (! 는 YAML 특수문자)
```

> ⚠️ 따옴표 없이 `password: naeda1234!` 라고 쓰면 YAML 파서가 `!`를 태그로 해석하여 인증 실패가 발생할 수 있습니다.

### Spring이 연결하는 주소

```
PostgreSQL : localhost:5432/naeda
Redis      : localhost:6379
AI 서버    : http://localhost:8000
```

### 정상 실행 확인

Spring 로그에서 아래 내용이 보이면 정상입니다:

```
HikariPool-1 - Start completed.
Database version: 16.x          ← 반드시 16.x 인지 확인!
Started NaedaApplication in x.xxx seconds
```

> ⚠️ `Database version`이 16이 아닌 다른 버전(12, 15, 18 등)으로 표시되면 Docker PostgreSQL이 아닌 로컬 PostgreSQL에 접속한 것입니다. [트러블슈팅](#트러블슈팅) 참고.

---

## DB 초기화 안내

### 자동 초기화 방식

`naeda/db/` 폴더 안의 `.sql` 파일들이 PostgreSQL 컨테이너 **최초 실행 시 자동으로 실행**됩니다.

```
naeda/db/
├── schema.sql               ← ENUM, 테이블, FK 생성
├── add_dummy_user.sql       ← 더미 유저 데이터 삽입
└── add_face_embeddings.sql  ← 더미 얼굴 임베딩 삽입
```

> ⚠️ **중요:** 이 초기화는 **볼륨이 비어있을 때만** 실행됩니다.
> 이미 `docker compose up`을 한 번 했다면 SQL이 다시 실행되지 않습니다.

### DB를 초기화하고 싶을 때 (스키마 변경 등)

```bash
# infra/ 폴더에서 실행
# 볼륨까지 삭제 후 재시작 → SQL 재실행됨
docker compose down -v
docker compose up -d
```

### DB 직접 접속

```bash
docker exec -it naeda-postgres psql -U naedauser -d naeda

# 접속 후 테이블 확인
\dt

# 종료
\q
```

---

## 종료 방법

```bash
# infra/ 폴더에서 실행

# 서비스 중지 (데이터 유지)
docker compose stop

# 서비스 중지 + 컨테이너 삭제 (데이터 유지)
docker compose down

# 데이터까지 완전 삭제 ⚠️ DB 초기화됨
docker compose down -v
```

---

## 자주 쓰는 명령어

```bash
# infra/ 폴더에서 실행

# 전체 로그 실시간 보기
docker compose logs -f

# 특정 서비스 로그만 보기
docker compose logs -f postgres
docker compose logs -f redis
docker compose logs -f ai-server

# 특정 서비스만 재시작
docker compose restart ai-server

# AI 서버 코드 변경 후 재빌드
docker compose up -d --build ai-server

# PostgreSQL CLI 접속
docker exec -it naeda-postgres psql -U naedauser -d naeda

# Redis CLI 접속
docker exec -it naeda-redis redis-cli -a redis1234!
```

---

## 트러블슈팅

### ❌ 로컬 PostgreSQL 포트 충돌 (Windows 필독)

**증상:** Spring 실행 시 `password authentication failed for user "naedauser"` 에러 발생. application.yml에 비밀번호를 하드코딩해도 계속 실패.

**원인:** SSAFY 교육용 PC 등에 PostgreSQL이 설치되어 있으면 동일한 5432 포트를 점유합니다. Spring이 `localhost:5432`로 접속하면 Docker PostgreSQL이 아닌 **로컬 PostgreSQL에 먼저 연결**되어, naedauser가 존재하지 않으므로 인증이 실패합니다.

**진단 방법:**

```bash
# 1. 5432 포트를 점유하는 프로세스 확인
netstat -ano | findstr :5432

# 2. 프로세스가 2개 이상 보이면 포트 충돌
#    PID 확인 후 어떤 프로세스인지 식별
tasklist | findstr <PID>
#    → postgres.exe 가 나오면 로컬 PostgreSQL이 실행 중

# 3. 서비스 이름 확인
sc query type=service state=all | findstr /i postgres
```

**Spring 로그에서도 확인 가능:**

```
Database version: 18.0    ← Docker PostgreSQL은 16인데 다른 버전이면 로컬 DB에 접속 중!
```

**해결 방법:**

```bash
# 관리자 권한 CMD에서 실행 (시작 메뉴 → cmd 검색 → 우클릭 → 관리자 권한으로 실행)

# 1. 서비스 이름 확인
sc query type=service state=all | findstr /i postgres
# → SERVICE_NAME: postgresql-x64-18  (버전에 따라 다름)

# 2. 서비스 중지
net stop postgresql-x64-18

# 3. (권장) 자동 시작 비활성화 — 재부팅해도 충돌 방지
sc config postgresql-x64-18 start=demand
```

> 💡 `start=demand`로 설정하면 재부팅 시 자동 시작되지 않습니다. 나중에 로컬 PostgreSQL이 필요하면 `net start postgresql-x64-18`로 수동 시작하세요.

### ❌ 포트 충돌 에러 (Docker 시작 시)

```
Error: Bind for 0.0.0.0:5432 failed: port is already allocated
```

Docker 자체가 시작되지 않는 경우입니다. 위의 [로컬 PostgreSQL 포트 충돌](#-로컬-postgresql-포트-충돌-windows-필독) 해결 후 다시 시도하세요.

### ❌ YAML 비밀번호 파싱 에러

**증상:** 로컬 PostgreSQL을 끈 뒤에도 인증 실패가 발생하는 경우.

`application.yml`에서 비밀번호의 `!`가 따옴표로 감싸져 있는지 확인:

```yaml
# ❌ 잘못된 예 — YAML이 ! 를 태그로 해석할 수 있음
password: naeda1234!

# ✅ 올바른 예
password: "naeda1234!"
```

### ❌ Docker Desktop이 실행 안 됨

명령어 실행 전 Docker Desktop 앱을 먼저 실행하세요.

### ❌ naeda-ai가 healthy가 안 됨

```bash
docker compose logs ai-server
```

로그 확인 후 에러 내용을 팀 채널에 공유해주세요.

### ❌ Spring에서 DB 연결 실패 (일반)

```bash
docker compose ps   # postgres가 healthy인지 먼저 확인
```

`naeda-postgres`가 `running (healthy)` 상태여야 Spring이 연결됩니다.

### ❌ 스키마가 적용 안 됨 (테이블이 없다는 에러)

볼륨이 이미 존재하면 SQL이 실행되지 않습니다. 아래 명령으로 초기화:

```bash
docker compose down -v
docker compose up -d
```

---

## 📌 개발 환경 구성도

```
[내 PC]
├── Docker Desktop  (infra/ 에서 실행)
│   ├── naeda-postgres  PostgreSQL 16  :5432  ✅
│   ├── naeda-redis     Redis 7.2      :6379  ✅
│   └── naeda-ai        FastAPI        :8000  ✅
│
└── IntelliJ
    └── Spring Boot :8080
        ├── → PostgreSQL :5432  연결
        ├── → Redis :6379       연결
        └── → AI서버 :8000      연결
```

### EC2 전환 후 (다음 주 예정)

```
[EC2 서버]
├── naeda-postgres  PostgreSQL 16  :5432
├── naeda-redis     Redis 7.2      :6379
└── naeda-ai        FastAPI        :8000

[각자 PC]
└── IntelliJ
    └── Spring Boot :8080
        ├── → EC2:5432  PostgreSQL 연결
        ├── → EC2:6379  Redis 연결
        └── → EC2:8000  AI서버 연결
```

> EC2 전환 시에는 `application.yml`의 `localhost`를 EC2 IP 주소로 변경하면 됩니다.

---

> 문의사항은 팀 채널에 올려주세요 🙌