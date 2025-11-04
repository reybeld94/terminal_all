import pytest

from ..models import ClockInRequest, WorkOrderDetailsResponse


def test_clock_in_request_rejects_zero_user_id():
    with pytest.raises(ValueError) as exc:
        ClockInRequest(
            workOrderAssemblyId=1,
            userId="0",
            divisionFK=1,
            deviceDate="2024-01-01T12:00:00+00:00",
        )

    assert "positive integer" in str(exc.value)


def test_clock_in_request_accepts_positive_user_id():
    request = ClockInRequest(
        workOrderAssemblyId=5,
        userId="123",
        divisionFK=1,
        deviceDate="2024-01-01T12:00:00+00:00",
    )

    assert request.userId == 123


def test_work_order_details_response_accepts_partial_payload():
    response = WorkOrderDetailsResponse(
        workOrderAssemblyId=42,
        workOrderNumber="WO-42",
        operationName="Assembly"
    )

    assert response.workOrderAssemblyId == 42
    assert response.workOrderAssemblyNumber is None
    assert response.operationName == "Assembly"
