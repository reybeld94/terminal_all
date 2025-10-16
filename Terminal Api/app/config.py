"""Application configuration module."""

from __future__ import annotations

import os
from dataclasses import dataclass
from functools import lru_cache
from typing import Optional

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class Settings:
    """Runtime configuration loaded from environment variables."""

    db_server: str
    db_user: str
    db_password: str
    db_name: str

    @classmethod
    def from_env(cls) -> "Settings":
        """Build settings using environment variables."""

        def read(key: str, default: Optional[str] = None) -> str:
            value = os.getenv(key, default)
            if value is None:
                raise RuntimeError(f"Missing required environment variable: {key}")
            return value

        return cls(
            db_server=read("DB_SERVER", ""),
            db_user=read("DB_USER", ""),
            db_password=read("DB_PASSWORD", ""),
            db_name=read("DB_NAME", ""),
        )


@lru_cache()
def get_settings() -> Settings:
    """Return a cached settings instance."""

    return Settings.from_env()
