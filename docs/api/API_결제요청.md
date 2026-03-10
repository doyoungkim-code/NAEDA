# 결제 요청 API

> Base URL: `/api/payment-requests`

단말기(키오스크)가 결제 요청을 생성하고, 폴링으로 결제 결과를 확인하는 비동기 결제 흐름 API.
결제 요청은 Redis에 임시 저장되며 TTL 30초 후 자동 만료된다.

---

## 1. 결제 요청 생성

단말기에서 매장 ID와 결제 금액을 전송하면, Redis에 PENDING 상태로 결제 요청을 생성한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/payment-requests` |
| **Content-Type** | `application/json` |
| **Auth** | JWT |

### Request Body

```json
{
  "storeId": 100,
  "amount": 15000
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| storeId | Long | O | NotNull | 매장 ID |
| amount | Long | O | NotNull, Positive, Max(100,000,000) | 결제 금액 (원) |

### Response

**Status: `201 Created`**

```json
{
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "storeId": 100,
  "amount": 15000,
  "status": "PENDING",
  "userNo": null,
  "nextAction": null,
  "paymentId": null,
  "failureReason": null,
  "createdAt": 1710000000000,
  "updatedAt": 1710000000000
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 금액이 0 이하 또는 1억 초과 | 요청 파라미터를 확인해주세요. |
| 400 | 페이스페이 미지원 매장 | 해당 매장은 페이스페이를 지원하지 않습니다. |
| 404 | 존재하지 않는 매장 | 존재하지 않는 매장입니다. |

---

## 2. 결제 요청 상태 조회 (폴링)

requestId로 결제 요청의 현재 상태를 조회한다. 단말기에서 1~2초 간격으로 반복 호출하여 결제 결과를 확인한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/payment-requests/{requestId}` |
| **Auth** | JWT |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| requestId | String | O | 결제 요청 ID (UUID) |

### Response

**Status: `200 OK`**

**케이스 1 — 대기 중 (PENDING)**

```json
{
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "storeId": 100,
  "amount": 15000,
  "status": "PENDING",
  "userNo": null,
  "nextAction": null,
  "paymentId": null,
  "failureReason": null,
  "createdAt": 1710000000000,
  "updatedAt": 1710000000000
}
```

**케이스 2 — 결제 성공 (SUCCESS)**

```json
{
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "storeId": 100,
  "amount": 15000,
  "status": "SUCCESS",
  "userNo": 10,
  "nextAction": "PASS",
  "paymentId": 42,
  "failureReason": null,
  "createdAt": 1710000000000,
  "updatedAt": 1710000005000
}
```

**케이스 3 — 결제 실패 (FAILED)**

```json
{
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "storeId": 100,
  "amount": 15000,
  "status": "FAILED",
  "userNo": 10,
  "nextAction": "REQUIRE_SECOND_FACTOR",
  "paymentId": null,
  "failureReason": "2차 인증 필요",
  "createdAt": 1710000000000,
  "updatedAt": 1710000005000
}
```

**케이스 4 — 차단 (BLOCKED)**

```json
{
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "storeId": 100,
  "amount": 15000,
  "status": "BLOCKED",
  "userNo": null,
  "nextAction": "BLOCK",
  "paymentId": null,
  "failureReason": "얼굴 매칭 실패",
  "createdAt": 1710000000000,
  "updatedAt": 1710000005000
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 404 | 요청 만료(TTL 30초) 또는 미존재 | 결제 요청이 만료되었거나 존재하지 않습니다. |

---

## 3. 매장별 결제 요청 목록 조회

매장 ID로 현재 활성 결제 요청 목록을 조회한다. 만료된 요청은 자동으로 제거된다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/payment-requests` |
| **Auth** | JWT |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| storeId | Long | O | 매장 ID |

### Response

**Status: `200 OK`**

```json
[
  {
    "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "storeId": 100,
    "amount": 15000,
    "status": "PENDING",
    "userNo": null,
    "nextAction": null,
    "paymentId": null,
    "failureReason": null,
    "createdAt": 1710000000000,
    "updatedAt": 1710000000000
  },
  {
    "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "storeId": 100,
    "amount": 30000,
    "status": "PROCESSING",
    "userNo": 10,
    "nextAction": null,
    "paymentId": null,
    "failureReason": null,
    "createdAt": 1710000010000,
    "updatedAt": 1710000015000
  }
]
```

> 활성 요청이 없으면 빈 배열 `[]` 반환.

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | storeId 파라미터 누락 | 요청 파라미터를 확인해주세요. |

---

## Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| requestId | String | 결제 요청 ID (UUID) |
| storeId | Long | 매장 ID |
| amount | Long | 결제 금액 |
| status | String | 요청 상태 (`PENDING` / `PROCESSING` / `SUCCESS` / `FAILED` / `BLOCKED`) |
| userNo | Long | 결제 사용자 번호 (결제 처리 전 null) |
| nextAction | String | 다음 액션 (`PASS` / `REQUIRE_SECOND_FACTOR` / `BLOCK`) |
| paymentId | Long | 결제 PK (결제 성공 시) |
| failureReason | String | 실패/차단 사유 |
| createdAt | long | 생성 시각 (epoch milliseconds) |
| updatedAt | long | 최종 수정 시각 (epoch milliseconds) |

---

## 상태 전이

```
PENDING → PROCESSING → SUCCESS
                     → FAILED
                     → BLOCKED
```

- **PENDING**: 단말기가 요청 생성 직후 (사용자 앱의 처리 대기 중)
- **PROCESSING**: 사용자 앱에서 얼굴 인증 시작
- **SUCCESS**: 결제 완료
- **FAILED**: 결제 실패 (2차 인증 필요 등)
- **BLOCKED**: 얼굴 매칭 실패로 차단
- **만료**: Redis TTL 30초 경과 시 자동 삭제 (404 반환)

---

## 처리 흐름

```
단말기 (키오스크)                              사용자 (모바일 앱)
    │                                            │
    │  POST /api/payment-requests                │
    │  { storeId, amount }                       │
    │  → requestId 수신 (PENDING)                │
    │                                            │
    │  GET /api/payment-requests/{requestId}     │
    │  → PENDING (반복 폴링, 1~2초 간격)          │
    │                                            │  얼굴 인증 → RBA → 결제 실행
    │                                            │  → 상태: PROCESSING → SUCCESS
    │  GET /api/payment-requests/{requestId}     │
    │  → SUCCESS (결제 완료!)                     │
    │                                            │
```
