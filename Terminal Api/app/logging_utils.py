"""Utilities for structured logging across the application."""

from __future__ import annotations

import json
from contextvars import ContextVar, Token
from typing import Any, Mapping

_REQUEST_ID_CTX_VAR: ContextVar[str | None] = ContextVar(
    "request_id", default=None
)


def set_request_id(request_id: str) -> Token[str | None]:
    """Store the given request id in a context variable."""

    return _REQUEST_ID_CTX_VAR.set(request_id)


def reset_request_id(token: Token[str | None]) -> None:
    """Reset the request id context variable using the provided token."""

    _REQUEST_ID_CTX_VAR.reset(token)


def get_request_id() -> str | None:
    """Return the current request id if available."""

    return _REQUEST_ID_CTX_VAR.get()


def log_json(payload: Mapping[str, Any]) -> None:
    """Emit the provided mapping as a JSON string to stdout."""

    print(json.dumps(payload, default=str), flush=True)
