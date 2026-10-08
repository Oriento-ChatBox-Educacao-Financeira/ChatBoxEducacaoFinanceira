import os
from pathlib import Path


def get_jwt_public_key() -> str:
    key_path = os.getenv("JWT_PUBLIC_KEY_PATH")

    if not key_path:
        raise RuntimeError(
            "JWT_PUBLIC_KEY_PATH não configurada"
        )

    return Path(key_path).read_text(encoding="utf-8")