from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

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
    return Settings()
