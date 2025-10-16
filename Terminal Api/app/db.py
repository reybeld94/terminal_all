"""Database utilities."""

from __future__ import annotations

import pymssql

from .config import get_settings


def get_conn() -> pymssql.Connection:
    """Return a new pymssql connection using configuration settings."""

    settings = get_settings()
    return pymssql.connect(
        server=settings.db_server,
        user=settings.db_user,
        password=settings.db_password,
        database=settings.db_name,
    )
