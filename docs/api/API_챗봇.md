# 챗봇 API (AI-019)

> Base URL: AI 서버 (`http://j14d103.p.ssafy.io:8000`)

---

## 1. 챗봇 대화

구미 지역 맛집/카페/축제를 자연어 대화로 추천한다.

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/chat` |
| **Auth** | - |

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| user_no | Long | O | 사용자 번호 |
| message | String | O | 사용자 메시지 (최소 1자) |
| history | Array | X | 이전 대화 이력 (기본: 빈 배열) |

#### history 배열 요소

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| role | String | O | `user` 또는 `assistant` |
| content | String | O | 메시지 내용 |

### Request 예시

```json
{
  "user_no": 1,
  "message": "구미 봉곡동 한식 맛집 추천해줘",
  "history": [
    {"role": "user", "content": "안녕"},
    {"role": "assistant", "content": "안녕하세요! 내다봇이에요. 구미 맛집이나 축제 추천해드릴까요?"}
  ]
}
```

### Response Body

| 이름 | 타입 | 설명 |
|------|------|------|
| reply | String | 챗봇 응답 메시지 |
| referenced_stores | Array\<Int\> | 응답에서 참조한 매장 store_id 목록 |
| referenced_festivals | Array\<Int\> | 응답에서 참조한 축제 festival_id 목록 |

### Response 예시

```json
{
  "reply": "봉곡동 한식 맛집 추천드려요!\n\n1. 순두부전문점가원 - 주소: 경상북도 구미시 인동남길 48\n2. 산더미미성돼지국밥 - 주소: 경상북도 구미시 송원서로 75",
  "referenced_stores": [3622, 3972],
  "referenced_festivals": []
}
```

### 의도 파악 키워드

| 의도 | 키워드 예시 |
|------|------------|
| 한식 | 한식, 밥, 국밥, 찌개, 비빔밥, 백반 |
| 양식 | 양식, 파스타, 피자, 스테이크, 햄버거 |
| 중식 | 중식, 짜장, 짬뽕, 탕수육 |
| 일식 | 일식, 초밥, 라멘, 돈까스, 우동 |
| 카페 | 카페, 커피, 디저트, 빵, 케이크 |
| 일반 매장 | 맛집, 식당, 밥집, 추천, 점심, 저녁 |
| 축제 | 축제, 이벤트, 행사, 공연, 전시 |
| 지역 | 진평동, 인동, 봉곡동, 도량동, 원평동 등 |

### 추천 우선순위

1. 페이스페이 가능 매장 우선
2. 구미 소상공인 매장 가산점
3. 평점 높은 순 + 랜덤 섞기

### 에러 시 응답

API 호출 실패 시에도 200 OK로 응답하되, 폴백 메시지를 반환한다.

```json
{
  "reply": "죄송합니다, 잠시 후 다시 시도해주세요.",
  "referenced_stores": [],
  "referenced_festivals": []
}
```

---

## 환경 설정

| 환경변수 | 설명 | 기본값 |
|----------|------|--------|
| OPENAI_API_KEY | GMS API 키 | - |
| OPENAI_MODEL | GPT 모델명 | gpt-4o-mini |
| DATABASE_URL | PostgreSQL 접속 URL | postgresql+asyncpg://... |
| CHAT_MAX_HISTORY_TURNS | 대화 이력 최대 턴 수 | 10 |
| CHAT_MAX_STORE_RESULTS | 매장 검색 최대 결과 수 | 10 |
| CHAT_MAX_FESTIVAL_RESULTS | 축제 검색 최대 결과 수 | 10 |
