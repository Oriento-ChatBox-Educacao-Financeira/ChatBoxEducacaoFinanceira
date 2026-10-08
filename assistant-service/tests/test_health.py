from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_check_returns_200():
    response = client.get("/health")

    assert response.status_code == 200


def test_health_check_returns_expected_body():
    response = client.get("/health")

    assert response.json() == {
        "status": "ok",
        "service": "oriento-assistant",
    }