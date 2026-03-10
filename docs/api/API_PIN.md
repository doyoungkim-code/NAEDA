# PIN API

> Base URL: `/api`

---

## 1. 회원가입 시 PIN 설정

회원가입 요청에 `pin` 필드를 포함해 페이스페이용 6자리 PIN을 함께 설정한다.

PIN은 서버에서 평문으로 저장하지 않고 해시값으로 저장한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/auth/signup` |
| **Auth** | - |

### Request Body

```json
{
  "userId": "hong123@ssafy.co.kr",
  "password": "password123!",
  "username": "홍길동",
  "residentNo": "9001011",
  "phone": "01012345678",
  "institutionCode": "001",
  "pin": "123456"
}
```

### Request 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | String | O | 로그인 아이디 |
| password | String | O | 로그인 비밀번호 |
| username | String | O | 사용자 이름 |
| residentNo | String | O | 주민등록번호 앞 7자리 |
| phone | String | O | 전화번호 |
| institutionCode | String | O | 기관코드 |
| pin | String | O | 페이스페이 PIN 6자리 숫자 |

### 검증 규칙

- `pin`은 반드시 숫자 6자리여야 한다.
- 예: `123456`
- 허용되지 않는 예:
  - `12345`
  - `1234567`
  - `12ab56`

### Response

**Status: `201 Created`**

```json
{
  "userNo": 1,
  "userId": "hong123@ssafy.co.kr",
  "username": "홍길동",
  "userKey": "cf1d49ba-663b-495d-9227-fc2643aa7c5e",
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | PIN 형식 오류 | 입력값이 올바르지 않습니다. |
| 409 | 중복 아이디 | 이미 존재하는 아이디입니다. |
| 502 | SSAFY API 연동 실패 | SSAFY API 오류 메시지 |

---

## 2. PIN 설정/변경

로그인 사용자의 페이스페이 PIN을 설정하거나 변경한다.

기존 PIN이 없는 사용자는 `newPin`만 보내면 초기 설정이 가능하다.  
기존 PIN이 이미 있는 사용자는 `currentPin`과 `newPin`을 함께 보내야 한다.

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/users/me/pin` |
| **Auth** | Bearer Access Token 필요 |

### Request Body

#### 2-1. 초기 설정

```json
{
  "newPin": "123456"
}
```

#### 2-2. PIN 변경

```json
{
  "currentPin": "123456",
  "newPin": "654321"
}
```

### Request 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| currentPin | String | 조건부 | 기존 PIN이 이미 있는 경우 필수 |
| newPin | String | O | 새로 설정할 PIN 6자리 숫자 |

### 검증 규칙

- `newPin`은 반드시 숫자 6자리여야 한다.
- 기존 PIN이 저장된 사용자라면 `currentPin` 입력이 필요하다.
- `currentPin`이 저장된 PIN과 일치해야 한다.
- `newPin`은 기존 PIN과 같을 수 없다.

### Response

**Status: `200 OK`**

```json
{
  "pinSet": true,
  "message": "PIN 변경이 완료되었습니다."
}
```

초기 설정 성공 시:

```json
{
  "pinSet": true,
  "message": "PIN 설정이 완료되었습니다."
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| pinSet | Boolean | PIN 저장 여부 |
| message | String | 처리 결과 메시지 |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | `newPin` 형식 오류 | 입력값이 올바르지 않습니다. |
| 400 | 기존 PIN 존재 상태에서 `currentPin` 누락 | 현재 PIN을 입력해주세요. |
| 400 | 새 PIN과 현재 PIN이 동일 | 새 PIN은 현재 PIN과 달라야 합니다. |
| 401 | 현재 PIN 불일치 | 현재 PIN이 일치하지 않습니다. |
| 404 | 로그인 사용자 없음 | 사용자를 찾을 수 없습니다. |

---

## 처리 흐름

### 회원가입 시 PIN 저장

```
POST /api/auth/signup
  1. 회원가입 요청 수신
  2. pin 형식 검증 (숫자 6자리)
  3. passwordEncoder.encode(pin)
  4. User.pinPassword에 해시 저장
  5. 회원가입 완료
```

### PIN 변경

```
PUT /api/users/me/pin
  1. JWT 기준 로그인 사용자 확인
  2. 사용자 조회
  3. 기존 PIN 존재 여부 확인
  4. 기존 PIN이 있으면 currentPin 검증
  5. newPin 형식 검증
  6. passwordEncoder.encode(newPin)
  7. User.pinPassword 갱신
```

---

## 보안 정책

- PIN은 평문 저장하지 않는다.
- PIN은 `BCrypt` 기반 해시로 저장한다.
- PIN 조회 API는 제공하지 않는다.
- 결제 시 PIN 확인은 저장된 해시와 `matches` 비교로 처리한다.
