"""Pydantic schemas for the API."""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, Field


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
