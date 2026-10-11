import asyncio
from uuid import uuid4

import pytest
from fastapi import HTTPException
from fastapi.security import HTTPAuthorizationCredentials

from app.conversations import dependencies


class FakeOwnershipClient:
    pass


def test_returns_authenticated_user_when_access_is_allowed(
    monkeypatch,
):
    expected_user_id = uuid4()
    conversation_id = uuid4()
    client = FakeOwnershipClient()

    credentials = HTTPAuthorizationCredentials(
        scheme="Bearer",
        credentials="valid-token",
    )

    monkeypatch.setattr(
        dependencies,
        "validate_access_token",
        lambda token: expected_user_id,
    )

    calls = {}

    async def fake_verify_conversation_ownership(
        conversation_id,
        access_token,
        client,
    ):
        calls["conversation_id"] = conversation_id
        calls["access_token"] = access_token
        calls["client"] = client

    monkeypatch.setattr(
        dependencies,
        "verify_conversation_ownership",
        fake_verify_conversation_ownership,
    )

    result = asyncio.run(
        dependencies.require_conversation_access(
            conversation_id=conversation_id,
            credentials=credentials,
            client=client,
        )
    )

    assert result == expected_user_id
    assert calls["conversation_id"] == conversation_id
    assert calls["access_token"] == "valid-token"
    assert calls["client"] is client


def test_rejects_request_without_token():
    with pytest.raises(HTTPException) as exc_info:
        asyncio.run(
            dependencies.require_conversation_access(
                conversation_id=uuid4(),
                credentials=None,
                client=FakeOwnershipClient(),
            )
        )

    assert exc_info.value.status_code == 401
    assert (
        exc_info.value.detail
        == "Token de autenticação ausente"
    )

    assert exc_info.value.headers == {
        "WWW-Authenticate": "Bearer"
    }


def test_propagates_invalid_jwt(
    monkeypatch,
):
    credentials = HTTPAuthorizationCredentials(
        scheme="Bearer",
        credentials="invalid-token",
    )

    def reject_token(token):
        raise HTTPException(
            status_code=401,
            detail="Token inválido ou expirado",
        )

    monkeypatch.setattr(
        dependencies,
        "validate_access_token",
        reject_token,
    )

    with pytest.raises(HTTPException) as exc_info:
        asyncio.run(
            dependencies.require_conversation_access(
                conversation_id=uuid4(),
                credentials=credentials,
                client=FakeOwnershipClient(),
            )
        )

    assert exc_info.value.status_code == 401


def test_propagates_conversation_denial(
    monkeypatch,
):
    credentials = HTTPAuthorizationCredentials(
        scheme="Bearer",
        credentials="valid-token",
    )

    monkeypatch.setattr(
        dependencies,
        "validate_access_token",
        lambda token: uuid4(),
    )

    async def deny_access(
        conversation_id,
        access_token,
        client,
    ):
        raise HTTPException(
            status_code=404,
            detail="Conversa não encontrada",
        )

    monkeypatch.setattr(
        dependencies,
        "verify_conversation_ownership",
        deny_access,
    )

    with pytest.raises(HTTPException) as exc_info:
        asyncio.run(
            dependencies.require_conversation_access(
                conversation_id=uuid4(),
                credentials=credentials,
                client=FakeOwnershipClient(),
            )
        )

    assert exc_info.value.status_code == 404
    assert exc_info.value.detail == "Conversa não encontrada"


def test_returns_503_when_java_api_is_not_configured(
    monkeypatch,
):
    monkeypatch.delenv(
        "JAVA_API_URL",
        raising=False,
    )

    with pytest.raises(HTTPException) as exc_info:
        dependencies.get_conversation_ownership_client()

    assert exc_info.value.status_code == 503
    assert (
        exc_info.value.detail
        == "Serviço de autorização não configurado"
    )