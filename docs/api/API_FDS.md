# FDS (이상거래 탐지) API

> Base URL: `/api/fds`

---

## 1. 사용자별 FDS 로그 조회

특정 사용자의 이상거래 탐지 이력을 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/fds/logs` |
| **Auth** | - |

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| userNo | Long | O | 사용자 번호 (양수) |

### Response (200 OK)

```json
[
  {
    "fdsId": 1,
    "paymentId": 10,
    "userNo": 1,
    "anomalyScore": 45,
    "triggeredRules": ["LATE_NIGHT", "HIGH_FREQUENCY"],
    "actionTaken": "ALERT",
    "userConfirmed": false,
    "detected": "2026-03-11T02:30:00"
  }
]
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| fdsId | Long | FDS 로그 ID |
| paymentId | Long | 결제 ID |
| userNo | Long | 사용자 번호 |
| anomalyScore | int | 이상 점수 (0~100) |
| triggeredRules | String[] | 발동된 규칙 목록 |
| actionTaken | String | 대응 조치 (NONE / ALERT / PAUSE / BLOCK) |
| userConfirmed | Boolean | 사용자 본인 확인 여부 |
| detected | String | 탐지 시각 |

---

## 2. 결제별 FDS 로그 조회

특정 결제 건의 이상거래 탐지 결과를 조회한다.

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/fds/logs/payment/{paymentId}` |
| **Auth** | - |

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| paymentId | Long | O | 결제 ID (양수) |

### Response (200 OK)

```json
{
  "fdsId": 1,
  "paymentId": 10,
  "userNo": 1,
  "anomalyScore": 45,
  "triggeredRules": ["LATE_NIGHT"],
  "actionTaken": "ALERT",
  "userConfirmed": false,
  "detected": "2026-03-11T02:30:00"
}
```

### Error Response (404 Not Found)

```json
{
  "message": "해당 결제의 FDS 로그가 없습니다: 10"
}
```

---

## FDS 룰 및 점수 기준

### 탐지 규칙

| 규칙 | 설명 | 점수 |
|------|------|------|
| LATE_NIGHT | 심야(00:00~06:00) 결제 | 10~33점 (금액에 따라 차등) |
| HIGH_FREQUENCY | 10분 내 3회 이상 연속 결제 | 25~33점 |
| AMOUNT_ANOMALY | 일일 평균의 300% 초과 결제 | 25~33점 |

### 점수별 대응 조치

| 점수 범위 | 조치 | 설명 |
|-----------|------|------|
| 0~29 | NONE | 정상 — 로그만 기록 |
| 30~59 | ALERT | 앱 알림 발송 |
| 60~79 | PAUSE | 결제 일시정지 + 사용자 확인 요청 |
| 80~100 | BLOCK | 결제 차단 |
