# 소비 리포트 API

> 클라이언트용: `/api/reports` | 내부(AI 서버)용: `/api/internal/reports`

---

## 1. 최신 소비 리포트 조회

사용자의 가장 최근 소비 리포트를 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/reports/latest` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| periodType | String | O | 리포트 기간 유형 (`WEEKLY`, `MONTHLY`) |

### Request Body

없음

### Response

**Status: `200 OK`**

```json
{
  "reportId": 1,
  "periodType": "MONTHLY",
  "periodStart": "2026-02-01",
  "periodEnd": "2026-02-28",
  "categoryBreakdown": {
    "식비": 300000,
    "카페": 80000
  },
  "totalSpending": 380000,
  "localSpending": 200000,
  "localRatio": 0.65,
  "localGrade": "B",
  "insights": ["카페 지출이 전월 대비 15% 증가했습니다"],
  "generated": "2026-03-04T09:30:00"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 파라미터 누락 | Required parameter 누락 에러 |
| 404 | 리포트가 존재하지 않는 경우 | 리포트가 존재하지 않습니다. |

---

## 2. 소비 리포트 히스토리 조회

사용자의 소비 리포트 목록을 최신순(periodStart 내림차순)으로 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/reports` |
| **Auth** | - |

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userNo | Long | O | 사용자 번호 |
| periodType | String | O | 리포트 기간 유형 (`WEEKLY`, `MONTHLY`) |

### Request Body

없음

### Response

**Status: `200 OK`**

```json
[
  {
    "reportId": 2,
    "periodType": "MONTHLY",
    "periodStart": "2026-02-01",
    "periodEnd": "2026-02-28",
    "categoryBreakdown": { "식비": 300000, "카페": 80000 },
    "totalSpending": 380000,
    "localSpending": 200000,
    "localRatio": 0.65,
    "localGrade": "B",
    "insights": ["카페 지출이 전월 대비 15% 증가했습니다"],
    "generated": "2026-03-04T09:30:00"
  },
  {
    "reportId": 1,
    "periodType": "MONTHLY",
    "periodStart": "2026-01-01",
    "periodEnd": "2026-01-31",
    "categoryBreakdown": { "식비": 250000 },
    "totalSpending": 400000,
    "localSpending": 150000,
    "localRatio": 0.38,
    "localGrade": "C",
    "insights": [],
    "generated": "2026-02-03T10:00:00"
  }
]
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 파라미터 누락 | Required parameter 누락 에러 |

---

## 3. 소비 리포트 저장 (내부 API)

AI 서버에서 생성한 소비 리포트를 저장한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/internal/reports` |
| **Auth** | 내부 서버 전용 |

### Query Parameters

없음

### Request Body

```json
{
  "userNo": 1,
  "periodType": "WEEKLY",
  "periodStart": "2026-02-24",
  "periodEnd": "2026-03-02",
  "categoryBreakdown": {
    "식비": 320000,
    "카페": 85000
  },
  "totalSpending": 405000,
  "localSpending": 200000,
  "localRatio": 0.49,
  "localGrade": "B",
  "insights": ["카페 지출이 전주 대비 20% 증가했어요"]
}
```

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| userNo | Long | O | NotNull | 사용자 번호 |
| periodType | PeriodType | O | NotNull | 기간 유형 (`WEEKLY`, `MONTHLY`) |
| periodStart | LocalDate | O | NotNull | 기간 시작일 |
| periodEnd | LocalDate | O | NotNull | 기간 종료일 |
| categoryBreakdown | Map\<String, Long\> | X | - | 카테고리별 지출 내역 |
| totalSpending | Long | X | - | 총 지출 금액 |
| localSpending | Long | X | - | 지역 소비 금액 |
| localRatio | Float | X | - | 지역 소비 비율 (0.0~1.0) |
| localGrade | LocalGrade | X | - | 지역 소비 등급 (`A`, `B`, `C`, `D`) |
| insights | List\<String\> | X | - | AI 인사이트 목록 |

### Response

**Status: `201 Created`**

```json
{
  "reportId": 1,
  "periodType": "WEEKLY",
  "periodStart": "2026-02-24",
  "periodEnd": "2026-03-02",
  "categoryBreakdown": {
    "식비": 320000,
    "카페": 85000
  },
  "totalSpending": 405000,
  "localSpending": 200000,
  "localRatio": 0.49,
  "localGrade": "B",
  "insights": ["카페 지출이 전주 대비 20% 증가했어요"],
  "generated": "2026-03-04T09:30:00"
}
```

### Error

| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 필수 값 누락 (userNo, periodType, periodStart, periodEnd) | Validation 에러 |

---

## Enum 정의

### PeriodType

| 값 | 설명 |
|----|------|
| `WEEKLY` | 주간 리포트 |
| `MONTHLY` | 월간 리포트 |

### LocalGrade

| 값 | 설명 |
|----|------|
| `A` | 최우수 |
| `B` | 우수 |
| `C` | 보통 |
| `D` | 미흡 |

---

## 공통 사항

### Response 공통 구조

별도의 공통 응답 래퍼 없이 DTO를 직접 반환한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| reportId | Long | 리포트 PK |
| periodType | String | 기간 유형 |
| periodStart | String | 기간 시작일 (yyyy-MM-dd) |
| periodEnd | String | 기간 종료일 (yyyy-MM-dd) |
| categoryBreakdown | Object | 카테고리별 지출 내역 |
| totalSpending | Long | 총 지출 금액 |
| localSpending | Long | 지역 소비 금액 |
| localRatio | Float | 지역 소비 비율 |
| localGrade | String | 지역 소비 등급 |
| insights | Array | AI 인사이트 목록 |
| generated | String | 리포트 생성 시각 (ISO 8601) |
