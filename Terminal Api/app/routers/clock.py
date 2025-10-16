"""Clock related endpoints router."""

from __future__ import annotations

from contextlib import closing
from datetime import datetime
import logging
from typing import Optional

import pymssql
from fastapi import APIRouter, HTTPException, status

from ..db import get_conn
from ..logging_utils import get_request_id, log_json
from ..schemas import (
    ClockInRequest,
    ClockInResponse,
    ClockOutRequest,
    ClockOutResponse,
)
_logger = logging.getLogger(__name__)


router = APIRouter(prefix="", tags=["clock"])


def _extract_status(row: Optional[dict[str, object]]) -> str:
    """Return the status string from a stored procedure SELECT row."""

    if not row:
        return ""
    # Stored procedures return `Status` (capitalized); fall back to lower case if needed.
    return str(row.get("Status") or row.get("status") or "")


@router.post("/clock-in", response_model=ClockInResponse)
def clock_in(payload: ClockInRequest) -> ClockInResponse:
    """Execute the clock-in stored procedure and return the status."""

    device_date = payload.device_date or datetime.utcnow()

    try:
        with closing(get_conn()) as conn:
            with conn.cursor(as_dict=True) as cursor:
                sp_name = "dbo.usp_mie_api_ClockInWorkOrderAssembly"
                sp_params = (
                    payload.work_order_assembly_id,
                    payload.user_id,
                    payload.division_fk,
                    device_date,
                )
                param_names = (
                    "work_order_assembly_id",
                    "user_id",
                    "division_fk",
                    "device_date",
                )
                log_json(
                    {
                        "level": "INFO",
                        "event": "stored_procedure.call",
                        "request_id": get_request_id(),
                        "name": sp_name,
                        "params": {
                            name: value for name, value in zip(param_names, sp_params)
                        },
                    }
                )
                cursor.callproc(
                    sp_name,
                    sp_params,
                )
                sp_status = _extract_status(cursor.fetchone())
                if not sp_status:
                    log_json(
                        {
                            "level": "ERROR",
                            "event": "stored_procedure.empty_status",
                            "request_id": get_request_id(),
                            "name": sp_name,
                        }
                    )
                    raise HTTPException(
                        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                        detail="DB_ERROR",
                    )
                # Ensure all result sets are consumed before the next query.
                while cursor.nextset():
                    pass

                cursor.execute(
                    """
                    SELECT TOP 1 WorkOrderCollectionPK
                    FROM WorkOrderCollection
                    WHERE EmployeeFK = %s AND WorkOrderAssemblyNumber = %s
                    ORDER BY WorkOrderCollectionPK DESC
                    """,
                    (payload.user_id, payload.work_order_assembly_id),
                )
                work_order_row = cursor.fetchone()
                work_order_collection_id = (
                    work_order_row.get("WorkOrderCollectionPK") if work_order_row else None
                )

                conn.commit()
    except pymssql.Error as exc:  # pragma: no cover - requires live DB
        _logger.exception("Database error during clock-in")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="DB_ERROR",
        ) from exc

    log_json(
        {
            "level": "INFO",
            "event": "stored_procedure.result",
            "request_id": get_request_id(),
            "name": "dbo.usp_mie_api_ClockInWorkOrderAssembly",
            "status": sp_status,
        }
    )

    return ClockInResponse(
        status=sp_status, work_order_collection_id=work_order_collection_id
    )


@router.post("/clock-out", response_model=ClockOutResponse)
def clock_out(payload: ClockOutRequest) -> ClockOutResponse:
    """Execute the clock-out stored procedure and return the status."""

    device_time_str = (payload.device_time or datetime.utcnow()).strftime("%Y-%m-%dT%H:%M:%S")

    try:
        with closing(get_conn()) as conn:
            with conn.cursor(as_dict=True) as cursor:
                sp_name = "dbo.usp_mie_api_ClockOutWorkOrderCollection"
                sp_params = (
                    payload.work_order_collection_id,
                    payload.quantity,
                    payload.quantity_scrapped,
                    payload.scrap_reason_pk,
                    int(payload.complete),
                    payload.comment,
                    device_time_str,
                    payload.division_fk,
                )
                log_json(
                    {
                        "level": "INFO",
                        "event": "stored_procedure.call",
                        "request_id": get_request_id(),
                        "name": sp_name,
                        "params": {
                            "work_order_collection_id": payload.work_order_collection_id,
                            "quantity": payload.quantity,
                            "quantity_scrapped": payload.quantity_scrapped,
                            "scrap_reason_pk": payload.scrap_reason_pk,
                            "complete": int(payload.complete),
                            "comment": payload.comment,
                            "device_time": device_time_str,
                            "division_fk": payload.division_fk,
                        },
                    }
                )
                cursor.callproc(
                    sp_name,
                    sp_params,
                )
                sp_status = _extract_status(cursor.fetchone())
                if not sp_status:
                    log_json(
                        {
                            "level": "ERROR",
                            "event": "stored_procedure.empty_status",
                            "request_id": get_request_id(),
                            "name": sp_name,
                        }
                    )
                    raise HTTPException(
                        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                        detail="DB_ERROR",
                    )
                while cursor.nextset():
                    pass

                conn.commit()
    except pymssql.Error as exc:  # pragma: no cover - requires live DB
        _logger.exception("Database error during clock-out")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="DB_ERROR",
        ) from exc

    log_json(
        {
            "level": "INFO",
            "event": "stored_procedure.result",
            "request_id": get_request_id(),
            "name": "dbo.usp_mie_api_ClockOutWorkOrderCollection",
            "status": sp_status,
        }
    )

    return ClockOutResponse(status=sp_status)
