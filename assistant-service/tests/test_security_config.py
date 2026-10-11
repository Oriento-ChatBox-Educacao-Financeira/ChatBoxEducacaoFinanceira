import pytest

from app.security.config import (
    get_jwt_algorithm,
    get_jwt_issuer,
)


def test_uses_default_jwt_issuer(monkeypatch):
    monkeypatch.delenv(
        "JWT_ISSUER",
        raising=False,
    )

    assert get_jwt_issuer() == "oriento"


def test_uses_configured_jwt_issuer(monkeypatch):
    monkeypatch.setenv(
        "JWT_ISSUER",
        "custom-issuer",
    )

    assert get_jwt_issuer() == "custom-issuer"


def test_uses_default_jwt_algorithm(monkeypatch):
    monkeypatch.delenv(
        "JWT_ALGORITHM",
        raising=False,
    )

    assert get_jwt_algorithm() == "RS256"


@pytest.mark.parametrize(
    "algorithm",
    [
        "RS256",
        "RS384",
        "RS512",
    ],
)
def test_accepts_allowed_rsa_algorithms(
    monkeypatch,
    algorithm,
):
    monkeypatch.setenv(
        "JWT_ALGORITHM",
        algorithm,
    )

    assert get_jwt_algorithm() == algorithm


@pytest.mark.parametrize(
    "algorithm",
    [
        "HS256",
        "none",
        "ES256",
    ],
)
def test_rejects_disallowed_algorithms(
    monkeypatch,
    algorithm,
):
    monkeypatch.setenv(
        "JWT_ALGORITHM",
        algorithm,
    )

    with pytest.raises(
        RuntimeError,
        match="Algoritmo JWT não permitido",
    ):
        get_jwt_algorithm()