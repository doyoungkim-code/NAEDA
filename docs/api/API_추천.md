# 추천 API

> Base URL: `/api/recommend`

---

## 1. 추천 가게 목록 조회

동/카테고리/정렬 필터를 적용하여 구미 지역 가게 추천 목록을 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/recommend/stores` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| dong | String | X | 동 이름 (예: `진평동`). 미입력 시 구미 전체 |
| category | String | X | 카테고리명 (예: `한식`). 미입력 시 전체 |
| sort | String | X | 정렬 기준: `rating`(별점순), `visits`(방문순). 미입력 시 추천 점수순 |

### 추천 점수 계산

기본 정렬(sort 미지정) 시 아래 공식으로 계산된 종합 점수 내림차순 정렬:

```
score = (rating / 5.0) × 0.6 + (방문수 / 최대방문수) × 0.4
```

- 별점이 높으면서 방문수도 많은 가게가 상위에 노출
- 별점이 낮아도 방문수가 많으면 추천 점수가 올라감

### Response

**Status: `200 OK`**

```json
[
  {
    "storeId": 1,
    "storeName": "구미 한식당",
    "categoryName": "한식",
    "roadAddress": "경북 구미시 진평동 123",
    "latitude": 36.1192,
    "longitude": 128.3444,
    "rating": 4.5,
    "imageUrl": "https://example.com/store.jpg",
    "description": "맛있는 한식당",
    "visitCount": 30,
    "score": 0.78
  }
]
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| storeId | Long | 가게 PK |
| storeName | String | 가게명 |
| categoryName | String | 카테고리명 |
| roadAddress | String | 도로명 주소 |
| latitude | Double | 위도 |
| longitude | Double | 경도 |
| rating | Double | 별점 (0.0 ~ 5.0) |
| imageUrl | String | 가게 이미지 URL |
| description | String | 가게 설명 |
| visitCount | Long | 결제(방문) 횟수 |
| score | Double | 추천 점수 (0.0 ~ 1.0) |

### 정렬 옵션 상세

| sort 값 | 정렬 기준 | 설명 |
|----------|-----------|------|
| (미입력) | score DESC | 별점 + 방문수 가중 종합 점수 |
| `rating` | rating DESC | 순수 별점 높은 순 |
| `visits` | visitCount DESC | 방문(결제) 많은 순 |

---

## 2. 동 목록 조회

가게가 존재하는 동 목록을 조회한다. 프론트에서 동 필터 드롭다운에 사용.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/recommend/dongs` |
| **Auth** | - |

### Query Parameters

없음

### Response

**Status: `200 OK`**

```json
[
  "경북 구미시 인의동",
  "경북 구미시 진평동",
  "경북 구미시 형곡동"
]
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| (배열 요소) | String | 가게가 존재하는 동 주소 (정렬됨) |

---

## 처리 흐름

```
지도 화면 진입
  └─ GET /api/recommend/dongs → 동 목록 로드 (드롭다운)
  └─ GET /api/recommend/stores → 전체 추천 목록 (기본 점수순)

동 클릭 / 필터 변경
  └─ GET /api/recommend/stores?dong=진평동&category=한식&sort=rating
       1. StoreRepository.findByFilters(dong, category) → 가게 조회
       2. PayTransactionRepository.countVisitsByStore() → 방문수 집계
       3. 점수 계산 + 정렬 적용
       4. RecommendResponse 목록 반환
```