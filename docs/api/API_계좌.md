# 계좌 API

> Base URL: `/api/accounts`

---

## 1. 계좌 목록 조회

사용자의 전체 계좌 목록을 조회한다. SSAFY 금융 API에서 실시간 데이터를 가져오고, DB에 저장된 계좌 정보와 병합하여 반환한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/accounts` |
| **Auth** | - |

### Query Parameters

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
    "accountId": 1,
    "bankCode": "999",
    "bankName": "싸피은행",
    "accountNo": "9990000000001234",
    "accountName": "내 계좌",
    "accountBalance": 5000000,
    "currency": "KRW"
  },
  {
    "accountId": null,
    "bankCode": "001",
    "bankName": "한국은행",
    "accountNo": "0010000000005678",
    "accountName": "한국은행 입출금",
    "accountBalance": 1000000,
    "currency": "KRW"
  }
]
```

> `accountId`가 `null`인 경우 SSAFY API에만 존재하고 아직 DB에 저장되지 않은 계좌이다.

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo 파라미터 누락 | Required parameter 'userNo' is not present |
| 404 | 존재하지 않는 사용자 | 존재하지 않는 사용자입니다. |

---

## 2. 계좌 단건 조회

특정 계좌번호로 계좌 상세 정보를 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/accounts/{accountNo}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| accountNo | String | O | SSAFY 계좌번호 |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

없음

### Response

**Status: `200 OK`**

```json
{
  "accountId": 1,
  "bankCode": "999",
  "bankName": "싸피은행",
  "accountNo": "9990000000001234",
  "accountName": "내 계좌",
  "accountBalance": 3000000,
  "currency": "KRW"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo 파라미터 누락 | Required parameter 'userNo' is not present |
| 404 | 존재하지 않는 계좌 | 계좌 정보를 찾을 수 없습니다. |

---

## Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| accountId | Long (nullable) | DB 계좌 PK. SSAFY에만 존재하면 null |
| bankCode | String | 은행 코드 |
| bankName | String | 은행명 |
| accountNo | String | 계좌번호 |
| accountName | String | 계좌 별칭 (DB 값 우선, 없으면 SSAFY 값) |
| accountBalance | Long | 계좌 잔액 |
| currency | String | 통화 코드 (예: KRW) |
