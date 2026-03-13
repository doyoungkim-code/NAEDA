# AI-011 Category Classification Implementation

## Overview

This document summarizes the implemented AI-011 scope across the AI server and backend.

- Goal: classify each card transaction into a normalized service category.
- Storage rule:
  - `transaction_log.category`: keep the original SSAFY category.
  - `transaction_log.ai_category`: store the AI-normalized category.
- Frontend usage: later clients can read `aiCategory` from backend APIs without calling the AI server directly.

## AI Implementation

### Added internal API

- Endpoint: `POST /internal/v1/consumption/categories/classify`
- Auth: `X-Service-Token`
- Request shape:

```json
{
  "transactions": [
    {
      "transactionId": "TX-1",
      "merchantName": "스타벅스 구미역점",
      "rawCategory": "생활",
      "memo": "승인",
      "amount": 5900,
      "transactedAt": "2026-03-13T09:30:00"
    }
  ]
}
```

- Response shape:

```json
{
  "results": [
    {
      "transactionId": "TX-1",
      "aiCategory": "카페",
      "confidence": 0.73,
      "fallbackUsed": false
    }
  ],
  "modelVersion": "consumption-rule-v1"
}
```

### Classifier strategy

The current implementation is a v1 rule-based classifier.

- Raw category mapping:
  - examples: `교육/육아 -> 교육`, `대형마트 -> 마트`, `통신 -> 주거/통신`
- Merchant keyword override:
  - examples: `스타벅스 -> 카페`, `카카오택시 -> 교통`, `이마트 -> 마트`
- Fallback:
  - if no strong merchant keyword exists, the classifier falls back to raw-category normalization or `기타`

This is intentionally lightweight so it can run immediately without a separate labeled training dataset.

## Backend Implementation

### Database / entity

Added `ai_category` to `transaction_log`.

```sql
ai_category VARCHAR(30)
```

Meaning:

- `category`: original SSAFY category
- `ai_category`: AI-normalized category

### Backend flow

1. Card transaction list is fetched from SSAFY.
2. Backend parses all transactions.
3. Transactions missing `aiCategory` are sent to the AI server in batch.
4. Backend stores:
   - raw category in `category`
   - normalized category in `aiCategory`
5. Existing cached transactions are reused.
6. If AI fails, backend falls back to a local category mapper so card transaction 조회 does not fail.

### Exposed response fields

`aiCategory` is now included in:

- card transaction response
- transaction log response

This keeps frontend integration simple later.

## Files Changed

### AI

- `AI/app/api/internal_consumption_categories.py`
- `AI/app/core/consumption_category_classifier.py`
- `AI/app/schemas/consumption_category.py`
- `AI/app/main.py`
- `AI/app/core/config.py`

### Backend

- `naeda/src/main/java/com/ssafy/naeda/domain/transaction/entity/TransactionLog.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/card/service/CardService.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/card/dto/response/CardTransactionResponse.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/transaction/dto/response/TransactionLogResponse.java`
- `naeda/src/main/java/com/ssafy/naeda/domain/consumption/client/...`

### Schema docs

- `naeda/db/schema.sql`
- `docs/erd/erd.sql`

## Tests Run

### AI

```bash
cd AI
$env:PYTHONPATH='C:\SSAFY\S14P21D103\AI'
.\.venv\Scripts\python.exe -m pytest tests\test_internal_embeddings.py tests\test_internal_consumption_categories.py
```

### Backend

```bash
cd naeda
$env:GRADLE_USER_HOME='C:\SSAFY\S14P21D103\naeda\.gradle-user'
.\gradlew.bat test --tests "com.ssafy.naeda.domain.card.service.CardServiceTest" --tests "com.ssafy.naeda.domain.card.controller.CardControllerTest" --tests "com.ssafy.naeda.domain.transaction.controller.TransactionLogControllerTest" --tests "com.ssafy.naeda.domain.transaction.service.TransactionLogServiceTest"
```

## Notes

- The AI classifier is currently rule-based, not a trained ML model.
- This is suitable for the current sprint because the system already receives a raw category from SSAFY and mainly needs normalization/refinement.
- If labeled data is accumulated later, the classifier can be upgraded to TF-IDF + Logistic Regression or another supervised model without changing the backend contract.
