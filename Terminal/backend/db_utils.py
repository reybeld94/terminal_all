"""Utility helpers for working with database stored procedures."""

from __future__ import annotations

from collections.abc import Sequence
from typing import Any, Tuple


def _normalize_parameters(
    params: Sequence[Any] | Sequence[Tuple[str, Any]] | Sequence[Tuple[str, Any, Any]]
) -> tuple[list[str], list[Any]]:
    names: list[str] = []
    values: list[Any] = []

    for index, param in enumerate(params):
        if isinstance(param, tuple):
            if len(param) == 2:
                name, value = param
            elif len(param) >= 3:
                name, value = param[0], param[1]
            else:
                raise ValueError("Parameter tuple must have at least two items (name, value)")
        else:
            name = f"arg{index}"
            value = param

        names.append(str(name))
        values.append(value)

    return names, values


def callproc_with_debug(cursor: Any, proc_name: str, params: Sequence[Any]) -> Any:
    """Call ``cursor.callproc`` logging parameter values and their types.

    ``params`` can be a simple sequence of values (``list``/``tuple``) or a
    sequence of ``(name, value)`` tuples.  Names are used purely for debugging
    output so the original parameter order is preserved when invoking the stored
    procedure.
    """

    param_names, param_values = _normalize_parameters(params)

    print(f"[DEBUG] Calling stored procedure '{proc_name}' with parameters:")
    for name, value in zip(param_names, param_values):
        print(f"  - {name}: {value!r} (type={type(value).__name__})")

    return cursor.callproc(proc_name, param_values)
