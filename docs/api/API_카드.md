# 카드 API

> Base URL: `/api/cards`

---

## 1. 카드 등록

SSAFY 카드 생성 API를 호출하여 카드를 등록한다. 응답의 cardTypeCode에 따라 신용카드/체크카드로 분기 저장하고, 결제수단(PaymentMethod)을 자동 생성한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/cards` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| userKey | String | O | SSAFY API userKey |

### Request Body

```json
{
  "cardUniqueNo": "1003-a139e9f23f1a4cc",
  "withdrawalAccountNo": "032355504232351",
  "withdrawalDate": "15"
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| cardUniqueNo | String | O | NotBlank | SSAFY 카드 상품 고유번호 |
| withdrawalAccountNo | String | O | NotBlank | 결제 연결 계좌번호 |
| withdrawalDate | String | O | NotBlank | 결제일 (일) |

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
| 400 | userNo 또는 userKey 파라미터 누락 | 요청 파라미터를 확인해주세요. |
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

## 카드 등록 처리 흐름

1. SSAFY createCreditCard API 호출 (카드 생성)
2. 응답에서 카드 정보 및 cardTypeCode 추출
3. 카드번호 중복 체크 (CreditCard + DebitCard 모두 확인)
4. withdrawalAccountNo로 Account 조회 → accountId 매핑
5. cardTypeCode 분기:
   - `"1"` (신용카드): CreditCard 저장 (creditLimit = maxBenefitLimit, billingDate = withdrawalDate)
   - 그 외 (체크카드): DebitCard 저장
6. PaymentMethod 자동 생성 (methodType: CREDIT_CARD 또는 DEBIT_CARD)
7. 등록 결과 반환
