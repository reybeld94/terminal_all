"""User related endpoints router."""

from __future__ import annotations

from contextlib import closing
import logging
from typing import Optional

import pymssql
from fastapi import APIRouter, HTTPException, status

from ..db import get_conn
from ..logging_utils import get_request_id, log_json
from ..schemas import UserStatusResponse

_logger = logging.getLogger(__name__)


router = APIRouter(prefix="", tags=["user"])


_USER_QUERY = """
SELECT UserPK, FirstName, LastName
FROM dbo.[User]
WHERE Code = %s
"""


_ACTIVE_WORK_ORDER_QUERY = """
SELECT TOP (1)
    w.WorkOrderCollectionPK,
    w.WorkOrderNumber,
    w.WorkOrderAssemblyNumber,
    w.TimeOn,
    wo.PartNumber,
    op.Code AS OperationCode,
    op.Name AS OperationName
FROM dbo.WorkOrderCollection AS w
LEFT JOIN dbo.WorkOrder AS wo
       ON wo.WorkOrderNumber = w.WorkOrderNumber
LEFT JOIN dbo.WorkOrderAssembly AS wa
       ON wa.WorkOrderFK = wo.WorkOrderPK
      AND wa.SequenceNumber = w.WorkOrderAssemblyNumber
LEFT JOIN dbo.Operation AS op
       ON op.OperationPK = wa.OperationFK
WHERE w.EmployeeFK = %s
  AND w.TimeOff IS NULL
  AND w.TimeOn IS NOT NULL
ORDER BY w.TimeOn DESC
"""


def _safe_get(row: Optional[dict[str, object]], key: str) -> Optional[object]:
    """Return row[key] if row exists, otherwise ``None``."""

    if not row:
        return None
    return row.get(key)


@router.get("/users/{employee_id}", response_model=UserStatusResponse)
def get_user_status(employee_id: str) -> UserStatusResponse:
    """Validate a user exists and return their active work order information."""

    try:
        with closing(get_conn()) as conn:
            with conn.cursor(as_dict=True) as cursor:
                log_json(
                    {
                        "level": "INFO",
                        "event": "user.lookup",
                        "request_id": get_request_id(),
                        "query": "USER_BY_CODE",
                        "params": {"code": employee_id},
                    }
                )
                cursor.execute(_USER_QUERY, (employee_id,))
                user_row = cursor.fetchone()
                log_json(
                    {
                        "level": "INFO",
                        "event": "user.lookup.result",
                        "request_id": get_request_id(),
                        "query": "USER_BY_CODE",
                        "result": bool(user_row),
                    }
                )

                if not user_row:
                    raise HTTPException(
                        status_code=status.HTTP_404_NOT_FOUND,
                        detail="USER_NOT_FOUND",
                    )

                cursor.execute(
                    _ACTIVE_WORK_ORDER_QUERY, (user_row["UserPK"],)
                )
                work_order_row = cursor.fetchone()
    except pymssql.Error as exc:  # pragma: no cover - requires live DB
        _logger.exception("Database error while fetching user status")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="DB_ERROR",
        ) from exc

    return UserStatusResponse(
        user_id=int(user_row["UserPK"]),
        first_name=str(user_row.get("FirstName") or ""),
        last_name=str(user_row.get("LastName") or ""),
        work_order_collection_id=_safe_get(work_order_row, "WorkOrderCollectionPK"),
        work_order_number=_safe_get(work_order_row, "WorkOrderNumber"),
        work_order_assembly_number=_safe_get(
            work_order_row, "WorkOrderAssemblyNumber"
        ),
        clock_in_time=_safe_get(work_order_row, "TimeOn"),
        part_number=_safe_get(work_order_row, "PartNumber"),
        operation_code=_safe_get(work_order_row, "OperationCode"),
        operation_name=_safe_get(work_order_row, "OperationName"),
    )
