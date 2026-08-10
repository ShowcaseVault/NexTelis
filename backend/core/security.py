import hashlib
import secrets
import string
from functools import lru_cache

from cryptography.fernet import Fernet

from backend.core.config import get_settings

_CLAIM_CODE_ALPHABET = string.ascii_uppercase + string.digits
_CLAIM_CODE_LENGTH = 8
_DEVICE_TOKEN_BYTES = 32
_SIP_PASSWORD_BYTES = 24


@lru_cache
def _fernet() -> Fernet:
    return Fernet(get_settings().secrets_encryption_key.encode("utf-8"))


def generate_claim_code() -> str:
    """Short, human-typeable code, e.g. 'K3F9QZ2P'."""
    return "".join(
        secrets.choice(_CLAIM_CODE_ALPHABET) for _ in range(_CLAIM_CODE_LENGTH)
    )


def generate_device_token() -> str:
    """Opaque bearer token handed to a device exactly once, at claim time."""
    return secrets.token_urlsafe(_DEVICE_TOKEN_BYTES)


def hash_device_token(raw_token: str) -> str:
    return hashlib.sha256(raw_token.encode("utf-8")).hexdigest()


def generate_sip_password() -> str:
    """Plaintext SIP auth secret for PJSIP realtime (userpass auth_type)."""
    return secrets.token_urlsafe(_SIP_PASSWORD_BYTES)


def encrypt_secret(raw_value: str) -> str:
    """Used by EncryptedString (backend/db/types.py) — not called directly by
    application code, which reads/writes plaintext through the ORM attribute."""
    return _fernet().encrypt(raw_value.encode("utf-8")).decode("utf-8")


def decrypt_secret(encrypted_value: str) -> str:
    return _fernet().decrypt(encrypted_value.encode("utf-8")).decode("utf-8")
