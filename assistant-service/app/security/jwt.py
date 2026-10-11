from uuid import UUID

import jwt
from fastapi import HTTPException, status

from app.security.config import (
    get_jwt_algorithm,
    get_jwt_issuer,
    get_jwt_public_key,
)


def validate_access_token(token: str) -> UUID:
    try:
        payload = jwt.decode(
            token,
            get_jwt_public_key(),
            algorithms=[get_jwt_algorithm()],
            issuer=get_jwt_issuer(),
            options={
                "require": [
                    "sub",
                    "iss",
                    "iat",
                    "exp",
                ],
            },
        )

        return UUID(payload["sub"])

    except (
        jwt.PyJWTError,
        ValueError,
        TypeError,
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token inválido ou expirado",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )
