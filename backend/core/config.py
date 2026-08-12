import logging
from functools import lru_cache
from pathlib import Path
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict

logger = logging.getLogger(__name__)

# Addresses that resolve to the backend itself rather than to a host a phone
# can reach. Devices are handed SIP_HOST verbatim, so these are never right.
_UNREACHABLE_SIP_HOSTS = {"127.0.0.1", "0.0.0.0", "localhost", "::1"}

ROOT_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=ROOT_DIR / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    ENVIRONMENT: str
    SERVER_HOST: str = "0.0.0.0"
    SERVER_PORT: int = 8000

    asterisk_host: str = "localhost"
    asterisk_ami_port: int = 5038
    asterisk_ami_user: str = "admin"
    asterisk_ami_pass: str = "changeme"

    # ── SIP, as devices should reach it ────────────────────────────────
    # Reported to devices via /server/info and used verbatim to build their
    # Linphone account, so this must be routable *from the handset* — not
    # from the backend. "localhost" or a Docker service name is always wrong
    # here even though both work for AMI above.
    #
    # It is deliberately separate from the API's own address: the API may sit
    # behind a reverse proxy or an HTTP tunnel, but SIP is UDP/TCP on 5060 and
    # RTP is UDP on high ports, so devices must reach Asterisk directly.
    sip_host: str = "127.0.0.1"
    sip_port: int = 5060
    # udp is the SIP default and what pjsip.conf binds first. tcp keeps the
    # connection open, which survives NAT without registration refreshes.
    # tls additionally encrypts signalling (pjsip.conf's transport-tls,
    # normally on 5061) and requires sip_host to be a real hostname, since
    # certs can't be issued for a bare IP. Media stays RTP/UDP under all of
    # these — this is signalling only.
    sip_transport: Literal["udp", "tcp", "tls"] = "udp"

    # Fernet key encrypting secrets-at-rest (e.g. numbers.sip_password).
    # Generate with: python -c "from cryptography.fernet import Fernet; \
    # print(Fernet.generate_key().decode())"
    secrets_encryption_key: str

    postgres_db: str = "nextelis"
    postgres_user: str = "nextelis"
    postgres_password: str = "changeme"
    postgres_port: int = 5433
    postgres_host: str = "localhost"

    database_url: str

    # Connection pool (SQLAlchemy AsyncAdaptedQueuePool)
    db_pool_size: int = 5
    db_max_overflow: int = 10
    db_pool_timeout_seconds: int = 30
    db_pool_recycle_seconds: int = 1800


@lru_cache
def get_settings() -> Settings:
    settings = Settings()

    # Surface a SIP address phones can't reach at startup rather than letting
    # it show up later as registrations that never arrive. A warning, not an
    # error: running everything on one machine for development is legitimate.
    if settings.sip_host in _UNREACHABLE_SIP_HOSTS:
        logger.warning(
            "SIP_HOST is %r, which resolves to the backend itself. Phones "
            "cannot reach Asterisk at that address — set SIP_HOST to this "
            "machine's LAN or public IP for calls to work.",
            settings.sip_host,
        )

    return settings
