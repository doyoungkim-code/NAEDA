# 결제 요청 API

> Base URL: `/api/payment-requests`

단말기(키오스크)가 결제 요청을 생성하고, 폴링으로 결제 결과를 확인하는 비동기 결제 흐름 API.
결제 요청은 Redis에 임시 저장되며 PENDING 상태에서는 TTL 30초, 처리 시작(PROCESSING) 이후에는 TTL 300초(5분)가 적용된다.

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
| 404 | 요청 만료 또는 미존재 | 결제 요청이 만료되었거나 존재하지 않습니다. |

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

## 4. 결제 처리 (얼굴 인증 + 이체)

사용자 앱에서 얼굴 이미지를 전송하면, AI 서버로 임베딩 벡터 추출 → DB 등록 벡터와 코사인 유사도 비교 → RBA 평가 → SSAFY 계좌 이체 → 포인트 적립까지 일괄 처리한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/payment-requests/{requestId}/process` |
| **Content-Type** | `multipart/form-data` |
| **Auth** | JWT |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| requestId | String | O | 결제 요청 ID (UUID) |

### Request (multipart/form-data)

| 파트명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| faceImage | File | O | 얼굴 이미지 (JPEG/PNG) |

### Response

**Status: `200 OK`**

**케이스 1 — 결제 성공 (PASS)**

```json
{
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "paymentId": 50,
  "status": "SUCCESS",
  "nextAction": "PASS",
  "storeId": 100,
  "amount": 15000,
  "earnedPoints": 750,
  "ssafyTransactionId": "TXN-001",
  "similarity": 0.95,
  "failureReason": null
}
```

**케이스 2 — 2차 인증 필요 (REQUIRE_SECOND_FACTOR)**

```json
{
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "paymentId": 51,
  "status": "FAILED",
  "nextAction": "REQUIRE_SECOND_FACTOR",
  "storeId": 100,
  "amount": 15000,
  "earnedPoints": null,
  "ssafyTransactionId": null,
  "similarity": 0.75,
  "failureReason": null
}
```

**케이스 3 — 얼굴 차단 (BLOCK)**

```json
{
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "paymentId": null,
  "status": "BLOCKED",
  "nextAction": "BLOCK",
  "storeId": 100,
  "amount": 15000,
  "earnedPoints": null,
  "ssafyTransactionId": null,
  "similarity": 0.3,
  "failureReason": null
}
```

**케이스 4 — SSAFY API 오류**

```json
{
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "paymentId": null,
  "status": "FAILED",
  "nextAction": null,
  "storeId": 100,
  "amount": 15000,
  "earnedPoints": null,
  "ssafyTransactionId": null,
  "similarity": null,
  "failureReason": "SSAFY API 오류: 서버 응답 없음"
}
```

### ProcessPaymentResponse 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| requestId | String | 결제 요청 ID |
| paymentId | Long | 결제 PK (BLOCK 시 null) |
| status | String | 처리 결과 (`SUCCESS` / `FAILED` / `BLOCKED`) |
| nextAction | String | 다음 액션 (`PASS` / `REQUIRE_SECOND_FACTOR` / `BLOCK`) |
| storeId | Long | 매장 ID |
| amount | Long | 결제 금액 |
| earnedPoints | Integer | 적립 포인트 (결제금액 x 5%, 성공 시) |
| ssafyTransactionId | String | SSAFY 거래 고유번호 (성공 시) |
| similarity | Double | 얼굴 코사인 유사도 (0~1, ≥0.7 MATCH, 0.65~0.7 AMBIGUOUS, <0.65 NO_MATCH) |
| failureReason | String | 실패 사유 (API 오류 등) |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 얼굴 이미지 누락 | 요청 파라미터를 확인해주세요. |
| 400 | 이미 처리 중이거나 완료된 요청 | 이미 처리 중이거나 완료된 결제 요청입니다. |
| 400 | 동시 요청 (분산 락 충돌) | 이미 처리 중이거나 완료된 결제 요청입니다. |
| 400 | 계좌 외 결제수단 | 계좌 결제 수단만 지원합니다. |
| 400 | 중복 거래 ID | 이미 처리된 결제입니다. |
| 404 | 요청 만료 또는 미존재 | 결제 요청이 만료되었거나 존재하지 않습니다. |
| 404 | 얼굴 인식 사용자 없음 | 얼굴 인식된 사용자를 찾을 수 없습니다. |
| 404 | 페이스페이 결제수단 미등록 | 페이스페이 결제 수단이 등록되지 않았습니다. |
| 404 | 출금/입금 계좌 없음 | 출금 계좌를 찾을 수 없습니다. / 매장 입금 계좌를 찾을 수 없습니다. |

---

## Response 필드 설명 (폴링 응답)

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

- **PENDING**: 단말기가 요청 생성 직후 (TTL 30초)
- **PROCESSING**: 사용자 앱에서 얼굴 인증 시작 (TTL 300초로 연장)
- **SUCCESS**: 결제 완료
- **FAILED**: 결제 실패 (2차 인증 필요, SSAFY API 오류 등)
- **BLOCKED**: 얼굴 매칭 실패로 차단
- **만료**: Redis TTL 경과 시 자동 삭제 (404 반환)

---

## 처리 흐름

```
단말기 (키오스크)                              사용자 (모바일 앱)
    │                                            │
    │  POST /api/payment-requests                │
    │  { storeId, amount }                       │
    │  → requestId 수신 (PENDING, TTL 30초)      │
    │                                            │
    │  GET /api/payment-requests/{requestId}     │
    │  → PENDING (반복 폴링, 1~2초 간격)          │
    │                                            │
    │                                            │  POST /api/payment-requests/{requestId}/process
    │                                            │  (faceImage 전송)
    │                                            │
    │                                            │  1. 분산 락 획득 (SETNX, 동시 요청 방지)
    │                                            │  2. PENDING → PROCESSING (TTL 300초로 연장)
    │                                            │  3. AI 서버에 이미지 전송 → 512차원 임베딩 벡터 추출
    │                                            │     (POST /internal/v1/embeddings/extract)
    │                                            │  4. DB 등록 임베딩과 코사인 유사도 계산
    │                                            │  5. RBA 평가 (유사도 + 금액 → AuthLevel 결정)
    │                                            │     - MATCH (≥0.7) + 일반금액 → PASS
    │                                            │     - AMBIGUOUS (0.65~0.7) → REQUIRE_SECOND_FACTOR
    │                                            │     - MATCH + 고액(≥5만원) → REQUIRE_SECOND_FACTOR
    │                                            │     - NO_MATCH (<0.65) → BLOCK
    │                                            │  6. SSAFY 계좌 이체 API 호출
    │                                            │  7. Payment DB 저장
    │                                            │  8. 포인트 적립 (결제금액 x 5%)
    │                                            │  9. PROCESSING → SUCCESS
    │                                            │
    │  GET /api/payment-requests/{requestId}     │
    │  → SUCCESS (결제 완료!)                     │
    │                                            │
```

## Redis 키 구조

| 키 패턴 | 타입 | TTL | 설명 |
|---------|------|-----|------|
| `payment:request:{requestId}` | STRING (JSON) | PENDING: 30초, 처리 중: 300초 | 결제 요청 데이터 |
| `payment:store:{storeId}:requests` | SET | 60초 | 매장별 활성 requestId 인덱스 |
| `payment:lock:{requestId}` | STRING | 300초 | 동시 요청 방지 분산 락 (SETNX) |

---

## 얼굴 인증 상세

결제 처리 시 얼굴 인증은 다음 단계로 진행된다.

```
사용자 앱                    Backend                         AI 서버
  │                           │                                │
  │  faceImage (MultipartFile)│                                │
  │ ─────────────────────────>│                                │
  │                           │  POST /internal/v1/embeddings/extract
  │                           │  (multipart: image)            │
  │                           │ ──────────────────────────────>│
  │                           │                                │
  │                           │  512차원 float[] 임베딩 벡터    │
  │                           │  + qualityScore, yaw, pitch    │
  │                           │ <──────────────────────────────│
  │                           │                                │
  │                           │  DB 저장 임베딩과 코사인 유사도 계산
  │                           │  → 최고 유사도 사용자 특정
  │                           │                                │
  │                           │  RBA 평가 (유사도 + 금액)
  │                           │  → AuthLevel + nextAction 결정
  │                           │                                │
  │  ProcessPaymentResponse   │                                │
  │ <─────────────────────────│                                │
```

### RBA AuthLevel 판정 기준

| 유사도 | 금액 | FaceMatchStatus | AuthLevel | nextAction |
|--------|------|-----------------|-----------|------------|
| ≥ 0.7 | < 5만원 | MATCH | FACE_ONLY | PASS |
| ≥ 0.7 | ≥ 5만원 | MATCH | FACE_SIGNATURE | REQUIRE_SECOND_FACTOR |
| 0.65 ~ 0.7 | < 5만원 | AMBIGUOUS | FACE_PHONE | REQUIRE_SECOND_FACTOR |
| 0.65 ~ 0.7 | ≥ 5만원 | AMBIGUOUS | FACE_SIGNATURE | REQUIRE_SECOND_FACTOR |
| < 0.65 | - | NO_MATCH | BLOCKED | BLOCK |
