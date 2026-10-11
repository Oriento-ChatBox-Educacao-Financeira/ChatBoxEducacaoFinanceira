from uuid import UUID

import httpx
from fastapi import HTTPException, status

from app.conversations.client import ConversationOwnershipClient


async def verify_conversation_ownership(
    conversation_id: UUID,
    access_token: str,
    client: ConversationOwnershipClient,
) -> None:
    try:
        has_access = await client.user_has_access(
            conversation_id=conversation_id,
            access_token=access_token,
        )

    except PermissionError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token inválido ou expirado",
            headers={"WWW-Authenticate": "Bearer"},
        )

    except (
        httpx.RequestError,
        httpx.HTTPStatusError,
    ):
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Serviço de autorização indisponível",
        )

    if not has_access:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Conversa não encontrada",
        )