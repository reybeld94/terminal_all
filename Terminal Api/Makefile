PYTHON ?= python3
PIP ?= pip3
APP_MODULE = app.main:app
UVICORN = uvicorn

.PHONY: install run test lint fmt

install:
$(PIP) install -r requirements.txt

run:
$(UVICORN) $(APP_MODULE) --host 0.0.0.0 --port 8000 --reload

test:
$(PYTHON) -m pytest

lint:
ruff check .

fmt:
black .
isort .
