# 포인트 API

> Base URL: `/api/points`

---

## 1. 포인트 지갑 생성

사용자의 포인트 지갑을 생성한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/points/wallet/{userNo}` |
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
  "walletId": 1,
  "userNo": 100,
  "balance": 0,
  "totalEarned": 0,
  "totalUsed": 0
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 500 | 이미 지갑이 존재하는 경우 | 이미 포인트 지갑이 존재합니다. userNo: {userNo} |

---

## 2. 포인트 잔액 조회

사용자의 포인트 지갑 정보를 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/points/wallet/{userNo}` |
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
  "walletId": 1,
  "userNo": 100,
  "balance": 5000,
  "totalEarned": 10000,
  "totalUsed": 5000
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 500 | 지갑이 존재하지 않는 경우 | 포인트 지갑이 존재하지 않습니다. userNo: {userNo} |

---

## 3. 포인트 적립

포인트를 적립한다. 비관적 락(SELECT FOR UPDATE)을 사용하여 동시성을 제어한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/points/wallet/{userNo}/earn` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

```json
{
  "amount": 1000,
  "description": "결제 적립",
  "paymentId": 42
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| amount | Long | O | NotNull, Positive | 적립 금액 (양수) |
| description | String | X | 최대 255자 | 적립 사유 |
| paymentId | Long | X | - | 연관 결제 ID |

### Response

**Status: `200 OK`**

```json
{
  "walletId": 1,
  "userNo": 100,
  "balance": 6000,
  "totalEarned": 11000,
  "totalUsed": 5000
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | amount가 null이거나 0 이하 | Validation 에러 |
| 500 | 지갑이 존재하지 않는 경우 | 포인트 지갑이 존재하지 않습니다. userNo: {userNo} |

---

## 4. 포인트 사용

포인트를 사용한다. 비관적 락(SELECT FOR UPDATE)을 사용하여 동시성을 제어한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/points/wallet/{userNo}/use` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

```json
{
  "amount": 500,
  "description": "쿠폰 교환"
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| amount | Long | O | NotNull, Positive | 사용 금액 (양수) |
| description | String | X | 최대 255자 | 사용 사유 |

### Response

**Status: `200 OK`**

```json
{
  "walletId": 1,
  "userNo": 100,
  "balance": 5500,
  "totalEarned": 11000,
  "totalUsed": 5500
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | amount가 null이거나 0 이하 | Validation 에러 |
| 500 | 지갑이 존재하지 않는 경우 | 포인트 지갑이 존재하지 않습니다. userNo: {userNo} |
| 500 | 잔액 부족 | 포인트가 부족합니다. 현재 잔액: {balance} |

---

## 5. 포인트 이력 조회

포인트 적립/사용 이력을 최신순으로 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/points/wallet/{userNo}/histories` |
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
[
  {
    "historyId": 2,
    "type": "USE_COUPON",
    "amount": 500,
    "balanceAfter": 5500,
    "description": "쿠폰 교환",
    "created": "2026-03-03T15:30:00"
  },
  {
    "historyId": 1,
    "type": "EARN",
    "amount": 1000,
    "balanceAfter": 6000,
    "description": "결제 적립",
    "created": "2026-03-03T15:00:00"
  }
]
```

### PointType enum

| 값 | 설명 |
|----|------|
| `EARN` | 포인트 적립 |
| `USE_COUPON` | 포인트 사용 (쿠폰 교환) |
| `CHARGE` | 포인트 충전 |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 500 | 지갑이 존재하지 않는 경우 | 포인트 지갑이 존재하지 않습니다. userNo: {userNo} |

---

## 공통 사항

### 동시성 제어

포인트 적립(`earn`)과 사용(`use`) 시 **비관적 락(Pessimistic Lock)**을 적용한다.
- `SELECT ... FOR UPDATE` 쿼리로 해당 지갑 row를 잠근 뒤 잔액을 변경한다.
- 동시 요청 시 먼저 도착한 트랜잭션이 완료될 때까지 다음 트랜잭션이 대기한다.

### Response 공통 구조

현재 별도의 공통 응답 래퍼 없이 DTO를 직접 반환한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| walletId | Long | 지갑 PK |
| userNo | Long | 사용자 번호 |
| balance | Long | 현재 잔액 |
| totalEarned | Long | 누적 적립액 |
| totalUsed | Long | 누적 사용액 |
