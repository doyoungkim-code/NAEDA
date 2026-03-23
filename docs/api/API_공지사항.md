# 공지사항 API

## Base URL
`/api/notices`

---

## 1. 공지사항 등록

- **URL**: `POST /api/notices`
- **Method**: `POST`

### Request Body
```json
{
  "title": "서비스 점검 안내",
  "content": "2026년 3월 25일 새벽 2시~4시 서비스 점검이 예정되어 있습니다."
}
```

### Request 상세
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | O | 공지 제목 (최대 200자) |
| content | String | O | 공지 내용 |

### Response (201 Created)
```json
{
  "noticeId": 1,
  "title": "서비스 점검 안내",
  "content": "2026년 3월 25일 새벽 2시~4시 서비스 점검이 예정되어 있습니다.",
  "fcmNotified": false,
  "created": "2026-03-23T10:00:00",
  "modified": null
}
```

### 에러 코드
| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 제목 또는 내용 누락 | Validation 에러 |

---

## 2. 공지사항 목록 조회

- **URL**: `GET /api/notices`
- **Method**: `GET`

### Response (200 OK)
```json
[
  {
    "noticeId": 2,
    "title": "신규 기능 안내",
    "content": "페이스페이 기능이 추가되었습니다.",
    "fcmNotified": true,
    "created": "2026-03-23T12:00:00",
    "modified": null
  },
  {
    "noticeId": 1,
    "title": "서비스 점검 안내",
    "content": "2026년 3월 25일 새벽 2시~4시 서비스 점검이 예정되어 있습니다.",
    "fcmNotified": false,
    "created": "2026-03-23T10:00:00",
    "modified": null
  }
]
```

- 최신순(created DESC) 정렬

---

## 3. 공지사항 단건 조회

- **URL**: `GET /api/notices/{noticeId}`
- **Method**: `GET`

### Path Parameter
| 이름 | 타입 | 설명 |
|------|------|------|
| noticeId | Long | 공지사항 ID |

### Response (200 OK)
```json
{
  "noticeId": 1,
  "title": "서비스 점검 안내",
  "content": "2026년 3월 25일 새벽 2시~4시 서비스 점검이 예정되어 있습니다.",
  "fcmNotified": false,
  "created": "2026-03-23T10:00:00",
  "modified": null
}
```

### 에러 코드
| Status | 조건 | 메시지 |
|--------|------|--------|
| 404 | 존재하지 않는 공지사항 | 존재하지 않는 공지사항입니다. |

---

## 4. 공지사항 수정

- **URL**: `PUT /api/notices/{noticeId}`
- **Method**: `PUT`

### Path Parameter
| 이름 | 타입 | 설명 |
|------|------|------|
| noticeId | Long | 공지사항 ID |

### Request Body
```json
{
  "title": "수정된 제목",
  "content": "수정된 내용"
}
```

### Request 상세
| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | O | 수정할 제목 |
| content | String | O | 수정할 내용 |

### Response (200 OK)
```json
{
  "noticeId": 1,
  "title": "수정된 제목",
  "content": "수정된 내용",
  "fcmNotified": false,
  "created": "2026-03-23T10:00:00",
  "modified": "2026-03-23T14:00:00"
}
```

### 에러 코드
| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 제목 또는 내용 누락 | Validation 에러 |
| 404 | 존재하지 않는 공지사항 | 존재하지 않는 공지사항입니다. |

---

## 5. 공지사항 삭제

- **URL**: `DELETE /api/notices/{noticeId}`
- **Method**: `DELETE`

### Path Parameter
| 이름 | 타입 | 설명 |
|------|------|------|
| noticeId | Long | 공지사항 ID |

### Response (204 No Content)
본문 없음

### 에러 코드
| Status | 조건 | 메시지 |
|--------|------|--------|
| 404 | 존재하지 않는 공지사항 | 존재하지 않는 공지사항입니다. |

---

## 6. 공지사항 FCM 알림 발송

- **URL**: `POST /api/notices/{noticeId}/notify`
- **Method**: `POST`
- **설명**: 해당 공지사항을 전체 사용자에게 FCM 푸시 알림으로 발송합니다.

### Path Parameter
| 이름 | 타입 | 설명 |
|------|------|------|
| noticeId | Long | 공지사항 ID |

### Response (200 OK)
```json
{
  "noticeId": 1,
  "totalUsers": 100,
  "targetUsers": 80,
  "successCount": 78,
  "failCount": 2
}
```

### Response 상세
| 이름 | 타입 | 설명 |
|------|------|------|
| noticeId | Long | 공지사항 ID |
| totalUsers | int | 전체 사용자 수 |
| targetUsers | int | FCM 토큰 보유 사용자 수 |
| successCount | int | 발송 성공 수 |
| failCount | int | 발송 실패 수 |

### 에러 코드
| Status | 조건 | 메시지 |
|--------|------|--------|
| 400 | 이미 발송된 공지사항 | 이미 FCM 알림이 발송된 공지사항입니다. |
| 404 | 존재하지 않는 공지사항 | 존재하지 않는 공지사항입니다. |

---

## 공통 사항

### 알림 연동
- FCM 발송 시 `notification` 테이블에 알림 이력이 자동 저장됩니다.
- 알림 유형: `SYSTEM`, 참조 유형: `NOTICE`
- 프론트에서 알림 탭 시 `referenceType=NOTICE`, `referenceId=noticeId`로 공지사항 상세 화면 라우팅 가능

### DB 마이그레이션 (최초 1회)
```sql
ALTER TYPE reference_type_enum ADD VALUE 'NOTICE';

CREATE TABLE notice (
    notice_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    content      TEXT         NOT NULL,
    fcm_notified BOOLEAN      NOT NULL DEFAULT FALSE,
    created      TIMESTAMP    NOT NULL DEFAULT NOW(),
    modified     TIMESTAMP
);
```
