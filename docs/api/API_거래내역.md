# 거래내역 API

> Base URL: `/api/transactions`

---

## 1. 전체 거래내역 조회

특정 계좌의 전체 거래내역을 최신순으로 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/transactions` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| accountId | Long | O | 계좌 PK |

### Request Body

없음

### Response

**Status: `200 OK`**

```json
[
  {
    "logId": 2,
    "accountId": 10,
    "transactionType": "WITHDRAW",
    "amount": 10000,
    "balanceAfter": 90000,
    "counterpart": "편의점",
    "memo": "간식 구매",
    "category": "생활",
    "ssafyTransactionId": "TXN002",
    "transacted": "2026-03-06T15:30:00"
  },
  {
    "logId": 1,
    "accountId": 10,
    "transactionType": "DEPOSIT",
    "amount": 100000,
    "balanceAfter": 100000,
    "counterpart": "홍길동",
    "memo": "용돈",
    "category": "이체",
    "ssafyTransactionId": "TXN001",
    "transacted": "2026-03-06T12:00:00"
  }
]
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 파라미터 누락 | Required parameter is not present |
| 404 | 계좌 미존재 또는 소유권 불일치 | 계좌를 찾을 수 없거나 접근할 수 없습니다. |

---

## 2. 기간 필터 거래내역 조회

특정 기간의 거래내역을 최신순으로 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/transactions/period` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| accountId | Long | O | 계좌 PK |
| from | DateTime | O | 조회 시작일시 (ISO 8601, 예: `2026-01-01T00:00:00`) |
| to | DateTime | O | 조회 종료일시 (ISO 8601, 예: `2026-03-01T00:00:00`) |

### Request Body

없음

### Response

**Status: `200 OK`**

응답 형식은 전체 거래내역 조회와 동일하다.

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 파라미터 누락 | Required parameter is not present |
| 404 | 계좌 미존재 또는 소유권 불일치 | 계좌를 찾을 수 없거나 접근할 수 없습니다. |

---

## 3. 거래유형 필터 거래내역 조회

특정 거래유형의 거래내역을 최신순으로 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/transactions/type` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| accountId | Long | O | 계좌 PK |
| transactionType | String | O | 거래 유형 (`DEPOSIT`, `WITHDRAW`, `TRANSFER`) |

### Request Body

없음

### Response

**Status: `200 OK`**

응답 형식은 전체 거래내역 조회와 동일하다.

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 파라미터 누락 | Required parameter is not present |
| 400 | 잘못된 transactionType 값 | Failed to convert value |
| 404 | 계좌 미존재 또는 소유권 불일치 | 계좌를 찾을 수 없거나 접근할 수 없습니다. |

---

## TransactionType enum

| 값 | 설명 |
|----|------|
| `DEPOSIT` | 입금 |
| `WITHDRAW` | 출금 |
| `TRANSFER` | 이체 |

## Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| logId | Long | 거래내역 PK |
| accountId | Long | 계좌 PK |
| transactionType | String | 거래 유형 |
| amount | Long | 거래 금액 |
| balanceAfter | Long | 거래 후 잔액 |
| counterpart | String | 거래 상대방 |
| memo | String | 메모 |
| category | String | 카테고리 |
| ssafyTransactionId | String | SSAFY 거래 고유번호 |
| transacted | DateTime | 거래 일시 |

## 공통 사항

### 소유권 검증

모든 API는 요청한 `userNo`가 해당 `accountId` 계좌의 소유자인지 검증한다. 소유권이 일치하지 않으면 `404 Not Found`를 반환한다.

### 정렬

모든 거래내역은 `transacted` 기준 **최신순(내림차순)**으로 정렬되어 반환된다.
