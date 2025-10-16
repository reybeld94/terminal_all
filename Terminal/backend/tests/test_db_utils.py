from unittest.mock import Mock

from backend.db_utils import callproc_with_debug


def test_callproc_with_debug_logs_parameter_values_and_types(capsys):
    cursor = Mock()
    cursor.callproc.return_value = ["result"]

    params = [
        ("work_order_assembly_id", 12345),
        ("user_id", 42),
        ("division_fk", 1),
        ("device_date", "2024-01-01T12:00:00+00:00"),
    ]

    result = callproc_with_debug(cursor, "dbo.uspClockIn", params)

    assert result == ["result"]
    cursor.callproc.assert_called_once_with(
        "dbo.uspClockIn",
        [12345, 42, 1, "2024-01-01T12:00:00+00:00"],
    )

    captured = capsys.readouterr().out
    assert "dbo.uspClockIn" in captured
    assert "user_id" in captured
    assert "42" in captured
    assert "int" in captured
    assert "device_date" in captured
    assert "str" in captured
