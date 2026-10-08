from datetime import datetime, timedelta, timezone
from uuid import uuid4

import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
from fastapi import HTTPException

from app.security import jwt as jwt_service


@pytest.fixture
def rsa_keys():
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048,
    )

    public_key = private_key.public_key().public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo,
    ).decode("utf-8")

    return private_key, public_key


@pytest.fixture
def configure_public_key(monkeypatch, rsa_keys):
    _, public_key = rsa_keys

    monkeypatch.setattr(
        jwt_service,
        "get_jwt_public_key",
        lambda: public_key,
    )


def create_token(private_key, **overrides):
    now = datetime.now(timezone.utc)

    payload = {
        "iss": "oriento",
        "sub": str(uuid4()),
        "iat": now,
        "exp": now + timedelta(minutes=15),
    }

    payload.update(overrides)

    return jwt.encode(
        payload,
        private_key,
        algorithm="RS256",
    )


def test_valid_token(rsa_keys, configure_public_key):
    private_key, _ = rsa_keys
    user_id = uuid4()

    token = create_token(
        private_key,
        sub=str(user_id),
    )

    assert jwt_service.validate_access_token(token) == user_id


def test_expired_token(rsa_keys, configure_public_key):
    private_key, _ = rsa_keys

    token = create_token(
        private_key,
        exp=datetime.now(timezone.utc) - timedelta(minutes=1),
    )

    with pytest.raises(HTTPException) as error:
        jwt_service.validate_access_token(token)

    assert error.value.status_code == 401


def test_invalid_signature(rsa_keys, configure_public_key):
    private_key, _ = rsa_keys

    token = create_token(private_key) + "invalid"

    with pytest.raises(HTTPException) as error:
        jwt_service.validate_access_token(token)

    assert error.value.status_code == 401


def test_invalid_issuer(rsa_keys, configure_public_key):
    private_key, _ = rsa_keys

    token = create_token(
        private_key,
        iss="outro-servico",
    )

    with pytest.raises(HTTPException) as error:
        jwt_service.validate_access_token(token)

    assert error.value.status_code == 401


def test_invalid_subject(rsa_keys, configure_public_key):
    private_key, _ = rsa_keys

    token = create_token(
        private_key,
        sub="usuario-invalido",
    )

    with pytest.raises(HTTPException) as error:
        jwt_service.validate_access_token(token)

    assert error.value.status_code == 401