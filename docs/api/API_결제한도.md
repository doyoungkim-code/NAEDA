# 결제 한도 API

> Base URL: `/api/payment/limit`

---

## 1. 결제 한도 조회

사용자의 결제 한도를 조회한다. 미설정 시 기본값을 반환한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/payment/limit` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Response

**Status: `200 OK`**

```json
{
  "userNo": 1,
  "dailyLimit": 500000,
  "monthlyLimit": 3000000,
  "singleTransactionLimit": 300000
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| userNo | Long | 사용자 번호 |
| dailyLimit | Long | 1일 결제 한도 (원) |
| monthlyLimit | Long | 월 결제 한도 (원) |
| singleTransactionLimit | Long | 1회 결제 한도 (원) |

### 기본값 (미설정 시)

| 항목 | 기본값 |
|------|--------|
| dailyLimit | 500,000원 |
| monthlyLimit | 3,000,000원 |
| singleTransactionLimit | 300,000원 |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo 파라미터 누락 | 요청 파라미터를 확인해주세요. |

---

## 2. 결제 한도 설정/수정

사용자의 1일/월/1회 결제 한도를 설정하거나 수정한다. 기존 설정이 없으면 새로 생성하고, 있으면 업데이트한다.

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/payment/limit` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

```json
{
  "dailyLimit": 1000000,
  "monthlyLimit": 5000000,
  "singleTransactionLimit": 500000
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| dailyLimit | Long | O | 1일 결제 한도 (원), 양수만 허용 |
| monthlyLimit | Long | O | 월 결제 한도 (원), 양수만 허용 |
| singleTransactionLimit | Long | O | 1회 결제 한도 (원), 양수만 허용 |

### 상한선

| 항목 | 최대값 |
|------|--------|
| dailyLimit | 5,000,000원 |
| monthlyLimit | 30,000,000원 |
| singleTransactionLimit | 3,000,000원 |

### Response

**Status: `200 OK`**

```json
{
  "userNo": 1,
  "dailyLimit": 1000000,
  "monthlyLimit": 5000000,
  "singleTransactionLimit": 500000
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo 파라미터 누락 | 요청 파라미터를 확인해주세요. |
| 400 | 필수 필드 누락 또는 음수 값 | 요청 본문을 확인해주세요. |
| 400 | 1일 한도 상한 초과 | 1일 한도는 최대 5000000원까지 설정 가능합니다. |
| 400 | 월 한도 상한 초과 | 월 한도는 최대 30000000원까지 설정 가능합니다. |
| 400 | 1회 한도 상한 초과 | 1회 한도는 최대 3000000원까지 설정 가능합니다. |

---

## 처리 흐름

```
회원가입 (POST /api/auth/signup)
  └─ PaymentLimit 기본값 자동 생성 (createDefaultLimit)

한도 조회 (GET /api/payment/limit?userNo=)
  1. userNo로 PaymentLimit 조회
  2. 설정이 없으면 기본값 반환 (DB 저장 없이)

한도 설정 (PUT /api/payment/limit?userNo=)
  1. 요청값 상한선 검증
  2. userNo로 기존 설정 조회
  3-a. 기존 설정 있음 → updateLimits() (dirty checking)
  3-b. 기존 설정 없음 → 새 PaymentLimit 생성 후 save
```