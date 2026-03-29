# FCM (Firebase Cloud Messaging) 가이드

> Spring Boot + Firebase Admin SDK 푸시 알림 구현

---

## 1. FCM이란?

Google이 제공하는 **무료** 푸시 알림 서비스다.

```
우리 서버  →  Google FCM 서버  →  사용자 폰 📱 띠링!
```

| 개념 | 설명 |
|------|------|
| **FCM Token** | 각 디바이스(폰)의 고유 주소 (앱 설치 시 자동 발급) |
| **Service Account Key** | 서버가 Google에 신분 증명하는 JSON 키 파일 |
| **Firebase Admin SDK** | 서버에서 FCM을 사용하기 위한 Java 라이브러리 |

우리 프로젝트는 **Token 기반 방식** 사용 — 개별 발송(결제 알림)과 전체 발송(축제 알림) 모두 가능.

---

## 2. 동작 흐름

```
[사전 준비] 앱 실행 → FCM Token 발급 → 서버에 전송 → DB 저장
                                        PUT /api/users/me/fcm-token

[알림 발송] 관리자가 API 호출 → 서버가 FCM Token 있는 유저 조회
            POST /api/festivals/1/notify
                  ↓
            각 유저에게 Firebase SDK로 발송 → Google → 폰
                  ↓
            모든 유저에게 notification 테이블에 이력 저장
```

---

## 3. Firebase Console 설정

### 1단계: Firebase 프로젝트 만들기

1. 브라우저에서 [Firebase Console](https://console.firebase.google.com/) 접속 (Google 계정 로그인)
2. **"프로젝트 추가"** 버튼 클릭
3. 프로젝트 이름 입력 → `naeda`
4. Google Analytics → **비활성화** 해도 됨 (FCM이랑 상관없음)
5. **"프로젝트 만들기"** 클릭 → 30초 정도 기다리면 완료

### 2단계: Android 앱 등록

프로젝트가 만들어지면 대시보드가 뜨는데:

1. 화면 중앙에 **Android 아이콘** 클릭 (앱 추가)
2. **Android 패키지 이름**: `com.ssafy.naeda` 입력
3. 앱 닉네임: 아무거나 (빈칸이어도 됨)
4. SHA-1: 지금은 빈칸으로 스킵해도 됨
5. **"앱 등록"** 클릭
6. `google-services.json` **다운로드** → `naedafront/app/` 폴더에 넣기
7. 나머지 단계 (SDK 추가 등)는 **"다음"** 눌러서 스킵 → **"콘솔로 이동"**

### 3단계: Service Account Key 발급 (서버용 - 가장 중요!)

이게 Spring Boot 서버에서 FCM 보내는 데 필요한 **열쇠**다.

1. 왼쪽 상단 **톱니바퀴 아이콘** 클릭 → **"프로젝트 설정"**
2. 상단 탭에서 **"서비스 계정"** 클릭
3. **"Firebase Admin SDK"** 가 보임
4. 프로그래밍 언어: **Java** 선택 (코드 샘플이 바뀌는데, 무시해도 됨)
5. 아래쪽 **"새 비공개 키 생성"** 버튼 클릭
6. 경고 팝업 → **"키 생성"** 클릭
7. JSON 파일이 자동 다운로드됨 (예: `naeda-firebase-adminsdk-xxxxx-abc123.json`)

```
⚠️ 이 JSON 파일은 절대 Git에 커밋하지 말 것! .gitignore에 추가!
```

### 4단계: 키 파일을 서버에 연결

다운로드된 JSON 파일을 프로젝트에 두고 환경변수로 경로를 잡아주면 된다.

```bash
# 예시: naeda 폴더 안에 넣는 경우 (Git에 안 올라가게 .gitignore 추가!)
FCM_CREDENTIALS_PATH=C:/Users/SSAFY/project/S14P21D103/naeda/firebase-key.json
```

**IntelliJ에서 설정하는 법:**
1. 우측 상단 **실행 버튼 옆 드롭다운** → **"Edit Configurations..."**
2. Spring Boot 실행 설정 선택
3. **"Environment variables"** 란에 입력:
   ```
   FCM_CREDENTIALS_PATH=C:/Users/SSAFY/project/S14P21D103/naeda/firebase-key.json
   ```
4. **OK** → 서버 재시작

서버 로그에 `[Firebase] 초기화 완료.`가 뜨면 성공!

### 요약

| 순서 | 할 일 | 결과물 |
|------|--------|--------|
| 1 | Firebase 프로젝트 생성 | 프로젝트 대시보드 |
| 2 | Android 앱 등록 | `google-services.json` → `naedafront/app/`에 넣기 |
| 3 | 서비스 계정 → 새 비공개 키 생성 | `xxx-firebase-adminsdk-xxx.json` (서버용) |
| 4 | IntelliJ 환경변수에 키 경로 설정 | 서버 실행 시 `[Firebase] 초기화 완료.` 로그 확인 |

---

## 4. Spring Boot 설정

**build.gradle:**
```gradle
implementation 'com.google.firebase:firebase-admin:9.4.3'
```

**application.yaml:**
```yaml
fcm:
  credentials-path: ${FCM_CREDENTIALS_PATH:}   # 비어있으면 FCM 비활성화 (앱은 정상 실행)
```

**FirebaseConfig.java** — 앱 시작 시 Firebase 초기화:
- 키 파일 경로 없으면 → 경고 로그만 출력, 앱은 정상 동작
- 키 파일 있으면 → `GoogleCredentials.fromStream()` → `FirebaseApp.initializeApp()`

---

## 5. 핵심 코드: FCM 발송

**FcmService.java**의 핵심 로직:

```java
// FCM 메시지 빌드 & 발송 (이게 전부다)
Message message = Message.builder()
        .setToken(token)           // 누구에게 (디바이스 고유 주소)
        .setNotification(          // 무엇을
                Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
        .build();

FirebaseMessaging.getInstance().send(message);  // 발송!
```

**sendToAllUsers() 동작:**
1. FCM Token 있는 유저 조회 → 각각 푸시 발송 + 알림 이력 DB 저장
2. FCM Token 없는 유저 → 알림 이력만 DB 저장 (앱에서 목록 조회 가능)
3. `FcmSendResult(전체유저수, 발송대상수, 성공수, 실패수)` 반환

---

## 6. API 명세

| Method | URL | 설명 |
|--------|-----|------|
| `PUT` | `/api/users/me/fcm-token?userNo=1` | FCM 토큰 등록 `{"fcmToken":"..."}` |
| `POST` | `/api/festivals` | 축제 등록 |
| `GET` | `/api/festivals` | 축제 목록 조회 |
| `GET` | `/api/festivals/{id}` | 축제 단건 조회 |
| `PUT` | `/api/festivals/{id}` | 축제 수정 |
| `DELETE` | `/api/festivals/{id}` | 축제 삭제 |
| **`POST`** | **`/api/festivals/{id}/notify`** | **축제 FCM 알림 발송** |
| `GET` | `/api/notifications?userNo=1` | 내 알림 목록 |
| `PATCH` | `/api/notifications/{id}/read` | 알림 읽음 처리 |
| `GET` | `/api/notifications/unread-count?userNo=1` | 안 읽은 알림 수 |

### 축제 알림 발송 응답 예시
```json
{
    "festivalId": 1,
    "totalUsers": 150,
    "targetUsers": 120,
    "successCount": 118,
    "failCount": 2
}
```

---

## 7. DB 변경사항

```sql
-- user 테이블에 FCM 토큰 컬럼 추가
ALTER TABLE "user" ADD COLUMN fcm_token VARCHAR(255);
```

`notification`, `festival` 테이블은 `schema.sql`에 이미 정의되어 있음.

---

## 8. 프로젝트 구조

```
global/config/FirebaseConfig.java    ← Firebase 초기화
global/fcm/FcmService.java          ← FCM 발송 핵심 로직
global/fcm/FcmSendResult.java       ← 발송 결과 DTO

domain/festival/                     ← 축제 CRUD + FCM 발송 API
domain/notification/                 ← 알림 이력 조회/읽음 처리
domain/user/ (수정)                  ← fcmToken 필드 + 토큰 등록 API
```

---

## 9. 테스트

Firebase 키 없이도 테스트 가능 — 실제 푸시만 안 가고, DB 저장/API 응답은 정상 동작.

```bash
# Swagger UI
http://localhost:8080/swagger-ui.html

# cURL 빠른 테스트
curl -X POST http://localhost:8080/api/festivals \
  -H "Content-Type: application/json" \
  -d '{"title":"테스트 축제","startDate":"2026-04-01","endDate":"2026-04-07"}'

curl -X POST http://localhost:8080/api/festivals/1/notify
```

---

## 10. 주의사항

- **중복 발송 방지**: `festival.fcm_notified = true`면 재발송 차단 (400 에러)
- **PostgreSQL ENUM**: `columnDefinition = "notification_type_enum"` 필수
- **키 파일 노출 시**: Firebase Console에서 즉시 삭제 후 재발급
- **토큰 갱신**: Android 앱이 토큰 갱신 시 `PUT /api/users/me/fcm-token` 재호출 필요
