"""Tests to ensure clock endpoints are public."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import pytest
from fastapi import HTTPException

from app.routers import clock, user, work_order  # noqa: E402


def test_routers_have_no_dependencies() -> None:
    """Ensure the routers do not enforce authentication."""

    assert clock.router.dependencies == []
    assert user.router.dependencies == []
    assert work_order.router.dependencies == []


@pytest.mark.parametrize(
    "row, expected_message",
    [
        ({"IsWorkOrderClosed": 1, "IsReleased": 1, "IsAssemblyClosed": 0}, "This work order is already closed."),
        ({"IsWorkOrderClosed": 0, "IsReleased": 0, "IsAssemblyClosed": 0}, "This work order has not been released yet."),
        ({"IsWorkOrderClosed": 0, "IsReleased": 1, "IsAssemblyClosed": 1}, "This assembly is already completed."),
    ],
)
def test_validate_work_order_status_rejections(row, expected_message):
    with pytest.raises(HTTPException) as exc_info:
        work_order._validate_work_order_status(row)

    assert expected_message == exc_info.value.detail


def test_validate_work_order_status_success():
    row = {"IsWorkOrderClosed": 0, "IsReleased": 1, "IsAssemblyClosed": 0}

    # Should not raise
    work_order._validate_work_order_status(row)
