# Terminal API

Proyecto FastAPI minimalista para gestionar el fichaje de órdenes de trabajo.

## Requisitos

- Python 3.11
- [Poetry](https://python-poetry.org/) no es necesario; se usa `venv` estándar
- Docker (opcional)

## Configuración local

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

Edita `.env` con las credenciales reales.

## Comandos Make

- `make install`: instala dependencias en el entorno activo.
- `make run`: levanta la API en `http://127.0.0.1:8000`.
- `make test`: ejecuta la suite de tests con `pytest`.
- `make lint`: ejecuta `ruff`.
- `make fmt`: formatea con `black` e `isort`.

## Logs

El middleware de la aplicación imprime un log JSON por cada request en `stdout` con los
campos `time`, `level`, `request_id`, `method`, `path`, `status_code` y `latency_ms`.
Puedes verlos ejecutando la API de manera interactiva:

```bash
make run
```

o siguiendo los logs del contenedor si usas Docker:

```bash
docker compose logs -f
```

## Docker

Construir y ejecutar la API:

```bash
docker compose up --build
```

La API quedará disponible en `http://localhost:8000`.

## Ejemplos con cURL

```bash
# Clock-in
curl -X POST http://localhost:8000/clock-in \
  -H "Content-Type: application/json" \
  -d '{
      "workOrderAssemblyId": 1,
      "userId": 42,
      "divisionFK": 7,
      "deviceDate": "2024-01-01T08:00:00"
  }'

# Clock-out
curl -X POST http://localhost:8000/clock-out \
  -H "Content-Type: application/json" \
  -d '{
      "workOrderCollectionId": 123,
    "quantity": 10,
    "quantityScrapped": 0,
    "scrapReasonPK": 1,
    "complete": true,
    "comment": "Proceso finalizado",
    "deviceTime": "2024-01-01T12:00:00",
    "divisionFK": 7
  }'
```
