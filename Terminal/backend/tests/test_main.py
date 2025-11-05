import pymssql
from fastapi.testclient import TestClient

from backend import main
from backend.database import fetch_work_order_details
from backend.models import WorkOrderDetailsResponse


client = TestClient(main.app)


def test_get_work_order_details_returns_data(monkeypatch):
    expected = WorkOrderDetailsResponse(
        workOrderAssemblyId=123,
        workOrderNumber="WO-123",
        workOrderAssemblyNumber="A123",
        partNumber="PN-1",
        operationCode="OP10",
        operationName="Assembly",
        description="Assembly description",
    )

    def fake_fetch(assembly_id: int) -> WorkOrderDetailsResponse | None:
        assert assembly_id == 123
        return expected

    monkeypatch.setattr(main, "fetch_work_order_details", fake_fetch)

    response = client.get("/work-orders/123")

    assert response.status_code == 200
    assert response.json() == expected.dict()


def test_get_work_order_details_returns_404_when_missing(monkeypatch):
    monkeypatch.setattr(main, "fetch_work_order_details", lambda assembly_id: None)

    response = client.get("/work-orders/999")

    assert response.status_code == 404
    assert response.json()["detail"] == "Work order not found"


def test_get_work_order_details_handles_database_errors(monkeypatch):
    def fake_fetch(assembly_id: int) -> WorkOrderDetailsResponse | None:
        raise pymssql.Error("boom")

    monkeypatch.setattr(main, "fetch_work_order_details", fake_fetch)

    response = client.get("/work-orders/10")

    assert response.status_code == 500
    assert response.json()["detail"] == "Database error"


def test_fetch_work_order_details_closes_connection(monkeypatch):
    row = {
        "WorkOrderAssemblyId": 321,
        "WorkOrderNumber": "WO-321",
        "WorkOrderAssemblyNumber": "A321",
        "PartNumber": "PN-9",
        "OperationCode": "OP40",
        "OperationName": "Final",
        "Description": "Final assembly",
        "IsClosed": 0,
    }

    class DummyCursor:
        def __init__(self) -> None:
            self.executed = False

        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def execute(self, query, params):
            self.executed = True

        def fetchone(self):
            return row

    class DummyConnection:
        def __init__(self) -> None:
            self.closed = False

        def cursor(self, *, as_dict):
            assert as_dict is True
            return DummyCursor()

        def close(self):
            self.closed = True

    dummy_connection = DummyConnection()

    monkeypatch.setenv("DB_SERVER", "server")
    monkeypatch.setenv("DB_USER", "user")
    monkeypatch.setenv("DB_PASSWORD", "password")
    monkeypatch.setenv("DB_NAME", "database")
    monkeypatch.setattr(main, "fetch_work_order_details", fetch_work_order_details)
    monkeypatch.setattr("backend.database.get_connection", lambda: dummy_connection)

    result = fetch_work_order_details(321)

    assert isinstance(result, WorkOrderDetailsResponse)
    assert dummy_connection.closed is True


def test_fetch_work_order_details_returns_none_when_closed(monkeypatch):
    class DummyCursor:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def execute(self, query, params):
            pass

        def fetchone(self):
            return {"WorkOrderAssemblyId": 1, "IsClosed": True}

    class DummyConnection:
        def cursor(self, *, as_dict):
            return DummyCursor()

        def close(self):
            pass

    monkeypatch.setenv("DB_SERVER", "server")
    monkeypatch.setenv("DB_USER", "user")
    monkeypatch.setenv("DB_PASSWORD", "password")
    monkeypatch.setenv("DB_NAME", "database")
    monkeypatch.setattr("backend.database.get_connection", lambda: DummyConnection())

    assert fetch_work_order_details(1) is None
