# Museo Virtual Interactivo

TP de Desarrollo de Software en Sistemas Distribuidos, UNLa. Grupo A.

- **Entrega 1 (Web Services):** catálogo de obras y reporte de asistencia por
  **GraphQL**; eventos, inscripciones, filtros favoritos, exportación a Excel
  y autenticación por **REST**, con JWT y permisos por rol (visitante, curador
  y administrador).
- **Entrega 2 (RPC):** recorridos personalizados, comparación de obras,
  recomendaciones y exploración por movimiento, que entran por un **API
  Gateway** y se resuelven con llamadas **gRPC** entre servicios.

El documento de la Entrega 2 (estrategia, diagramas, servicios, contratos,
modelo de datos, pruebas) está en
[`docs/entrega-2/Entrega-2-Grupo-A.pdf`](docs/entrega-2/Entrega-2-Grupo-A.pdf).

## Estructura

```text
api-rest-graphql/   Backend Spring Boot: REST + GraphQL (Entrega 1, :8000) y
                    servidor gRPC con CatalogoService, EventosService y
                    UsuariosService (:9090)
nestjs-service/     Motor de experiencias en NestJS/TypeScript: MotorService
                    por gRPC (:9091). Recorridos, recomendaciones y comparación
gateway/            API Gateway en Java/WebFlux (:8080): valida el JWT y
                    orquesta las llamadas gRPC. Sin base ni lógica de negocio
proto/              Contratos gRPC compartidos por Java y TypeScript
frontend/           React + Vite (:5173)
pruebas/e2e.mjs     Prueba de punta a punta contra todo levantado
docs/               Contratos REST, colección de Postman y documento de entrega
```

```text
             ┌─ REST + GraphQL (Entrega 1) ─▶ backend :8000 ──┐
React :5173 ─┤                                                 ├─ MariaDB
             └─ REST /api/v2 (Entrega 2) ─▶ gateway :8080      │
                                              │ gRPC           │
                          ┌───────────────────┴──────┐         │
                          ▼                          ▼         │
             backend :9090 (Catalogo,       motor NestJS :9091 │
             Eventos, Usuarios) ────────────────────────────────┘
```

## Cómo levantarlo

Hace falta **JDK 21**, **Node 20.19+ o 22.12+** y **MariaDB 11.4** (con
Docker o instalada). Cada componente va en su propia terminal, en este orden.

### 1. Base de datos

Con Docker, desde la raíz: `docker compose up -d`.

Sin Docker: instalar MariaDB 11.4 desde
[mariadb.org](https://mariadb.org/download/) (sirve el ZIP portable) y crear
la base y el usuario que espera el backend:

```sql
CREATE DATABASE museodb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'museo'@'localhost' IDENTIFIED BY 'museo';
GRANT ALL PRIVILEGES ON museodb.* TO 'museo'@'localhost';
```

Una base de la Entrega 1 sirve: al arrancar, el backend agrega las columnas y
tablas nuevas y completa los datos de ejemplo que falten. Si la base corrió la
primera versión de la rama gRPC, le quedan tablas sin uso
(`preferencias_usuario`, `interacciones_usuario` y las de sus listas): no
molestan y se pueden borrar.

### 2. Backend (REST + GraphQL + gRPC)

```bash
cd api-rest-graphql
./mvnw spring-boot:run "-Dspring-boot.run.profiles=default"
```

En Windows con cmd o PowerShell: `mvnw.cmd`. Queda en `http://localhost:8000`
(REST y GraphQL) y en `localhost:9090` (gRPC). La base se configura con
`DB_URL`, `DB_USUARIO` y `DB_CLAVE`; por defecto
`jdbc:mariadb://localhost:3306/museodb` con `museo`/`museo`.

El perfil `local` (el que se activa si no se indica otro) apunta a
`root`/`root` en el 3306; usarlo solo si la base local está configurada así.

### 3. Motor NestJS

```bash
cd nestjs-service
npm ci
npm run build
npm start
```

Escucha gRPC en `localhost:9091`. Lee los contratos de `../proto`.

### 4. API Gateway

```bash
cd gateway
./mvnw spring-boot:run
```

Queda en `http://localhost:8080`. Si se reinicia el backend o el motor con
el gateway levantado, el gateway puede responder 503 durante unos segundos:
el cliente gRPC reintenta la conexión esperando cada vez más entre intentos.
Variables: `BACKEND_GRPC`
(`localhost:9090`), `MOTOR_GRPC` (`localhost:9091`), `JWT_SECRET` (la misma
que el backend), `FRONTEND_URL` (para CORS).

### 5. Frontend

```bash
cd frontend
npm ci
npm run dev
```

Queda en `http://localhost:5173`. Usa `VITE_API_URL` (backend) y
`VITE_GATEWAY_URL` (gateway) si hay que cambiar las direcciones.

## Usuarios de prueba

Los crea el backend al arrancar:

| Email | Rol |
|---|---|
| `visitante@test.com` | VISITANTE |
| `curador@test.com` | CURADOR |
| `admin@test.com` | ADMINISTRADOR |

**Contraseña para todos**: `Aa@12345678`

Cualquiera puede registrarse desde la pantalla de login; el usuario nuevo
siempre queda como visitante.

## APIs

- **Entrega 1:** Swagger en `http://localhost:8000/swagger-ui.html`, GraphiQL
  en `http://localhost:8000/graphiql`, contrato de eventos en
  [`docs/contrato-api-eventos.md`](docs/contrato-api-eventos.md) y colección
  de Postman en `docs/postman/`.
- **Entrega 2:** contrato REST del gateway en
  [`docs/contrato-api-gateway.md`](docs/contrato-api-gateway.md) y contratos
  gRPC en [`proto/`](proto/).

## Pruebas

Tests de cada proyecto (no necesitan base ni otros servicios: el backend usa
H2 en memoria y los servicios gRPC se prueban con servidores en memoria):

```bash
cd api-rest-graphql && ./mvnw test
cd gateway && ./mvnw test
cd nestjs-service && npm test
```

Prueba de punta a punta, con todo levantado (pasos 1 a 4):

```bash
node pruebas/e2e.mjs
```

Hace login, recorre las cuatro funcionalidades nuevas a través del gateway
verificando qué servicios llamó, y comprueba que lo de la Entrega 1 sigue
respondiendo igual.
