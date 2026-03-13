from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
AUTH_HEADER = {"X-Service-Token": "dev-internal-token"}


def test_generate_monthly_insights_requires_service_token():
    response = client.post(
        "/internal/v1/consumption/reports/monthly/insights",
        json={
            "periodStart": "2026-03-01",
            "periodEnd": "2026-03-31",
            "totalSpending": 100000,
            "categoryBreakdown": {"카페": 40000},
        },
    )

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_generate_monthly_insights_top_category_message():
    response = client.post(
        "/internal/v1/consumption/reports/monthly/insights",
        headers=AUTH_HEADER,
        json={
            "periodStart": "2026-03-01",
            "periodEnd": "2026-03-31",
            "totalSpending": 120000,
            "categoryBreakdown": {"카페": 50000, "식비": 30000},
        },
    )

    data = response.json()
    assert response.status_code == 200
    assert data["modelVersion"] == "consumption-monthly-insight-rule-v1"
    assert len(data["insights"]) == 1
    assert "카페" in data["insights"][0]


def test_generate_monthly_insights_previous_month_increase_message():
    response = client.post(
        "/internal/v1/consumption/reports/monthly/insights",
        headers=AUTH_HEADER,
        json={
            "periodStart": "2026-03-01",
            "periodEnd": "2026-03-31",
            "totalSpending": 200000,
            "categoryBreakdown": {"카페": 80000, "식비": 50000},
            "previousTotalSpending": 150000,
            "previousCategoryBreakdown": {"카페": 40000, "식비": 60000},
        },
    )

    data = response.json()
    assert response.status_code == 200
    assert "지난달보다" in data["insights"][0]


def test_generate_monthly_insights_no_spending_message():
    response = client.post(
        "/internal/v1/consumption/reports/monthly/insights",
        headers=AUTH_HEADER,
        json={
            "periodStart": "2026-03-01",
            "periodEnd": "2026-03-31",
            "totalSpending": 0,
            "categoryBreakdown": {},
        },
    )

    data = response.json()
    assert response.status_code == 200
    assert "거의 없었어요" in data["insights"][0]
