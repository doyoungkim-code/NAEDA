# 알림 설정 API

> Base URL: `/api/notification-settings`

---

## 1. 알림 설정 조회

사용자의 알림 수신 설정을 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/notification-settings/{userNo}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

없음

### Response

**Status: `200 OK`**

```json
{
  "settingId": 1,
  "paymentEnabled": true,
  "fdsEnabled": true,
  "festivalEnabled": true,
  "pointEnabled": true,
  "systemEnabled": true
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| settingId | Long | 설정 PK |
| paymentEnabled | Boolean | 결제 알림 수신 여부 |
| fdsEnabled | Boolean | FDS 경고 알림 수신 여부 |
| festivalEnabled | Boolean | 축제/이벤트 알림 수신 여부 |
| pointEnabled | Boolean | 포인트 알림 수신 여부 |
| systemEnabled | Boolean | 시스템 알림 수신 여부 |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 404 | 설정이 존재하지 않는 경우 | 알림 설정을 찾을 수 없습니다. userNo: {userNo} |

---

## 2. 알림 설정 생성

사용자의 알림 수신 설정을 생성한다. 모든 항목이 기본값 `true`로 생성된다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/notification-settings/{userNo}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

없음

### Response

**Status: `201 Created`**

```json
{
  "settingId": 1,
  "paymentEnabled": true,
  "fdsEnabled": true,
  "festivalEnabled": true,
  "pointEnabled": true,
  "systemEnabled": true
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 409 | 이미 설정이 존재하는 경우 | 알림 설정이 이미 존재합니다. userNo: {userNo} |

---

## 3. 알림 설정 수정

사용자의 알림 수신 설정을 수정한다. 모든 항목을 한 번에 전달해야 한다.

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/notification-settings/{userNo}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

```json
{
  "paymentEnabled": true,
  "fdsEnabled": true,
  "festivalEnabled": false,
  "pointEnabled": true,
  "systemEnabled": false
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| paymentEnabled | Boolean | O | @NotNull | 결제 알림 수신 여부 |
| fdsEnabled | Boolean | O | @NotNull | FDS 경고 알림 수신 여부 |
| festivalEnabled | Boolean | O | @NotNull | 축제/이벤트 알림 수신 여부 |
| pointEnabled | Boolean | O | @NotNull | 포인트 알림 수신 여부 |
| systemEnabled | Boolean | O | @NotNull | 시스템 알림 수신 여부 |

### Response

**Status: `200 OK`**

```json
{
  "settingId": 1,
  "paymentEnabled": true,
  "fdsEnabled": true,
  "festivalEnabled": false,
  "pointEnabled": true,
  "systemEnabled": false
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 필드 누락 | Validation 에러 |
| 404 | 설정이 존재하지 않는 경우 | 알림 설정을 찾을 수 없습니다. userNo: {userNo} |

---

## 공통 사항

### 알림 유형

| 설정 필드 | 알림 유형 | 설명 |
|-----------|-----------|------|
| paymentEnabled | PAYMENT | 결제 완료/실패 알림 |
| fdsEnabled | FDS_ALERT | 이상 거래 탐지 경고 알림 |
| festivalEnabled | FESTIVAL | 축제/이벤트 알림 |
| pointEnabled | POINT | 포인트 적립/사용 알림 |
| systemEnabled | SYSTEM | 시스템 공지 알림 |

### 설정 규칙

- 사용자당 **1개의 설정만** 존재한다 (`user_no` UNIQUE 제약).
- 생성 시 모든 알림이 **기본 활성화(true)** 상태로 생성된다.
- 수정 시 모든 필드를 함께 전달해야 한다 (부분 수정 미지원).