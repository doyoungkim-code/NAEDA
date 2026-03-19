# 배송 주소 API

> Base URL: `/api/addresses`

---

## 1. 주소 목록 조회

사용자의 전체 배송 주소 목록을 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/addresses/{userNo}` |
| **Auth** | - |

### Path Parameters

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
    "addressId": 1,
    "addressName": "집",
    "recipient": "홍길동",
    "phone": "01012345678",
    "roadAddress": "서울시 강남구 테헤란로 1",
    "numberAddress": "역삼동 123-4",
    "detailAddress": "101호",
    "zipCode": "06234",
    "isDefault": true
  },
  {
    "addressId": 2,
    "addressName": "회사",
    "recipient": "홍길동",
    "phone": "01012345678",
    "roadAddress": "서울시 서초구 서초대로 2",
    "numberAddress": "서초동 456-7",
    "detailAddress": "5층",
    "zipCode": "06500",
    "isDefault": false
  }
]
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| addressId | Long | 주소 PK |
| addressName | String | 주소 별칭 (집, 회사 등) |
| recipient | String | 수령인 이름 |
| phone | String | 수령인 연락처 |
| roadAddress | String | 도로명 주소 |
| numberAddress | String | 지번 주소 |
| detailAddress | String | 상세주소 (동/호) |
| zipCode | String | 우편번호 |
| isDefault | Boolean | 기본 배송지 여부 |

---

## 2. 주소 단건 조회

배송 주소를 단건 조회한다. 본인 주소만 조회 가능하다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/addresses/{userNo}/{addressId}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| addressId | Long | O | 주소 ID |

### Request Body

없음

### Response

**Status: `200 OK`**

```json
{
  "addressId": 1,
  "addressName": "집",
  "recipient": "홍길동",
  "phone": "01012345678",
  "roadAddress": "서울시 강남구 테헤란로 1",
  "numberAddress": "역삼동 123-4",
  "detailAddress": "101호",
  "zipCode": "06234",
  "isDefault": true
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 본인 주소가 아닌 경우 | 본인의 주소만 접근할 수 있습니다. |
| 404 | 주소가 존재하지 않는 경우 | 주소를 찾을 수 없습니다. addressId: {addressId} |

---

## 3. 주소 생성

새 배송 주소를 등록한다. `isDefault`가 true이면 기존 기본 배송지를 자동 해제한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/addresses/{userNo}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |

### Request Body

```json
{
  "addressName": "집",
  "recipient": "홍길동",
  "phone": "01012345678",
  "roadAddress": "서울시 강남구 테헤란로 1",
  "numberAddress": "역삼동 123-4",
  "detailAddress": "101호",
  "zipCode": "06234",
  "isDefault": true
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| addressName | String | X | 최대 50자 | 주소 별칭 |
| recipient | String | O | @NotBlank | 수령인 이름 |
| phone | String | O | @NotBlank | 수령인 연락처 |
| roadAddress | String | O | @NotBlank | 도로명 주소 |
| numberAddress | String | X | - | 지번 주소 |
| detailAddress | String | X | - | 상세주소 |
| zipCode | String | X | 최대 10자 | 우편번호 |
| isDefault | Boolean | O | @NotNull | 기본 배송지 여부 |

### Response

**Status: `201 Created`**

```json
{
  "addressId": 3,
  "addressName": "집",
  "recipient": "홍길동",
  "phone": "01012345678",
  "roadAddress": "서울시 강남구 테헤란로 1",
  "numberAddress": "역삼동 123-4",
  "detailAddress": "101호",
  "zipCode": "06234",
  "isDefault": true
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 필드 누락 | Validation 에러 |

---

## 4. 주소 수정

배송 주소를 수정한다. 본인 주소만 수정 가능하다. `isDefault`를 true로 변경하면 기존 기본 배송지를 자동 해제한다.

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/addresses/{userNo}/{addressId}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| addressId | Long | O | 주소 ID |

### Request Body

```json
{
  "addressName": "새 집",
  "recipient": "홍길동",
  "phone": "01098765432",
  "roadAddress": "서울시 마포구 월드컵로 10",
  "numberAddress": "상암동 100-1",
  "detailAddress": "301호",
  "zipCode": "03900",
  "isDefault": false
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| addressName | String | X | 최대 50자 | 주소 별칭 |
| recipient | String | O | @NotBlank | 수령인 이름 |
| phone | String | O | @NotBlank | 수령인 연락처 |
| roadAddress | String | O | @NotBlank | 도로명 주소 |
| numberAddress | String | X | - | 지번 주소 |
| detailAddress | String | X | - | 상세주소 |
| zipCode | String | X | 최대 10자 | 우편번호 |
| isDefault | Boolean | O | @NotNull | 기본 배송지 여부 |

### Response

**Status: `200 OK`**

(주소 단건 조회와 동일한 형식)

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 본인 주소가 아닌 경우 | 본인의 주소만 접근할 수 있습니다. |
| 400 | 필수 필드 누락 | Validation 에러 |
| 404 | 주소가 존재하지 않는 경우 | 주소를 찾을 수 없습니다. addressId: {addressId} |

---

## 5. 주소 삭제

배송 주소를 삭제한다. 본인 주소만 삭제 가능하다.

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/addresses/{userNo}/{addressId}` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| addressId | Long | O | 주소 ID |

### Request Body

없음

### Response

**Status: `204 No Content`**

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 본인 주소가 아닌 경우 | 본인의 주소만 접근할 수 있습니다. |
| 404 | 주소가 존재하지 않는 경우 | 주소를 찾을 수 없습니다. addressId: {addressId} |

---

## 6. 기본 배송지 설정

해당 주소를 기본 배송지로 설정한다. 기존 기본 배송지는 자동 해제된다.

| 항목 | 내용 |
|------|------|
| **Method** | `PATCH` |
| **URL** | `/api/addresses/{userNo}/{addressId}/default` |
| **Auth** | - |

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| addressId | Long | O | 주소 ID |

### Request Body

없음

### Response

**Status: `200 OK`**

```json
{
  "addressId": 2,
  "addressName": "회사",
  "recipient": "홍길동",
  "phone": "01012345678",
  "roadAddress": "서울시 서초구 서초대로 2",
  "numberAddress": "서초동 456-7",
  "detailAddress": "5층",
  "zipCode": "06500",
  "isDefault": true
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 본인 주소가 아닌 경우 | 본인의 주소만 접근할 수 있습니다. |
| 404 | 주소가 존재하지 않는 경우 | 주소를 찾을 수 없습니다. addressId: {addressId} |

---

## 공통 사항

### 기본 배송지 규칙

- 기본 배송지는 사용자당 **최대 1개**만 유지된다.
- 새 주소 생성/수정 시 `isDefault: true`를 전달하면 기존 기본 배송지가 자동으로 `false`로 변경된다.
- 기본 배송지 설정(PATCH) 시에도 동일하게 기존 기본 배송지가 해제된다.

### 소유권 검증

- 모든 단건 조회/수정/삭제 API는 해당 주소의 `userNo`가 요청자와 일치하는지 검증한다.
- 불일치 시 `400 Bad Request`를 반환한다.