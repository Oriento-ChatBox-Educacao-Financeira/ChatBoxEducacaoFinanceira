from uuid import UUID

from fastapi import Depends, HTTPException, status
from fastapi.security import (
    HTTPAuthorizationCredentials,
    HTTPBearer,
)

from app.conversations.authorization import (
    verify_conversation_ownership,
)
from app.conversations.client import (
    ConversationOwnershipClient,
)
from app.security.jwt import validate_access_token


bearer_scheme = HTTPBearer(auto_error=False)


def get_conversation_ownership_client() -> ConversationOwnershipClient:
    try:
        return ConversationOwnershipClient()

    except RuntimeError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Serviço de autorização não configurado",
        ) from exc


async def require_conversation_access(
    conversation_id: UUID,
    credentials: HTTPAuthorizationCredentials | None = Depends(
        bearer_scheme
    ),
    client: ConversationOwnershipClient = Depends(
        get_conversation_ownership_client
    ),
) -> UUID:
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token de autenticação ausente",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )

    access_token = credentials.credentials

    user_id = validate_access_token(
        access_token
    )

    await verify_conversation_ownership(
        conversation_id=conversation_id,
        access_token=access_token,
        client=client,
    )

    return user_id