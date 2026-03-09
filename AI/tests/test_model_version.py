from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
AUTH_HEADER = {"X-Service-Token": "dev-internal-token"}


def test_model_version_requires_service_token():
    response = client.get("/internal/v1/model/version")

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_model_version_returns_current_registry():
    response = client.get("/internal/v1/model/version", headers=AUTH_HEADER)

    data = response.json()
    assert response.status_code == 200
    assert data["featureVersion"] == "fds-feature-v1"
    assert data["ruleVersion"] == "fds-rule-v1"
    assert data["modelVersion"] == "rule-only-v1"
    assert data["algorithm"] == "RULE_ENGINE"
    assert data["artifactPath"] == "N/A"
    assert data["updatedAt"] == "2026-03-09T00:00:00"
