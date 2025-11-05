"""Database helpers for accessing work order information."""

from __future__ import annotations

import os
from contextlib import closing
from typing import Any, Mapping

import pymssql

from .models import WorkOrderDetailsResponse


class DatabaseConfigurationError(RuntimeError):
    """Raised when mandatory database configuration is missing."""


def _get_required_env(name: str) -> str:
    value = os.getenv(name)
    if value is None or not value.strip():
        raise DatabaseConfigurationError(
            f"Missing required environment variable: {name}"
        )
    return value


def get_connection() -> pymssql.Connection:
    """Create a new ``pymssql`` connection using environment credentials."""

    return pymssql.connect(
        server=_get_required_env("DB_SERVER"),
        user=_get_required_env("DB_USER"),
        password=_get_required_env("DB_PASSWORD"),
        database=_get_required_env("DB_NAME"),
    )


def _row_represents_closed_assembly(row: Mapping[str, Any]) -> bool:
    """Return ``True`` when the SQL result indicates a closed assembly."""

    candidates = (
        row.get("IsClosed"),
        row.get("isClosed"),
        row.get("Closed"),
        row.get("closed"),
        row.get("Status"),
        row.get("status"),
    )

    for value in candidates:
        if value is None:
            continue
        if isinstance(value, bool):
            return value
        if isinstance(value, (int, float)):
            return value != 0
        if isinstance(value, str):
            normalized = value.strip().lower()
            if normalized in {"1", "true", "yes", "closed", "complete"}:
                return True
    return False


def fetch_work_order_details(assembly_id: int) -> WorkOrderDetailsResponse | None:
    """Return work order details fetched from the database.

    ``None`` is returned when the assembly does not exist or is closed.
    """

    query = (
        """
        SELECT
            woa.WorkOrderAssemblyId,
            wo.WorkOrderNumber,
            woa.WorkOrderAssemblyNumber,
            woa.PartNumber,
            op.OperationCode,
            op.OperationName,
            woa.Description,
            woa.IsClosed
        FROM WorkOrderAssembly AS woa
        INNER JOIN WorkOrder AS wo ON wo.WorkOrderPK = woa.WorkOrderFK
        LEFT JOIN Operation AS op ON op.OperationPK = woa.OperationFK
        WHERE woa.WorkOrderAssemblyId = %s
        """
    )

    with closing(get_connection()) as conn:
        with conn.cursor(as_dict=True) as cursor:
            cursor.execute(query, (assembly_id,))
            row = cursor.fetchone()

    if row is None or _row_represents_closed_assembly(row):
        return None

    return WorkOrderDetailsResponse(
        workOrderAssemblyId=row["WorkOrderAssemblyId"],
        workOrderNumber=row.get("WorkOrderNumber"),
        workOrderAssemblyNumber=row.get("WorkOrderAssemblyNumber"),
        partNumber=row.get("PartNumber"),
        operationCode=row.get("OperationCode"),
        operationName=row.get("OperationName"),
        description=row.get("Description"),
    )
