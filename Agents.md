# Agents.md

## Project overview

This repository contains **Museo Virtual Interactivo**, a UNLa distributed-systems
course project. It has two related deliveries:

- **Delivery 1:** Spring Boot backend with REST, GraphQL, JWT authentication,
  events, registrations, favorites, attendance reports, and Excel export.
- **Delivery 2:** An API Gateway and a NestJS gRPC experience engine for
  personalized tours, artwork comparison, recommendations, and movement-based
  exploration.

The repository's current implementation is described by the root `README.md`
and the Delivery 2 contracts. `docs/PLAN.md` is valuable design history, but
some of its early assumptions (PostgreSQL, two original Spring servers, and a
not-yet-implemented status) describe the previous proposal rather than the
current codebase. Do not silently reintroduce those assumptions.

## Repository map

| Directory | Responsibility | Runtime |
|---|---|---|
| `api-rest-graphql/` | Spring Boot REST + GraphQL backend and gRPC servers for `CatalogoService`, `EventosService`, and `UsuariosService` | HTTP `8000`, gRPC `9090` |
| `gateway/` | Spring WebFlux API Gateway; validates JWT, orchestrates gRPC calls, and contains no database or business rules | HTTP `8080` |
| `nestjs-service/` | NestJS/TypeScript `MotorService`; pure calculations for tours, recommendations, and comparisons | gRPC `9091` |
| `proto/` | Shared gRPC contracts used by Java and TypeScript | — |
| `frontend/` | React 19 + Vite client for REST, GraphQL, and gateway features | HTTP `5173` |
| `pruebas/e2e.mjs` | End-to-end test against the complete running system | — |
| `docs/` | API contracts, plans, delivery material, and supporting documentation | — |

The main runtime flow is:

```text
React :5173
  ├─ REST + GraphQL ──> backend :8000 ──> MariaDB
  └─ REST /api/v2 ────> gateway :8080
                          ├─ gRPC ──> backend :9090
                          └─ gRPC ──> motor :9091
```

## Source of truth and contracts

- Read `README.md` first for current setup, ports, environment variables,
  test commands, and test users.
- Read `docs/contrato-api-eventos.md` before changing event, registration,
  favorite-filter, or Excel behavior. The frontend consumes this contract;
  backend changes can require coordinated frontend changes.
- Read `docs/contrato-api-gateway.md` before changing any `/api/v2` endpoint,
  orchestration order, role requirement, trace format, or gRPC error mapping.
- Treat files in `proto/` as shared public contracts. Changes require updating
  both Java and TypeScript consumers and their tests.
- `frontend/README.md` documents routes and which API each screen uses.
- `nestjs-service/README.md` documents the engine's boundaries: it has no
  database and must not call other services.
- `docs/PLAN.md` records rationale, proposed rules, acceptance criteria, and
  the original architecture. Use it for design context and constraints, but
  verify implementation details against source and the current contracts.

## Local setup

Prerequisites:

- JDK 21
- Node 20.19+ or 22.12+
- MariaDB 11.4, either installed or via Docker

Start components in this order, each in its own terminal:

```bash
# From repository root
docker compose up -d

# Terminal 1
cd api-rest-graphql
./mvnw spring-boot:run "-Dspring-boot.run.profiles=default"

# Terminal 2
cd nestjs-service
npm ci
npm run build
npm start

# Terminal 3
cd gateway
./mvnw spring-boot:run

# Terminal 4
cd frontend
npm ci
npm run dev
```

On Windows, use `mvnw.cmd`. The backend defaults to MariaDB at
`jdbc:mariadb://localhost:3306/museodb` with `museo`/`museo`; configure
`DB_URL`, `DB_USUARIO`, and `DB_CLAVE` when needed. The gateway uses
`BACKEND_GRPC`, `MOTOR_GRPC`, `JWT_SECRET`, and `FRONTEND_URL`. The frontend
uses `VITE_API_URL` and `VITE_GATEWAY_URL`.

Do not commit real credentials, JWT secrets, or `.env` files. The repository is
public; use example values only in example configuration.

## Development conventions

### Backend and API behavior

- Preserve JWT behavior across backend and gateway. The backend issues the JWT;
  protected routes use `Authorization: Bearer <token>`.
- Roles are `VISITANTE`, `CURADOR`, and `ADMINISTRADOR`. Public registration
  always creates a visitor; clients must not be able to choose a privileged
  role.
- Keep HTTP status semantics stable: `400` invalid input, `401` missing or
  invalid token, `403` insufficient role, `404` missing/resource-private
  resource, `409` business conflict, and `503` unavailable gRPC dependency.
- Keep event type values exactly `VISITA_GUIADA`, `TALLER`, and `CHARLA`.
- Event/favorite dates and JSON names must remain compatible with
  `docs/contrato-api-eventos.md` and the frontend.
- The gateway validates requests and coordinates calls; business decisions
  belong in the backend or NestJS motor as appropriate. Do not add a database
  or business rules to the gateway.
- Include `trazas` in successful Delivery 2 responses and preserve the service,
  method, and duration information used by the UI and video demonstration.
- In the NestJS motor, keep domain algorithms as pure functions under
  `nestjs-service/src/motor/`; controllers should delegate to them.
- When changing a gRPC message or method, update the `.proto`, generated/runtime
  consumers, serialization mapping, and tests together.

### Frontend behavior

- Keep backend and gateway URLs configurable through Vite environment
  variables; do not hard-code deployment-specific URLs.
- The frontend hides unavailable role-based features, but the backend/gateway
  remains the authority for authorization.
- Reuse the existing API clients in `frontend/src/api/` rather than creating
  ad-hoc request handling in pages.
- Preserve the existing routes and API ownership documented in
  `frontend/README.md`.
- If the token expires or a protected request returns `401`, return the user to
  login as the current client expects.

### Documentation and assets

- Keep API contracts synchronized with implementation and frontend usage.
- Do not present proposed or pending criteria from `docs/PLAN.md` as executed
  tests unless they have actually been run.
- Preserve attribution and licensing notes in
  `frontend/public/obras/FUENTES.md`; do not add artwork assets without source
  and rights information.

## Validation commands

Run the smallest relevant test suite after a change:

```bash
# Spring backend
cd api-rest-graphql && ./mvnw test

# Gateway
cd gateway && ./mvnw test

# NestJS motor
cd nestjs-service && npm test

# Full-system test, after starting the database, backend, motor, and gateway
node pruebas/e2e.mjs
```

The NestJS tests cover the pure algorithms and a gRPC integration path. The
repository documentation notes that there are no automated frontend tests;
frontend changes should at minimum pass the existing lint/build checks and be
verified against the real backend when the affected feature is available.

## Safe change workflow

1. Identify whether the change belongs to Delivery 1, Delivery 2, a shared
   proto contract, or the frontend.
2. Read the relevant contract and current implementation before editing.
3. Check `git status` first and do not overwrite unrelated local work.
4. Make the smallest compatible change; update docs/contracts when behavior
   changes.
5. Run the focused tests, then the end-to-end test if cross-service behavior
   changed.
6. Review `git diff` for accidental credentials, `.DS_Store` files, generated
   artifacts, or unrelated edits.

## Known caveats

- Restarting the backend or motor while the gateway is running can produce
  temporary `503` responses while the gRPC client retries with backoff.
- The current event contract says deleting an event also deletes its
  registrations, while the older plan proposed rejecting deletion when
  registrations exist. Follow the current implementation/contract unless the
  team explicitly changes and coordinates this behavior.
- The frontend documentation says Delivery 2 screens compile and pass lint but
  were not verified against a real gateway; do not claim full integration
  coverage without running the services.
- Test credentials are documented for local demonstrations only and must not
  be reused as production secrets.
