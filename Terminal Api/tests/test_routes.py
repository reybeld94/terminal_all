"""Tests to ensure clock endpoints are public."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.routers import clock, user  # noqa: E402


def test_routers_have_no_dependencies() -> None:
    """Ensure the routers do not enforce authentication."""

    assert clock.router.dependencies == []
    assert user.router.dependencies == []
