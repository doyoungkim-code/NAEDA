# 결제 API

> Base URL: `/api/payments`

---

## 1. 페이스페이 결제

단말기에서 얼굴 이미지와 결제 정보를 전송하면, 얼굴 인식으로 사용자를 특정하고 등록된 페이스페이 결제 수단으로 SSAFY 카드 결제를 실행한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/payments` |
| **Content-Type** | `multipart/form-data` |
| **Auth** | - |

### Request (multipart/form-data)

| 파트명 | 타입 | 필수 | 설명 |
|--------|------|------|------|
| request | JSON | O | 결제 요청 정보 |
| faceImage | File | O | 얼굴 이미지 |

**request 파트 (JSON)**

```json
{
  "storeId": 1,
  "amount": 15000
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| storeId | Long | O | NotNull | 매장 ID (SSAFY merchantId) |
| amount | Long | O | NotNull, Positive | 결제 금액 (원) |

### Response

**Status: `200 OK`**

**케이스 1 — 결제 성공 (`nextAction: "PASS"`)**

```json
{
  "paymentId": 42,
  "userNo": 1,
  "storeId": 1,
  "amount": 15000,
  "authMethod": "FACE_PAY",
  "authLevel": "FACE_ONLY",
  "status": "SUCCESS",
  "earnedPoints": 750,
  "ssafyTransactionId": "12",
  "paid": "2025-03-09T14:30:00",
  "nextAction": "PASS",
  "similarity": 0.92,
  "rbaReason": "매칭 성공: 얼굴만으로 인증 (similarity=0.92, amount=15000)"
}
```

**케이스 2 — 2차 인증 필요 (`nextAction: "REQUIRE_SECOND_FACTOR"`)**

```json
{
  "paymentId": 43,
  "userNo": 1,
  "storeId": 1,
  "amount": 15000,
  "authMethod": "FACE_PAY",
  "authLevel": "FACE_PHONE",
  "status": "FAILED",
  "earnedPoints": 0,
  "ssafyTransactionId": null,
  "paid": "2025-03-09T14:30:00",
  "nextAction": "REQUIRE_SECOND_FACTOR",
  "similarity": 0.67,
  "rbaReason": "애매한 매칭 구간: PHONE 2차 인증 필요 (similarity=0.67, amount=15000)"
}
```

**케이스 3 — 결제 차단 (`nextAction: "BLOCK"`)**

> 얼굴 불일치로 사용자 특정 불가 → Payment DB 미저장

```json
{
  "paymentId": null,
  "userNo": null,
  "storeId": null,
  "amount": null,
  "authMethod": null,
  "authLevel": null,
  "status": "BLOCKED",
  "earnedPoints": null,
  "ssafyTransactionId": null,
  "paid": null,
  "nextAction": "BLOCK",
  "similarity": 0.41,
  "rbaReason": "얼굴 매칭 실패: similarity=0.41"
}
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| paymentId | Long | 결제 PK (BLOCK 시 null) |
| userNo | Long | 인식된 사용자 번호 (BLOCK 시 null) |
| storeId | Long | 매장 ID |
| amount | Long | 결제 금액 |
| authMethod | String | 인증 방식 (`FACE_PAY` / `PIN_FALLBACK`) |
| authLevel | String | RBA 인증 레벨 (`FACE_ONLY` / `FACE_PHONE` / `FACE_SIGNATURE` / `BLOCKED`) |
| status | String | 결제 상태 (`SUCCESS` / `FAILED` / `BLOCKED`) |
| earnedPoints | Integer | 적립 포인트 (결제금액 × 5%) |
| ssafyTransactionId | String | SSAFY 거래 고유번호 (성공 시) |
| paid | LocalDateTime | 결제 일시 |
| nextAction | String | 다음 액션 (`PASS` / `REQUIRE_SECOND_FACTOR` / `BLOCK`) |
| similarity | Double | 얼굴 유사도 (0~1) |
| rbaReason | String | RBA 판정 사유 |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 페이스페이 미지원 매장 | 해당 매장은 페이스페이를 지원하지 않습니다. |
| 400 | 카드 외 결제수단 (ACCOUNT 타입) | 카드 결제 수단만 지원합니다. |
| 404 | 존재하지 않는 매장 | 존재하지 않는 매장입니다. |
| 404 | 얼굴 인식 사용자를 DB에서 찾을 수 없음 | 얼굴 인식된 사용자를 찾을 수 없습니다. |
| 404 | 페이스페이 결제 수단 미등록 | 페이스페이 결제 수단이 등록되지 않았습니다. |
| 404 | 카드 정보 없음 | 신용카드 정보를 찾을 수 없습니다. / 체크카드 정보를 찾을 수 없습니다. |
| 502 | SSAFY API 오류 | (SSAFY 응답 오류 메시지) |

---

## 2. 결제 내역 목록 조회

사용자의 결제 내역을 최신순으로 조회한다. `from` / `to` 파라미터로 기간 필터링 가능.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/payments` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| from | LocalDateTime | X | 조회 시작 일시 (ISO 8601, 예: `2025-01-01T00:00:00`) |
| to | LocalDateTime | X | 조회 종료 일시 (ISO 8601, 예: `2025-12-31T23:59:59`) |

> `from`, `to` 둘 다 지정해야 기간 필터가 적용됨. 하나만 지정하면 전체 조회.

### Response

**Status: `200 OK`**

```json
[
  {
    "paymentId": 42,
    "userNo": 1,
    "storeId": 1,
    "amount": 15000,
    "authMethod": "FACE_PAY",
    "authLevel": "FACE_ONLY",
    "status": "SUCCESS",
    "earnedPoints": 750,
    "ssafyTransactionId": "12",
    "paid": "2025-03-09T14:30:00"
  }
]
```

> `nextAction`, `similarity`, `rbaReason`은 목록 조회 시 포함되지 않음.

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo 파라미터 누락 | 요청 파라미터를 확인해주세요. |

---

## 3. 결제 단건 조회

결제 ID로 단건 결제 내역을 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/payments/{paymentId}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| paymentId | Long | O | 결제 PK |

### Response

**Status: `200 OK`**

```json
{
  "paymentId": 42,
  "userNo": 1,
  "storeId": 1,
  "amount": 15000,
  "authMethod": "FACE_PAY",
  "authLevel": "FACE_ONLY",
  "status": "SUCCESS",
  "earnedPoints": 750,
  "ssafyTransactionId": "12",
  "paid": "2025-03-09T14:30:00"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 404 | 존재하지 않는 결제 ID | 존재하지 않는 결제 내역입니다. |

---

## 처리 흐름

```
단말기 → POST /api/payments (multipart/form-data)

1. 매장 조회 → facePayEnabled 확인
2. FaceService.search(faceImage, topK=1, amount)
   └─ AI 임베딩 추출 → 코사인 유사도 → RbaEngine.evaluate()
3. BLOCKED (similarity < 0.65)
   → Payment 미저장, nextAction="BLOCK" 반환
4. bestUserId → User 조회 → 페이스페이 결제 수단(isFacePay=true) 자동 조회
   → 카드 종류(CREDIT/DEBIT)에 따라 cardNo, cvc 추출
5. REQUIRE_SECOND_FACTOR (similarity 0.65~0.69 또는 고액)
   → Payment(FAILED) 저장, nextAction="REQUIRE_SECOND_FACTOR" 반환
6. PASS (similarity >= 0.70)
   → SSAFY createCreditCardTransaction 호출
      Body: cardNo, cvc, merchantId(=storeId), paymentBalance(=amount)
   → Payment(SUCCESS) 저장, earnedPoints = amount × 5%
   → PointService.earnPoints() 포인트 적립
   → nextAction="PASS" 반환
```

## AuthLevel 상세

| 값 | 조건 | 설명 |
|----|------|------|
| FACE_ONLY | MATCH + 일반 금액 | 얼굴 인증만으로 결제 승인 |
| FACE_PHONE | AMBIGUOUS + 일반 금액 | 얼굴 + 휴대폰 2차 인증 필요 |
| FACE_SIGNATURE | MATCH 또는 AMBIGUOUS + 고액(≥50,000원) | 얼굴 + 전자서명 필요 |
| BLOCKED | NO_MATCH | 결제 차단 |
