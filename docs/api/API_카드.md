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

## 처리 흐름

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
