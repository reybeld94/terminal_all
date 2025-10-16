"""FastAPI application entry point."""

from __future__ import annotations

import json
import time
import traceback
import uuid
from datetime import datetime, timezone
from typing import Any, Callable, Mapping

from fastapi import FastAPI, Request
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import JSONResponse, Response
from starlette.types import Message

from .routers import clock, user
from .logging_utils import log_json, reset_request_id, set_request_id


def _now_iso() -> str:
    """Return the current UTC time formatted in ISO 8601."""

    return datetime.now(tz=timezone.utc).isoformat()


class RequestIdMiddleware(BaseHTTPMiddleware):
    """Attach a request_id to each incoming request and log request details."""

    async def dispatch(
        self, request: Request, call_next: Callable[[Request], Response]
    ) -> Response:
        request_id = str(uuid.uuid4())
        request.state.request_id = request_id
        token = set_request_id(request_id)
        start_time = time.perf_counter()

        body_bytes = await request.body()
        body_sent = False

        async def receive() -> Message:  # type: ignore[override]
            nonlocal body_sent
            if body_sent:
                return {"type": "http.request", "body": b"", "more_body": False}
            body_sent = True
            return {"type": "http.request", "body": body_bytes, "more_body": False}

        request._receive = receive  # type: ignore[attr-defined]

        log_json(
            _request_log_payload(
                request,
                request_id,
                event="request.received",
                body=_safe_parse_body(body_bytes),
            )
        )

        try:
            response = await call_next(request)
        except Exception:  # pragma: no cover - defensive guard
            response = JSONResponse(
                {"detail": "Internal Server Error"}, status_code=500
            )
            latency_ms = (time.perf_counter() - start_time) * 1000
            log_json(
                {
                    "time": _now_iso(),
                    "level": "ERROR",
                    "event": "request.failed",
                    "request_id": request_id,
                    "method": request.method,
                    "path": request.url.path,
                    "status_code": response.status_code,
                    "latency_ms": latency_ms,
                    "error": traceback.format_exc(limit=3).strip(),
                }
            )
            response.headers["X-Request-ID"] = request_id
            return response
        finally:
            reset_request_id(token)

        latency_ms = (time.perf_counter() - start_time) * 1000
        response.headers["X-Request-ID"] = request_id

        log_json(
            _request_log_payload(
                request,
                request_id,
                event="request.completed",
                status_code=response.status_code,
                latency_ms=latency_ms,
            )
        )

        return response


def _safe_parse_body(body_bytes: bytes) -> Any:
    """Attempt to decode the request body into JSON or UTF-8 text."""

    if not body_bytes:
        return None
    try:
        return json.loads(body_bytes)
    except json.JSONDecodeError:
        return body_bytes.decode("utf-8", "replace")


def _request_log_payload(
    request: Request,
    request_id: str,
    *,
    event: str,
    body: Any | None = None,
    status_code: int | None = None,
    latency_ms: float | None = None,
) -> Mapping[str, Any]:
    """Build a log payload for request lifecycle events."""

    payload: dict[str, Any] = {
        "time": _now_iso(),
        "level": "INFO",
        "event": event,
        "request_id": request_id,
        "method": request.method,
        "path": request.url.path,
    }
    if request.query_params:
        payload["query"] = dict(request.query_params)
    if status_code is not None:
        payload["status_code"] = status_code
    if latency_ms is not None:
        payload["latency_ms"] = latency_ms
    if body is not None:
        payload["body"] = body
    return payload


def create_app() -> FastAPI:
    """Application factory."""

    application = FastAPI()
    application.add_middleware(RequestIdMiddleware)
    application.include_router(clock.router)
    application.include_router(user.router)

    return application


app = create_app()
