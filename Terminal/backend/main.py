from fastapi import FastAPI, HTTPException
from fastapi.concurrency import run_in_threadpool

import pymssql

from .models import (
    ApiResponse,
    ClockInRequest,
    ClockOutRequest,
    WorkOrderDetailsResponse,
)

from .database import DatabaseConfigurationError, fetch_work_order_details

app = FastAPI()

@app.post("/clock-in", response_model=ApiResponse)
async def clock_in(request: ClockInRequest) -> ApiResponse:
    return ApiResponse(status="success", message="Clock In registrado correctamente")


@app.post("/clock-out", response_model=ApiResponse)
async def clock_out(request: ClockOutRequest) -> ApiResponse:
    status_message = "Clock Out completado" if request.complete else "Clock Out pendiente"
    return ApiResponse(status="success", message=status_message)


@app.get("/work-orders/{assembly_id}", response_model=WorkOrderDetailsResponse)
async def get_work_order_details(assembly_id: int) -> WorkOrderDetailsResponse:
    try:
        details = await run_in_threadpool(fetch_work_order_details, assembly_id)
    except DatabaseConfigurationError as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    except pymssql.Error as exc:  # pragma: no cover - requires live DB
        raise HTTPException(status_code=500, detail="Database error") from exc

    if details is None:
        raise HTTPException(status_code=404, detail="Work order not found")

    return details
