from sqlalchemy import String
from sqlalchemy.types import TypeDecorator

from backend.core.security import decrypt_secret, encrypt_secret


class EncryptedString(TypeDecorator):
    """Fernet-encrypts a string column at rest; ORM attributes stay plaintext.

    Fernet ciphertext is base64 and roughly 1.4x the plaintext length plus a
    fixed overhead — impl_length must be large enough for the encrypted form,
    not the plaintext form.
    """

    impl = String
    cache_ok = True

    def __init__(self, plaintext_length: int, *args, **kwargs) -> None:
        impl_length = plaintext_length * 2 + 128
        super().__init__(impl_length, *args, **kwargs)

    def process_bind_param(self, value: str | None, dialect) -> str | None:
        if value is None:
            return None
        return encrypt_secret(value)

    def process_result_value(self, value: str | None, dialect) -> str | None:
        if value is None:
            return None
        return decrypt_secret(value)
