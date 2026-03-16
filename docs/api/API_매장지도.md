# 매장 지도 API

> Base URL: `/api/stores`

---

## 1. 지도용 매장 목록 조회

구미시 공공 CSV 데이터를 기반으로 적재된 지도용 매장 목록을 조회한다.

- 공공 매장만 반환한다.
- 활성 상태(`isActive=true`)인 매장만 반환한다.
- 지도 마커 표시가 가능한 좌표(`latitude`, `longitude`)가 있는 매장만 반환한다.
- 이미지, 설명, 평점은 네이버 보강 결과가 있으면 함께 반환한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/stores/map` |
| **Auth** | - |

### Query Parameters

없음

### Request Body

없음

### Response

**Status: `200 OK`**

```json
[
  {
    "storeId": -101,
    "userNo": null,
    "storeName": "백운한정식",
    "categoryId": "PUBLIC_RESTAURANT",
    "categoryName": "한식",
    "roadAddress": "경상북도 구미시 인동35길 38",
    "numberAddress": "경상북도 구미시 구평동 551-15",
    "latitude": 36.123456,
    "longitude": 128.123456,
    "phone": "0544758889",
    "isLocalBusiness": true,
    "facePayEnabled": false,
    "rating": 4.3,
    "imageUrl": "https://example.com/store.jpg",
    "description": "한식당",
    "sourceType": "PUBLIC_CSV",
    "isActive": true
  }
]
```

### Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| storeId | Long | 매장 PK |
| userNo | Long | 사장님 사용자 번호. 공공 CSV 매장은 null 가능 |
| storeName | String | 매장명 |
| categoryId | String | 내부 카테고리 ID (`PUBLIC_RESTAURANT`, `PUBLIC_BAKERY` 등) |
| categoryName | String | 업종명 |
| roadAddress | String | 도로명 주소 |
| numberAddress | String | 지번 주소 |
| latitude | Double | 위도 |
| longitude | Double | 경도 |
| phone | String | 전화번호 |
| isLocalBusiness | Boolean | 구미 지역 소상공인 여부 |
| facePayEnabled | Boolean | 페이스페이 사용 가능 여부 |
| rating | Double | 평점. 값이 없으면 `0.0`일 수 있음 |
| imageUrl | String | 대표 이미지 URL. 없으면 null |
| description | String | 매장 설명. 없으면 null |
| sourceType | String | 매장 출처 (`PUBLIC_CSV`, `SSAFY`) |
| isActive | Boolean | 활성 여부 |

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 500 | 서버 내부 오류 | 서버 내부 오류 |

### 프론트 사용 방식

- 지도 마커 위치
  - `latitude`
  - `longitude`
- 마커/카드 제목
  - `storeName`
- 보조 정보
  - `categoryName`
  - `roadAddress`
  - `phone`
- 매장 카드 정보
  - `imageUrl`
  - `description`
  - `rating`

### null / 기본값 처리 권장

- `imageUrl == null`
  - 기본 플레이스홀더 이미지 사용
- `description == null`
  - `"설명 없음"` 또는 `categoryName`으로 대체
- `rating == 0.0`
  - 평점 미노출 또는 `"평점 없음"` 처리 권장

### Android Retrofit 예시

```kotlin
data class MapStoreResponseDto(
    val storeId: Long,
    val userNo: Long? = null,
    val storeName: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val roadAddress: String? = null,
    val numberAddress: String? = null,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val isLocalBusiness: Boolean = false,
    val facePayEnabled: Boolean = false,
    val rating: Double = 0.0,
    val imageUrl: String? = null,
    val description: String? = null,
    val sourceType: String? = null,
    val isActive: Boolean = true
)

private interface StoreMapApiService {
    @GET("api/stores/map")
    suspend fun getMapStores(): List<MapStoreResponseDto>
}
```

### 주의 사항

- `imageUrl`, `description`, `rating`은 네이버 보강 결과이므로 서버 환경에 네이버 검색 API 키가 없으면 비어 있을 수 있다.
- 이 경우에도 지도 마커 표시에는 문제가 없다.

---
