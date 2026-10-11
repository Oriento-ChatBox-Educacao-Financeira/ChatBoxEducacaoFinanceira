import asyncio
from uuid import uuid4

import httpx
import pytest

from app.conversations import client as client_module
from app.conversations.client import ConversationOwnershipClient


class FakeAsyncClient:
    response_status = 200
    last_url = None
    last_headers = None

    def __init__(self, **kwargs):
        self.kwargs = kwargs

    async def __aenter__(self):
        return self

    async def __aexit__(
        self,
        exc_type,
        exc_value,
        traceback,
    ):
        return None

    async def get(
        self,
        url,
        headers=None,
    ):
        FakeAsyncClient.last_url = url
        FakeAsyncClient.last_headers = headers

        request = httpx.Request("GET", url)

        return httpx.Response(
            status_code=self.response_status,
            request=request,
        )


def configure_fake_client(
    monkeypatch,
    status_code,
):
    FakeAsyncClient.response_status = status_code
    FakeAsyncClient.last_url = None
    FakeAsyncClient.last_headers = None

    monkeypatch.setattr(
        client_module.httpx,
        "AsyncClient",
        FakeAsyncClient,
    )


def test_returns_true_when_java_allows_access(
    monkeypatch,
):
    configure_fake_client(
        monkeypatch,
        200,
    )

    conversation_id = uuid4()

    client = ConversationOwnershipClient(
        base_url="http://java-api:8080"
    )

    result = asyncio.run(
        client.user_has_access(
            conversation_id=conversation_id,
            access_token="test-token",
        )
    )

    assert result is True

    assert FakeAsyncClient.last_url == (
        "http://java-api:8080"
        f"/api/mensagens/conversa/{conversation_id}"
    )

    assert FakeAsyncClient.last_headers == {
        "Authorization": "Bearer test-token"
    }


@pytest.mark.parametrize(
    "status_code",
    [
        403,
        404,
    ],
)
def test_returns_false_when_access_is_denied(
    monkeypatch,
    status_code,
):
    configure_fake_client(
        monkeypatch,
        status_code,
    )

    client = ConversationOwnershipClient(
        base_url="http://java-api:8080"
    )

    result = asyncio.run(
        client.user_has_access(
            conversation_id=uuid4(),
            access_token="test-token",
        )
    )

    assert result is False


def test_raises_permission_error_for_401(
    monkeypatch,
):
    configure_fake_client(
        monkeypatch,
        401,
    )

    client = ConversationOwnershipClient(
        base_url="http://java-api:8080"
    )

    with pytest.raises(PermissionError):
        asyncio.run(
            client.user_has_access(
                conversation_id=uuid4(),
                access_token="invalid-token",
            )
        )


def test_raises_http_error_for_java_failure(
    monkeypatch,
):
    configure_fake_client(
        monkeypatch,
        500,
    )

    client = ConversationOwnershipClient(
        base_url="http://java-api:8080"
    )

    with pytest.raises(httpx.HTTPStatusError):
        asyncio.run(
            client.user_has_access(
                conversation_id=uuid4(),
                access_token="test-token",
            )
        )


def test_requires_java_api_url(
    monkeypatch,
):
    monkeypatch.delenv(
        "JAVA_API_URL",
        raising=False,
    )

    with pytest.raises(
        RuntimeError,
        match="JAVA_API_URL não configurada",
    ):
        ConversationOwnershipClient()