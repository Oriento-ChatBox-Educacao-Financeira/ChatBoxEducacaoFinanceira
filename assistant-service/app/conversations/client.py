import os
from uuid import UUID

import httpx


class ConversationOwnershipClient:
    def __init__(
        self,
        base_url: str | None = None,
        timeout: float = 5.0,
    ) -> None:
        self.base_url = base_url or os.getenv("JAVA_API_URL")
        self.timeout = timeout

        if not self.base_url:
            raise RuntimeError("JAVA_API_URL não configurada")

    async def user_has_access(
        self,
        conversation_id: UUID,
        access_token: str,
    ) -> bool:
        url = (
            f"{self.base_url.rstrip('/')}"
            f"/api/mensagens/conversa/{conversation_id}"
        )

        async with httpx.AsyncClient(
            timeout=self.timeout,
            follow_redirects=False,
        ) as client:
            response = await client.get(
                url,
                headers={
                    "Authorization": f"Bearer {access_token}",
                },
            )

        if response.status_code == 200:
            return True

        if response.status_code in (403, 404):
            return False

        if response.status_code == 401:
            raise PermissionError("Token rejeitado pela API Java")

        response.raise_for_status()

        return False