from fastapi import FastAPI, HTTPException

from .models import (
    ApiResponse,
    ClockInRequest,
    ClockOutRequest,
    WorkOrderDetailsResponse,
)

app = FastAPI()

WORK_ORDERS = {
    129454: WorkOrderDetailsResponse(
        workOrderAssemblyId=129454,
        workOrderNumber="WO-129454",
        workOrderAssemblyNumber="A129454",
        partNumber="PN-8745",
        operationCode="OP10",
        operationName="Initial Assembly",
        description="Assembly 129454 - Preparation and setup",
    ),
    120001: WorkOrderDetailsResponse(
        workOrderAssemblyId=120001,
        workOrderNumber="WO-120001",
        workOrderAssemblyNumber="A120001",
        partNumber="PN-3301",
        operationCode="OP20",
        operationName="Welding",
        description="Frame welding sequence",
    ),
    130010: WorkOrderDetailsResponse(
        workOrderAssemblyId=130010,
        workOrderNumber="WO-130010",
        workOrderAssemblyNumber="A130010",
        partNumber="PN-9920",
        operationCode="OP30",
        operationName="Quality Inspection",
        description="Inspection and QA verification",
    ),
}


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
        return WORK_ORDERS[assembly_id]
    except KeyError as exc:
        raise HTTPException(status_code=404, detail="Work order not found") from exc
