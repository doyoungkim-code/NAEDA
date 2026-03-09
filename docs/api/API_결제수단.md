# 결제수단 API

> Base URL: `/api/payment-methods`

---

## 1. 결제 수단 목록 조회

사용자의 등록된 결제 수단 목록을 조회한다. 카드 등록(POST /api/cards) 시 자동 생성된다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/payment-methods` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Response

**Status: `200 OK`**

```json
[
  {
    "paymentMethodId": 1,
    "userNo": 1,
    "methodType": "CREDIT_CARD",
    "accountId": null,
    "debitCardId": null,
    "creditCardId": 10,
    "isDefault": true,
    "isFacePay": true,
    "created": "2025-03-09T10:00:00"
  },
  {
    "paymentMethodId": 2,
    "userNo": 1,
    "methodType": "DEBIT_CARD",
    "accountId": null,
    "debitCardId": 5,
    "creditCardId": null,
    "isDefault": false,
    "isFacePay": false,
    "created": "2025-03-09T10:05:00"
  }
]
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| paymentMethodId | Long | 결제 수단 PK |
| userNo | Long | 사용자 번호 |
| methodType | String | 결제 수단 종류 (`ACCOUNT` / `DEBIT_CARD` / `CREDIT_CARD`) |
| accountId | Long | 계좌 ID (ACCOUNT 타입 시) |
| debitCardId | Long | 체크카드 ID (DEBIT_CARD 타입 시) |
| creditCardId | Long | 신용카드 ID (CREDIT_CARD 타입 시) |
| isDefault | Boolean | 기본 결제 수단 여부 |
| isFacePay | Boolean | 페이스페이 결제 수단 여부 |
| created | LocalDateTime | 등록 일시 |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo 파라미터 누락 | 요청 파라미터를 확인해주세요. |

---

## 2. 페이스페이 결제 수단 지정

특정 결제 수단을 페이스페이 전용으로 지정한다. 기존에 `isFacePay=true`인 수단이 있으면 자동으로 해제되고 새 수단으로 교체된다.

> 페이스페이 결제(POST /api/payments) 사용 전 반드시 설정 필요.

| 항목 | 내용 |
|------|------|
| **Method** | `PATCH` |
| **URL** | `/api/payment-methods/{paymentMethodId}/face-pay` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| paymentMethodId | Long | O | 페이스페이로 지정할 결제 수단 PK |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

없음

### Response

**Status: `204 No Content`**

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 본인 소유가 아닌 결제 수단 | 본인의 결제 수단만 설정할 수 있습니다. |
| 400 | userNo 파라미터 누락 | 요청 파라미터를 확인해주세요. |
| 404 | 존재하지 않는 결제 수단 ID | 존재하지 않는 결제 수단입니다. |

---

## 처리 흐름

```
카드 등록 (POST /api/cards)
  └─ PaymentMethod 자동 생성 (isFacePay=false)

페이스페이 수단 지정 (PATCH /api/payment-methods/{id}/face-pay)
  1. paymentMethodId로 PaymentMethod 조회
  2. userNo 소유권 검증
  3. 기존 isFacePay=true 수단 → clearFacePay() (isFacePay=false)
  4. 지정 수단 → setAsFacePay() (isDefault=true, isFacePay=true)

결제 (POST /api/payments)
  └─ findByUserNoAndIsFacePayTrue() → 페이스페이 수단 자동 조회
```
