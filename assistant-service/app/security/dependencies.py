from uuid import UUID

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.security.jwt import validate_access_token


bearer_scheme = HTTPBearer(auto_error=False)


def get_current_user_id(
    credentials: HTTPAuthorizationCredentials | None = Depends(
        bearer_scheme
    ),
) -> UUID:
    if credentials is None:
        from fastapi import HTTPException

        raise HTTPException(
            status_code=401,
            detail="Token de autenticação obrigatório",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return validate_access_token(credentials.credentials)