"""Pydantic schemas for the API."""

from __future__ import annotations

from datetime import date, datetime, time
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class ClockInRequest(BaseModel):
    work_order_assembly_id: int = Field(alias="workOrderAssemblyId")
    user_id: int = Field(alias="userId")
    division_fk: int = Field(alias="divisionFK")
    device_date: Optional[datetime] = Field(default=None, alias="deviceDate")

    model_config = {"populate_by_name": True}


class ClockInResponse(BaseModel):
    status: str
    work_order_collection_id: Optional[int] = Field(
        default=None, alias="workOrderCollectionId"
    )

    model_config = {"populate_by_name": True}


class ClockOutRequest(BaseModel):
    work_order_collection_id: int = Field(alias="workOrderCollectionId")
    quantity: Decimal
    quantity_scrapped: Decimal = Field(alias="quantityScrapped")
    scrap_reason_pk: int = Field(alias="scrapReasonPK")
    complete: bool
    comment: Optional[str] = None
    device_time: Optional[datetime] = Field(default=None, alias="deviceTime")
    division_fk: int = Field(alias="divisionFK")

    model_config = {"populate_by_name": True}


class ClockOutResponse(BaseModel):
    status: str

    model_config = {"populate_by_name": True}


class UserStatusResponse(BaseModel):
    user_id: int = Field(alias="userId")
    first_name: str = Field(alias="firstName")
    last_name: str = Field(alias="lastName")
    work_order_collection_id: Optional[int] = Field(
        default=None, alias="workOrderCollectionId"
    )
    work_order_number: Optional[str] = Field(
        default=None, alias="workOrderNumber"
    )
    work_order_assembly_number: Optional[int] = Field(
        default=None, alias="workOrderAssemblyNumber"
    )
    clock_in_time: Optional[datetime] = Field(default=None, alias="clockInTime")
    part_number: Optional[str] = Field(default=None, alias="partNumber")
    operation_code: Optional[str] = Field(default=None, alias="operationCode")
    operation_name: Optional[str] = Field(default=None, alias="operationName")

    model_config = {"populate_by_name": True}

    @field_validator("work_order_number", mode="before")
    @classmethod
    def _coerce_work_order_number(cls, value: object) -> Optional[str]:
        """Ensure the work order number is serialized as a string."""

        if value is None:
            return None
        return str(value)

    @field_validator("clock_in_time", mode="before")
    @classmethod
    def _parse_clock_in_time(
        cls, value: object
    ) -> Optional[datetime]:  # pragma: no cover - parsing is data dependent
        """Coerce various database representations into ``datetime``."""

        if value in (None, ""):
            return None

        if isinstance(value, datetime):
            return value

        if isinstance(value, date) and not isinstance(value, datetime):
            return datetime.combine(value, datetime.min.time())

        if isinstance(value, time):
            return datetime.combine(date.today(), value)

        if isinstance(value, str):
            # Try ISO first
            try:
                return datetime.fromisoformat(value)
            except ValueError:
                pass

            time_formats = ["%H:%M:%S", "%H:%M"]
            for fmt in time_formats:
                try:
                    parsed_time = datetime.strptime(value, fmt).time()
                except ValueError:
                    continue
                return datetime.combine(date.today(), parsed_time)

        raise ValueError("Invalid clock_in_time value")
