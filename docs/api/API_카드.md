# 카드 API

> Base URL: `/api/cards`

---

## 1. 카드 등록

SSAFY 카드 생성 API를 호출하여 카드를 등록한다. `cardTypeCode`에 따라 신용카드/체크카드로 분기 저장하고, 결제수단(PaymentMethod)을 자동 생성한다. userKey는 서버에서 DB 조회하여 사용.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/cards` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

```json
{
  "cardUniqueNo": "1003-a139e9f23f1a4cc",
  "withdrawalAccountNo": "032355504232351",
  "withdrawalDate": "15",
  "cardTypeCode": "1"
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| cardUniqueNo | String | O | NotBlank | SSAFY 카드 상품 고유번호 (API 24에서 확인) |
| withdrawalAccountNo | String | O | NotBlank | 결제 연결 계좌번호 |
| withdrawalDate | String | O | NotBlank | 결제일 (일) |
| cardTypeCode | String | O | NotBlank | 카드 타입 (`"1"` = 신용카드, `"2"` = 체크카드) |

> `cardTypeCode`는 SSAFY API 25 응답에 포함되지 않으므로 클라이언트가 카드 상품 조회(API 24) 시 확인한 값을 직접 전달해야 한다.

### Response

**Status: `201 CREATED`**

```json
{
  "cardId": 100,
  "cardNo": "1003622654847049",
  "cvc": "713",
  "cardUniqueNo": "1003-a139e9f23f1a4cc",
  "cardIssuerCode": "1003",
  "cardIssuerName": "롯데카드",
  "cardName": "디지로카 SEOUL",
  "cardExpiryDate": "20290409",
  "cardType": "CREDIT",
  "withdrawalAccountNo": "032355504232351",
  "withdrawalDate": "15",
  "paymentMethodId": 50
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| cardId | Long | 생성된 카드 PK (credit_card_id 또는 debit_card_id) |
| cardNo | String | 카드 번호 |
| cvc | String | CVC 코드 |
| cardUniqueNo | String | SSAFY 카드 상품 고유번호 |
| cardIssuerCode | String | 카드 발급사 코드 |
| cardIssuerName | String | 카드 발급사 이름 |
| cardName | String | 카드 상품명 |
| cardExpiryDate | String | 카드 만료일 (yyyyMMdd) |
| cardType | String | 카드 유형 (`CREDIT` 또는 `DEBIT`) |
| withdrawalAccountNo | String | 결제 연결 계좌번호 |
| withdrawalDate | String | 결제일 (일) |
| paymentMethodId | Long | 자동 생성된 결제수단 PK |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 필드 누락 또는 검증 실패 | 입력값이 올바르지 않습니다. |
| 400 | userNo 파라미터 누락 | 요청 파라미터를 확인해주세요. |
| 404 | 존재하지 않는 사용자 | 존재하지 않는 사용자입니다. |
| 404 | 연결 계좌를 DB에서 찾을 수 없음 | 연결 계좌를 찾을 수 없습니다: {accountNo} |
| 409 | 이미 등록된 카드번호 | 이미 등록된 카드입니다: {cardNo} |

---

## 2. 카드 목록 조회

사용자의 활성 카드(신용카드 + 체크카드) 목록을 반환한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/cards` |
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
    "cardId": 100,
    "cardNo": "1003000000001111",
    "cvc": "123",
    "cardUniqueNo": "1003-unique-abc",
    "cardIssuerCode": "1003",
    "cardIssuerName": "롯데카드",
    "cardName": "디지로카 SEOUL",
    "cardExpiryDate": "20290101",
    "isActive": true,
    "accountId": 1,
    "cardType": "CREDIT",
    "creditLimit": 1000000,
    "billingDate": 15
  },
  {
    "cardId": 200,
    "cardNo": "1005000000002222",
    "cvc": "456",
    "cardUniqueNo": "1005-unique-def",
    "cardIssuerCode": "1005",
    "cardIssuerName": "신한카드",
    "cardName": "신한 체크카드",
    "cardExpiryDate": "20290101",
    "isActive": true,
    "accountId": 2,
    "cardType": "DEBIT",
    "creditLimit": null,
    "billingDate": null
  }
]
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| cardId | Long | 카드 PK (credit_card_id 또는 debit_card_id) |
| cardNo | String | 카드 번호 |
| cvc | String | CVC 코드 |
| cardUniqueNo | String | SSAFY 카드 상품 고유번호 |
| cardIssuerCode | String | 카드 발급사 코드 |
| cardIssuerName | String | 카드 발급사 이름 |
| cardName | String | 카드 상품명 |
| cardExpiryDate | String | 카드 만료일 (yyyyMMdd) |
| isActive | Boolean | 활성 여부 |
| accountId | Long | 연결 계좌 PK |
| cardType | String | 카드 유형 (`CREDIT` 또는 `DEBIT`) |
| creditLimit | Long | 신용 한도 (신용카드만, 체크카드는 null) |
| billingDate | Integer | 결제일 (신용카드만, 체크카드는 null) |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo 파라미터 누락 | 요청 파라미터를 확인해주세요. |

---

## 3. 카드 삭제

카드를 비활성화(soft delete)하고 연결된 결제수단(PaymentMethod)을 삭제한다.

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/cards/{cardId}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| cardId | Long | O | 삭제할 카드 PK |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| cardType | String | O | 카드 유형 (`CREDIT` 또는 `DEBIT`) |

### Response

**Status: `204 No Content`**

(응답 본문 없음)

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo 또는 cardType 파라미터 누락 | 요청 파라미터를 확인해주세요. |
| 404 | 카드를 찾을 수 없음 | 카드를 찾을 수 없습니다: {cardId} |

### 처리 흐름

1. cardType에 따라 CreditCard 또는 DebitCard 조회
2. 카드 비활성화 (`isActive = false`)
3. 연결된 PaymentMethod 조회 후 삭제

---

## 4. 카드 결제 내역 조회

SSAFY 카드 거래내역 조회 API를 호출하여 결제 내역을 반환한다. 조회된 거래는 transaction_log 테이블에 캐싱하며, 이미 저장된 거래는 DB에서 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/cards/{cardId}/transactions` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| cardId | Long | O | 조회할 카드 PK |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| userKey | String | O | SSAFY API userKey |
| startDate | String | O | 조회 시작일 (yyyyMMdd) |
| endDate | String | O | 조회 종료일 (yyyyMMdd) |

### Response

**Status: `200 OK`**

```json
[
  {
    "logId": 1,
    "transactionUniqueNo": "TX-001",
    "categoryName": "식비",
    "merchantName": "스타벅스",
    "transactionDate": "2024-04-10",
    "transactionTime": "14:30:00",
    "amount": 5000,
    "cardStatus": "승인"
  },
  {
    "logId": 2,
    "transactionUniqueNo": "TX-002",
    "categoryName": "교통",
    "merchantName": "카카오택시",
    "transactionDate": "2024-04-11",
    "transactionTime": "09:15:00",
    "amount": 12000,
    "cardStatus": "승인"
  }
]
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| logId | Long | 거래 로그 PK |
| transactionUniqueNo | String | SSAFY 거래 고유번호 |
| categoryName | String | 거래 카테고리 |
| merchantName | String | 가맹점명 |
| transactionDate | String | 거래 날짜 (yyyy-MM-dd) |
| transactionTime | String | 거래 시간 (HH:mm:ss) |
| amount | Long | 거래 금액 |
| cardStatus | String | 카드 상태 (승인 등) |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo, userKey, startDate, endDate 중 누락 | 요청 파라미터를 확인해주세요. |
| 404 | 카드를 찾을 수 없음 | 카드를 찾을 수 없습니다: {cardId} |

### 처리 흐름

1. cardId로 credit_card → debit_card 순서로 탐색 + 소유자(userNo) 검증
2. SSAFY inquireCreditCardTransactionList API 호출
3. 각 거래에 대해 ssafyTransactionId 기준 중복 체크
   - 이미 저장된 거래: DB에서 조회
   - 새 거래: transaction_log에 저장
4. CardTransactionResponse 리스트로 변환하여 반환

---

## 카드 등록 처리 흐름

1. User 조회 → userKey 획득 (DB 조회, URL 노출 없음)
2. SSAFY createCreditCard API 호출 (`/edu/creditCard/createCreditCardProduct`)
3. 응답에서 cardNo, cvc 등 카드 정보 추출
4. 카드번호 중복 체크 (CreditCard + DebitCard 모두 확인)
5. withdrawalAccountNo로 Account 조회 → accountId 매핑
6. request.cardTypeCode 분기:
   - `"1"` (신용카드): CreditCard 저장 (creditLimit = maxBenefitLimit, billingDate = withdrawalDate)
   - `"2"` (체크카드): DebitCard 저장
7. PaymentMethod 자동 생성 (methodType: CREDIT_CARD 또는 DEBIT_CARD, isFacePay=false)
8. 등록 결과 반환
