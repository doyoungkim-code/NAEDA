# 이체 API

> Base URL: `/api/transfers`

---

## 1. 계좌 이체

출금 계좌에서 입금 계좌로 이체를 수행한다. SSAFY 금융 API를 통해 실제 이체를 처리하고, 거래내역(TransactionLog)을 저장한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/transfers` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

```json
{
  "withdrawalAccountNo": "9990000000001234",
  "depositAccountNo": "0010000000005678",
  "amount": 50000,
  "memo": "용돈"
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| withdrawalAccountNo | String | O | NotNull, NotBlank | 출금 계좌번호 |
| depositAccountNo | String | O | NotNull, NotBlank | 입금 계좌번호 |
| amount | Long | O | NotNull, Min(1) | 이체 금액 (1원 이상) |
| memo | String | X | - | 메모 (미입력 시 "이체") |

### Response

**Status: `200 OK`**

```json
{
  "withdrawalAccountNo": "9990000000001234",
  "depositAccountNo": "0010000000005678",
  "amount": 50000,
  "transactionDate": "20260306",
  "withdrawalTransactionNo": "61",
  "depositTransactionNo": "62"
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| withdrawalAccountNo | String | 출금 계좌번호 |
| depositAccountNo | String | 입금 계좌번호 |
| amount | Long | 이체 금액 |
| transactionDate | String | 거래 일자 (yyyyMMdd) |
| withdrawalTransactionNo | String | SSAFY 출금 거래 고유번호 |
| depositTransactionNo | String | SSAFY 입금 거래 고유번호 |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 필드 누락 또는 검증 실패 | Validation 에러 |
| 400 | 이체 금액이 0 이하 | 이체 금액은 1원 이상이어야 합니다. |
| 404 | 존재하지 않는 사용자 | 존재하지 않는 사용자입니다. |
| 404 | 출금 계좌 미존재 또는 소유권 불일치 | 계좌를 찾을 수 없거나 접근 권한이 없습니다. |

---

## 처리 흐름

1. 사용자 조회 → userKey 획득
2. 출금 계좌 소유권 검증 (요청한 userNo가 출금 계좌의 소유자인지 확인)
3. SSAFY 이체 API 호출 (실제 이체 처리)
4. SSAFY 잔액 조회 API 호출 → 이체 후 잔액 확인
5. TransactionLog 저장 (거래유형: WITHDRAW)
6. 이체 결과 반환
