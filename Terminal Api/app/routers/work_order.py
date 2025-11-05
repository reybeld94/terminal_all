"""Work order assembly lookup endpoints."""

from __future__ import annotations

from contextlib import closing
from typing import Any, Mapping

import pymssql
from fastapi import APIRouter, HTTPException, status

from ..db import get_conn
from ..logging_utils import get_request_id, log_json
from ..schemas import WorkOrderDetailsResponse

router = APIRouter(prefix="", tags=["work-orders"])


_WORK_ORDER_ASSEMBLY_QUERY = """
SELECT
    woa.WorkOrderAssemblyPK AS WorkOrderAssemblyId,
    wo.WorkOrderNumber,
    woa.SequenceNumber AS WorkOrderAssemblyNumber,
    woa.PartNumber,
    COALESCE(
      NULLIF(woa.HardwareDescription,''),
      NULLIF(woa.MaterialDescription,''),
      NULLIF(woa.OutsideProcessingDescription,''),
      NULLIF(i.Description,''),
      NULLIF(ri.Description,''),
      NULLIF(woa.PartNumber,''),
      NULLIF(i.PartNumber,''),
      NULLIF(ri.PartNumber,'')
    ) AS DescriptionResolved,
    COALESCE(NULLIF(i.PartNumber,''), NULLIF(ri.PartNumber,''), NULLIF(woa.PartNumber,'')) AS PartNumberResolved,
    op.Code AS OperationCode,
    op.Name AS OperationName,
    CASE
      WHEN woa.QuantityToFabricate IS NOT NULL AND woa.QuantityIssued >= woa.QuantityToFabricate THEN CAST(1 AS bit)
      ELSE CAST(0 AS bit)
    END AS IsAssemblyClosed,
    CASE WHEN wo.ClosedDate IS NOT NULL THEN CAST(1 AS bit) ELSE CAST(0 AS bit) END AS IsWorkOrderClosed,
    CASE WHEN wo.ReleasedDate IS NOT NULL THEN CAST(1 AS bit) ELSE CAST(0 AS bit) END AS IsReleased
FROM dbo.WorkOrderAssembly AS woa
INNER JOIN dbo.WorkOrder AS wo ON wo.WorkOrderPK = woa.WorkOrderFK
LEFT JOIN dbo.Operation AS op ON op.OperationPK = woa.OperationFK
LEFT JOIN dbo.Item AS i ON i.ItemPK = woa.ItemFK
LEFT JOIN dbo.Router AS r ON r.RouterPK = woa.RouterFK
LEFT JOIN dbo.Item AS ri ON ri.ItemPK = r.ItemFK
WHERE woa.WorkOrderAssemblyPK = %s
"""


def _to_bool(value: Any) -> bool:
    """Coerce database values to ``bool`` consistently."""

    if isinstance(value, bool):
        return value
    if value is None:
        return False
    if isinstance(value, (int, float)):
        return bool(value)
    if isinstance(value, str):
        lowered = value.strip().lower()
        return lowered in {"1", "true", "t", "yes", "y"}
    return False


def _raise_validation_error(reason: str) -> None:
    """Raise an HTTP 400 error with the provided message."""

    raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=reason)


def _validate_work_order_status(row: Mapping[str, Any]) -> None:
    """Ensure the work order and assembly can be used for clock-in operations."""

    if _to_bool(row.get("IsWorkOrderClosed")):
        _raise_validation_error("This work order is already closed.")
    if not _to_bool(row.get("IsReleased")):
        _raise_validation_error("This work order has not been released yet.")
    if _to_bool(row.get("IsAssemblyClosed")):
        _raise_validation_error("This assembly is already completed.")


@router.get("/work-orders/{assembly_id}", response_model=WorkOrderDetailsResponse)
def get_work_order_details(assembly_id: int) -> WorkOrderDetailsResponse:
    """Return the details of a work order assembly after validating its status."""

    try:
        with closing(get_conn()) as conn:
            with conn.cursor(as_dict=True) as cursor:
                log_json(
                    {
                        "level": "INFO",
                        "event": "work_order.lookup",
                        "request_id": get_request_id(),
                        "params": {"assembly_id": assembly_id},
                    }
                )
                cursor.execute(_WORK_ORDER_ASSEMBLY_QUERY, (assembly_id,))
                row = cursor.fetchone()
    except pymssql.Error as exc:  # pragma: no cover - requires live DB
        log_json(
            {
                "level": "ERROR",
                "event": "work_order.lookup.error",
                "request_id": get_request_id(),
                "error": str(exc),
            }
        )
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="DB_ERROR"
        ) from exc

    if not row:
        log_json(
            {
                "level": "WARNING",
                "event": "work_order.lookup.not_found",
                "request_id": get_request_id(),
                "params": {"assembly_id": assembly_id},
            }
        )
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Assembly not found.",
        )

    try:
        _validate_work_order_status(row)
    except HTTPException as exc:
        log_json(
            {
                "level": "WARNING",
                "event": "work_order.lookup.invalid",
                "request_id": get_request_id(),
                "params": {"assembly_id": assembly_id},
                "reason": exc.detail,
            }
        )
        raise

    log_json(
        {
            "level": "INFO",
            "event": "work_order.lookup.success",
            "request_id": get_request_id(),
            "params": {"assembly_id": assembly_id},
        }
    )

    return WorkOrderDetailsResponse(
        work_order_assembly_id=int(row.get("WorkOrderAssemblyId") or assembly_id),
        work_order_number=row.get("WorkOrderNumber"),
        work_order_assembly_number=row.get("WorkOrderAssemblyNumber"),
        part_number=row.get("PartNumberResolved") or row.get("PartNumber"),
        operation_code=row.get("OperationCode"),
        operation_name=row.get("OperationName"),
        description=row.get("DescriptionResolved"),
        is_work_order_closed=_to_bool(row.get("IsWorkOrderClosed")),
        is_released=_to_bool(row.get("IsReleased")),
        is_assembly_closed=_to_bool(row.get("IsAssemblyClosed")),
    )
