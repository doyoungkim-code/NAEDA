# 포인트 상품 API

> Base URL: `/api/products`

---

## 1. 상품 등록

포인트 상품을 등록한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/products` |
| **Auth** | - |

### Request Body

```json
{
  "productName": "아메리카노 쿠폰",
  "description": "스타벅스 아메리카노 교환권",
  "category": "카페",
  "imageUrl": "https://example.com/image.png",
  "pointPrice": 3000,
  "stockQuantity": 100,
  "startsAt": "2026-01-01T00:00:00",
  "endsAt": "2026-12-31T23:59:00"
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| productName | String | O | NotBlank, 최대 120자 | 상품명 |
| description | String | X | 최대 500자 | 상품 설명 |
| category | String | X | 최대 50자 | 상품 카테고리 |
| imageUrl | String | X | 최대 500자 | 이미지 URL |
| pointPrice | Long | O | NotNull, Positive, 최대 1억 | 포인트 가격 |
| stockQuantity | Integer | O | NotNull, 0 이상 | 재고 수량 |
| startsAt | LocalDateTime | X | - | 판매 시작일 |
| endsAt | LocalDateTime | X | - | 판매 종료일 |

### Response

**Status: `201 Created`**

```json
{
  "productId": 1,
  "productName": "아메리카노 쿠폰",
  "description": "스타벅스 아메리카노 교환권",
  "category": "카페",
  "imageUrl": "https://example.com/image.png",
  "pointPrice": 3000,
  "stockQuantity": 100,
  "status": "ON_SALE",
  "startsAt": "2026-01-01T00:00:00",
  "endsAt": "2026-12-31T23:59:00"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 필드 누락 또는 검증 실패 | Validation 에러 |

---

## 2. 상품 단건 조회

포인트 상품을 단건 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/products/{productId}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| productId | Long | O | 상품 ID |

### Request Body

없음

### Response

**Status: `200 OK`**

```json
{
  "productId": 1,
  "productName": "아메리카노 쿠폰",
  "description": "스타벅스 아메리카노 교환권",
  "category": "카페",
  "imageUrl": "https://example.com/image.png",
  "pointPrice": 3000,
  "stockQuantity": 100,
  "status": "ON_SALE",
  "startsAt": "2026-01-01T00:00:00",
  "endsAt": "2026-12-31T23:59:00"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 404 | 상품이 존재하지 않는 경우 | 포인트 상품을 찾을 수 없습니다. id={productId} |

---

## 3. 상품 전체 목록 조회

등록된 모든 포인트 상품을 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/products` |
| **Auth** | - |

### Request Body

없음

### Response

**Status: `200 OK`**

```json
[
  {
    "productId": 1,
    "productName": "아메리카노 쿠폰",
    "description": "스타벅스 아메리카노 교환권",
    "category": "카페",
    "imageUrl": "https://example.com/image.png",
    "pointPrice": 3000,
    "stockQuantity": 100,
    "status": "ON_SALE",
    "startsAt": "2026-01-01T00:00:00",
    "endsAt": "2026-12-31T23:59:00"
  },
  {
    "productId": 2,
    "productName": "치킨 교환권",
    "description": "BBQ 황금올리브 교환권",
    "category": "음식",
    "imageUrl": "https://example.com/chicken.png",
    "pointPrice": 10000,
    "stockQuantity": 50,
    "status": "ON_SALE",
    "startsAt": null,
    "endsAt": null
  }
]
```

---

## 4. 판매중 상품 목록 조회

판매중(ON_SALE) 상태의 포인트 상품을 조회한다. 카테고리 필터와 키워드 검색을 선택적으로 사용할 수 있다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/products/available` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| category | String | X | 카테고리 필터 (예: 카페, 음식) |
| keyword | String | X | 상품명 부분 검색 |

### 사용 예시

```
GET /api/products/available                                    # 전체 판매중 상품
GET /api/products/available?category=카페                       # 카페 카테고리만
GET /api/products/available?keyword=쿠폰                       # "쿠폰" 포함 검색
GET /api/products/available?category=카페&keyword=아메리카노     # 카페 + "아메리카노" 검색
```

### Request Body

없음

### Response

**Status: `200 OK`**

```json
[
  {
    "productId": 1,
    "productName": "아메리카노 쿠폰",
    "description": "스타벅스 아메리카노 교환권",
    "category": "카페",
    "imageUrl": "https://example.com/image.png",
    "pointPrice": 3000,
    "stockQuantity": 100,
    "status": "ON_SALE",
    "startsAt": "2026-01-01T00:00:00",
    "endsAt": "2026-12-31T23:59:00"
  }
]
```

---

## 5. 상품 수정

포인트 상품 정보를 수정한다.

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/products/{productId}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| productId | Long | O | 상품 ID |

### Request Body

```json
{
  "productName": "아메리카노 쿠폰 (수정)",
  "description": "스타벅스 아메리카노 교환권 - 한정판",
  "category": "카페",
  "imageUrl": "https://example.com/image_v2.png",
  "pointPrice": 3500,
  "stockQuantity": 80,
  "status": "ON_SALE",
  "startsAt": "2026-01-01T00:00:00",
  "endsAt": "2026-06-30T23:59:00"
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| productName | String | O | NotBlank, 최대 120자 | 상품명 |
| description | String | X | 최대 500자 | 상품 설명 |
| category | String | X | 최대 50자 | 상품 카테고리 |
| imageUrl | String | X | 최대 500자 | 이미지 URL |
| pointPrice | Long | O | NotNull, Positive, 최대 1억 | 포인트 가격 |
| stockQuantity | Integer | O | NotNull, 0 이상 | 재고 수량 |
| status | String | O | NotNull | 상품 상태 (ON_SALE / SOLD_OUT) |
| startsAt | LocalDateTime | X | - | 판매 시작일 |
| endsAt | LocalDateTime | X | - | 판매 종료일 |

### Response

**Status: `200 OK`**

```json
{
  "productId": 1,
  "productName": "아메리카노 쿠폰 (수정)",
  "description": "스타벅스 아메리카노 교환권 - 한정판",
  "category": "카페",
  "imageUrl": "https://example.com/image_v2.png",
  "pointPrice": 3500,
  "stockQuantity": 80,
  "status": "ON_SALE",
  "startsAt": "2026-01-01T00:00:00",
  "endsAt": "2026-06-30T23:59:00"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 필드 누락 또는 검증 실패 | Validation 에러 |
| 404 | 상품이 존재하지 않는 경우 | 포인트 상품을 찾을 수 없습니다. id={productId} |

---

## 6. 상품 삭제

포인트 상품을 삭제한다.

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/products/{productId}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| productId | Long | O | 상품 ID |

### Request Body

없음

### Response

**Status: `204 No Content`**

(응답 본문 없음)

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 404 | 상품이 존재하지 않는 경우 | 포인트 상품을 찾을 수 없습니다. id={productId} |

---

## 7. 포인트 상품 구매

포인트를 사용하여 상품을 구매한다. 포인트 차감, 재고 차감, 포인트 이력 기록, 주문 생성이 하나의 트랜잭션으로 처리된다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/orders` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

```json
{
  "productId": 1,
  "roadAddress": "구미시 인동중앙로 100",
  "numberAddress": "인동동 123-4"
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| productId | Long | O | NotNull | 상품 ID |
| roadAddress | String | X | - | 도로명 주소 |
| numberAddress | String | X | - | 지번 주소 |

### Response

**Status: `201 Created`**

```json
{
  "orderId": 1,
  "userNo": 1,
  "productId": 1,
  "productName": "아메리카노 쿠폰",
  "pointPrice": 3000,
  "roadAddress": "구미시 인동중앙로 100",
  "numberAddress": "인동동 123-4",
  "orderAt": "2026-03-10T15:30:00"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | productId 누락 | Validation 에러 |
| 400 | userNo 누락 | 요청 파라미터를 확인해주세요 |
| 400 | 포인트 잔액 부족 | 포인트 잔액이 부족합니다 |
| 404 | 상품이 존재하지 않는 경우 | 포인트 상품을 찾을 수 없습니다 |
| 404 | 포인트 지갑이 없는 경우 | 포인트 지갑을 찾을 수 없습니다 |
| 500 | 판매 불가 상품 (SOLD_OUT/기간 외) | 현재 구매할 수 없는 상품입니다 |

---

## 8. 내 주문 내역 조회

사용자의 포인트 상품 주문 내역을 최신순으로 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/orders` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### 사용 예시

```
GET /api/orders?userNo=1
```

### Request Body

없음

### Response

**Status: `200 OK`**

```json
[
  {
    "orderId": 2,
    "userNo": 1,
    "productId": 3,
    "productName": "치킨 교환권",
    "pointPrice": 15000,
    "roadAddress": "구미시 인동중앙로 200",
    "numberAddress": "인동동 456-7",
    "orderAt": "2026-03-10T16:00:00"
  },
  {
    "orderId": 1,
    "userNo": 1,
    "productId": 1,
    "productName": "아메리카노 쿠폰",
    "pointPrice": 3000,
    "roadAddress": "구미시 인동중앙로 100",
    "numberAddress": "인동동 123-4",
    "orderAt": "2026-03-10T15:30:00"
  }
]
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | userNo 누락 | 요청 파라미터를 확인해주세요 |

---

## PointProductStatus enum

| 값 | 설명 |
|----|------|
| `ON_SALE` | 판매중 |
| `SOLD_OUT` | 품절 |

---

## Response 공통 구조

| 필드 | 타입 | 설명 |
|------|------|------|
| productId | Long | 상품 PK |
| productName | String | 상품명 |
| description | String | 상품 설명 |
| category | String | 상품 카테고리 |
| imageUrl | String | 이미지 URL |
| pointPrice | Long | 포인트 가격 |
| stockQuantity | Integer | 재고 수량 |
| status | String | 상품 상태 (ON_SALE / SOLD_OUT) |
| startsAt | String | 판매 시작일 (ISO 8601) |
| endsAt | String | 판매 종료일 (ISO 8601) |

---

## 9. 주문 단건 조회

포인트 상품 주문을 단건 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/orders/{orderId}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| orderId | Long | O | 주문 ID |

### Request Body

없음

### Response

**Status: `200 OK`**
```json
{
  "orderId": 1,
  "userNo": 1,
  "productId": 1,
  "productName": "아메리카노 쿠폰",
  "pointPrice": 3000,
  "roadAddress": "구미시 인동중앙로 100",
  "numberAddress": "인동동 123-4",
  "orderAt": "2026-03-10T15:30:00"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 404 | 주문이 존재하지 않는 경우 | 주문을 찾을 수 없습니다. id={orderId} |

## Order Response 공통 구조

| 필드 | 타입 | 설명 |
|------|------|------|
| orderId | Long | 주문 PK |
| userNo | Long | 사용자 번호 |
| productId | Long | 상품 ID |
| productName | String | 상품명 |
| pointPrice | Long | 차감 포인트 |
| roadAddress | String | 도로명 주소 (nullable) |
| numberAddress | String | 지번 주소 (nullable) |
| orderAt | String | 주문 시각 (ISO 8601) |