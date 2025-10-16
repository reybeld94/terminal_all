from fastapi import FastAPI

from .models import ApiResponse, ClockInRequest, ClockOutRequest

app = FastAPI()


@app.post("/clock-in", response_model=ApiResponse)
async def clock_in(request: ClockInRequest) -> ApiResponse:
    return ApiResponse(status="success", message="Clock In registrado correctamente")


@app.post("/clock-out", response_model=ApiResponse)
async def clock_out(request: ClockOutRequest) -> ApiResponse:
    status_message = "Clock Out completado" if request.complete else "Clock Out pendiente"
    return ApiResponse(status="success", message=status_message)
