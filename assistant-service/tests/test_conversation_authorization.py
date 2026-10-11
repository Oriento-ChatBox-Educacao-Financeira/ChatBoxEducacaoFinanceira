import asyncio
from uuid import uuid4

import httpx
import pytest
from fastapi import HTTPException

from app.conversations.authorization import verify_conversation_ownership


class FakeOwnershipClient:
    def __init__(
        self,
        result: bool = True,
        exception: Exception | None = None,
    ) -> None:
        self.result = result
        self.exception = exception

    async def user_has_access(
        self,
        conversation_id,
        access_token,
    ) -> bool:
        if self.exception:
            raise self.exception

        return self.result


def test_allows_conversation_owned_by_user():
    client = FakeOwnershipClient(result=True)

    asyncio.run(
        verify_conversation_ownership(
            conversation_id=uuid4(),
            access_token="test-token",
            client=client,
        )
    )


def test_hides_conversation_not_owned_by_user():
    client = FakeOwnershipClient(result=False)

    with pytest.raises(HTTPException) as exc_info:
        asyncio.run(
            verify_conversation_ownership(
                conversation_id=uuid4(),
                access_token="test-token",
                client=client,
            )
        )

    assert exc_info.value.status_code == 404
    assert exc_info.value.detail == "Conversa não encontrada"


def test_returns_401_when_java_rejects_token():
    client = FakeOwnershipClient(
        exception=PermissionError(
            "Token rejeitado pela API Java"
        )
    )

    with pytest.raises(HTTPException) as exc_info:
        asyncio.run(
            verify_conversation_ownership(
                conversation_id=uuid4(),
                access_token="invalid-token",
                client=client,
            )
        )

    assert exc_info.value.status_code == 401
    assert exc_info.value.headers == {
        "WWW-Authenticate": "Bearer"
    }


def test_returns_503_when_java_api_is_unavailable():
    request = httpx.Request(
        "GET",
        "http://java-api:8080",
    )

    client = FakeOwnershipClient(
        exception=httpx.RequestError(
            "Connection failed",
            request=request,
        )
    )

    with pytest.raises(HTTPException) as exc_info:
        asyncio.run(
            verify_conversation_ownership(
                conversation_id=uuid4(),
                access_token="test-token",
                client=client,
            )
        )

    assert exc_info.value.status_code == 503
    assert (
        exc_info.value.detail
        == "Serviço de autorização indisponível"
    )