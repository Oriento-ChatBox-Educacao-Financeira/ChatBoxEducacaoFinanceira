import os
from pathlib import Path


ALLOWED_JWT_ALGORITHMS = {
    "RS256",
    "RS384",
    "RS512",
}


def get_jwt_public_key() -> str:
    key_path = os.getenv("JWT_PUBLIC_KEY_PATH")

    if not key_path:
        raise RuntimeError(
            "JWT_PUBLIC_KEY_PATH não configurada"
        )

    return Path(key_path).read_text(encoding="utf-8")


def get_jwt_issuer() -> str:
    return os.getenv(
        "JWT_ISSUER",
        "oriento",
    )


def get_jwt_algorithm() -> str:
    algorithm = os.getenv(
        "JWT_ALGORITHM",
        "RS256",
    )

    if algorithm not in ALLOWED_JWT_ALGORITHMS:
        raise RuntimeError(
            f"Algoritmo JWT não permitido: {algorithm}"
        )

    return algorithm
