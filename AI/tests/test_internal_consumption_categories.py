from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
AUTH_HEADER = {"X-Service-Token": "dev-internal-token"}


def test_classify_consumption_categories_requires_service_token():
    response = client.post(
        "/internal/v1/consumption/categories/classify",
        json={"transactions": [{"transactionId": "TX-1", "merchantName": "스타벅스"}]},
    )

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_classify_consumption_categories_success():
    response = client.post(
        "/internal/v1/consumption/categories/classify",
        headers=AUTH_HEADER,
        json={
            "transactions": [
                {
                    "transactionId": "TX-1",
                    "merchantName": "스타벅스 구미역점",
                    "rawCategory": "생활",
                    "memo": "일시불",
                    "amount": 5900,
                },
                {
                    "transactionId": "TX-2",
                    "merchantName": "카카오택시",
                    "rawCategory": "생활",
                    "memo": "승인",
                    "amount": 18300,
                },
            ]
        },
    )

    data = response.json()
    assert response.status_code == 200
    assert data["modelVersion"] == "consumption-rule-v1"
    assert data["results"][0]["transactionId"] == "TX-1"
    assert data["results"][0]["aiCategory"] == "카페"
    assert data["results"][0]["confidence"] >= 0.7
    assert data["results"][0]["fallbackUsed"] is False
    assert data["results"][1]["aiCategory"] == "교통"


def test_classify_consumption_categories_raw_category_fallback():
    response = client.post(
        "/internal/v1/consumption/categories/classify",
        headers=AUTH_HEADER,
        json={
            "transactions": [
                {
                    "transactionId": "TX-3",
                    "merchantName": "알 수 없는 가맹점",
                    "rawCategory": "교육/육아",
                    "memo": "승인",
                    "amount": 120000,
                }
            ]
        },
    )

    data = response.json()
    assert response.status_code == 200
    assert data["results"][0]["aiCategory"] == "교육"
    assert data["results"][0]["fallbackUsed"] is True


def test_classify_consumption_categories_invalid_payload():
    response = client.post(
        "/internal/v1/consumption/categories/classify",
        headers=AUTH_HEADER,
        json={},
    )

    data = response.json()
    assert response.status_code == 400
    assert data["code"] == "INVALID_REQUEST"
