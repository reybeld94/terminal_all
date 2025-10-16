Especificación única

Objetivo: crear una “fuente de verdad” para que Codex la respete en todos los pasos.

Eres un desarrollador senior. Este repositorio implementa una API REST minimalista para ejecutar dos Stored Procedures de MIE Trak en SQL Server:

SPs (ya existen en la DB y NO deben modificarse):
- dbo.usp_mie_api_ClockInWorkOrderAssembly(workOrderAssemblyId INT, userId INT, divisionFK INT, deviceDate DATETIME) → SELECT @status AS Status
- dbo.usp_mie_api_ClockOutWorkOrderCollection(workOrderCollectionId INT, quantity DECIMAL(17,5), quantityScrapped DECIMAL(17,5), scrapReasonPK INT, complete BIT, comment NVARCHAR(MAX), deviceTime NVARCHAR(100), divisionFK INT) → SELECT @status AS Status

Contrato REST (estable y versionado):
- POST /clock-in
  body: { workOrderAssemblyId:int, userId:int, divisionFK:int, deviceDate?:ISO8601 }
  resp: { status:string, workOrderCollectionId?:int }

- POST /clock-out
  body: { workOrderCollectionId:int, quantity:number, quantityScrapped:number, scrapReasonPK:int, complete:boolean, comment?:string, deviceTime?:ISO8601, divisionFK:int }
  resp: { status:string }

Requisitos:
- Lenguaje: Python 3.11, FastAPI, uvicorn, pymssql, python-dotenv, PyJWT.
- Seguridad: Autenticación Bearer JWT (HS256). Rechazar peticiones sin token o token inválido.
- Configuración por .env: DB_SERVER, DB_USER, DB_PASSWORD, DB_NAME, JWT_SECRET, JWT_AUDIENCE, JWT_ISSUER.
- Conexión DB: pool simple por conexión por request (pymssql). No usar ORM.
- Manejo de errores: devolver 500 con mensaje “DB_ERROR” si falla el SP; 400 si payload inválido.
- Logs: JSON a stdout con nivel info, incluye path, method, status_code y request_id (uuid por request).
- OpenAPI accesible en /docs y /openapi.json.
- No almacenar secretos en el repo. Proveer `.env.example`.
- Licencia MIT. README con instrucciones de ejecución local y Docker.
- Tests: pytest para validar 401 sin token y 200 con token “dummy” (mockear acceso DB).

Criterios de aceptación:
- `POST /clock-in` ejecuta el SP y retorna `status` y `workOrderCollectionId` (resolviendo con un SELECT posterior por EmployeeFK+WorkOrderAssemblyNumber).
- `POST /clock-out` ejecuta el SP y retorna `status`.
- Rechazo con 401 al faltar Bearer.
- `make run` levanta el servidor.
