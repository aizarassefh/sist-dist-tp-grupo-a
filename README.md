# Museo Virtual Interactivo

TP de Web Services — Desarrollo de Software en Sistemas Distribuidos, UNLa.

Sistema de gestión para un museo virtual: catálogo de obras y reporte de
asistencia por **GraphQL**; eventos, inscripciones, filtros favoritos,
exportación a Excel y autenticación por **REST**, con JWT y permisos por rol
(visitante, curador y administrador).

## Hito 2: Arquitectura gRPC (NUEVO)

El proyecto ahora incluye una arquitectura de microservicios con **gRPC**:

- **4 Nuevas Funcionalidades**: Recorridos Guiados, Comparación de Obras, Recomendaciones Personalizadas, Exploración del Catálogo
- **Mezcla de Tecnologías**: Java (Spring Boot) + TypeScript (NestJS)
- **Comunicación**: gRPC para servicios internos, REST para el frontend
- **Arquitectura**: Spring Cloud Gateway como orquestador

📖 **Ver documentación completa del Hito 2**: [README-GRPC.md](./README-GRPC.md)

### Nueva Estructura del Proyecto

```text
api-rest-graphql/        Spring Boot + gRPC Server (puerto 9090) - Hito 1 compatible
├── src/main/java/
│   └── com/unla/museo/
│       ├── catalogo/grpc/        # Catálogo via gRPC
│       ├── usuarios/grpc/        # Usuarios via gRPC
│       ├── eventos/grpc/         # Eventos via gRPC
│       ├── recorridos/grpc/      # ✨ Recorridos (Hito 2)
│       ├── comparacion/grpc/     # ✨ Comparación (Hito 2)
│       ├── recomendaciones/grpc/ # ✨ Recomendaciones (Hito 2)
│       └── exploracion/grpc/     # ✨ Exploración (Hito 2)
│
nestjs-service/            NestJS + gRPC (puerto 9091) - Tecnología diferente ✨
├── src/
│   ├── controllers/      # Recomendaciones & Usuarios
│   └── services/         # Lógica de negocio
│
gateway/                 Spring Cloud Gateway (puerto 8080) ✨
├── src/main/java/
│   └── com/unla/museo/gateway/
│       └── controller/
│           ├── RecorridosController.java
│           ├── ComparacionController.java
│           ├── RecomendacionesController.java
│           └── ExploracionController.java
│
proto/                   Contratos gRPC compartidos ✨
├── catalogo.proto
├── usuarios.proto
├── eventos.proto
├── recorridos.proto
├── comparacion.proto
├── recomendaciones.proto
├── exploracion.proto
├── gateway.proto
└── common.proto
│
frontend/                React + Vite (sin cambios al API existente del Hito 1)
docs/                    Documentación
│
docker-compose.yml       Orquestación de todos los servicios
```

### Nuevos Endpoints del Hito 2 (via Gateway)

| Funcionalidad | Endpoint | Descripción |
|--------------|----------|-------------|
| **Recorridos** | `POST /api/v2/recorridos/generar` | Genera recorrido personalizado |
| **Comparación** | `GET /api/v2/comparar` | Compara dos obras |
| **Recomendaciones** | `GET /api/v2/recomendaciones/personalizadas` | Sugerencias personalizadas |
| **Exploración** | `POST /api/v2/explorar/espacio` | Vista espacial del catálogo |

### Levantar todo el Hito 2 (Recomendado)

```bash
# Docker Compose - levanta todos los servicios
docker compose up -d

# URLs finales:
# - Frontend:    http://localhost:5173
# - Gateway:     http://localhost:8080 (punto único de entrada)
# - Spring Boot: http://localhost:8000 (API original Hito 1)
# - Swagger:     http://localhost:8000/swagger-ui.html
# - GraphiQL:    http://localhost:8000/graphiql
```

---

## Estructura Original (Hito 1)

```text
backend/     Spring Boot: API REST, API GraphQL y seguridad
frontend/    React + Vite: las pantallas
docs/        contrato de la API REST, colección de Postman y plan
docker-compose.yml   MariaDB para desarrollo
```

## Cómo levantarlo

Hace falta **Docker Desktop**, **JDK 21 o superior** y **Node 20.19+ o 22.12+**.

### Opción 1: Docker Compose (todo automático)

```bash
# Desde la raíz del proyecto
docker compose up -d
```

Esto levantará:
- MariaDB (base de datos)
- Spring Boot API (REST + gRPC en puerto 9090)
- NestJS Service (gRPC en puerto 9091)
- Spring Gateway (puerto 8080)
- Frontend React (puerto 5173)

### Opción 2: Desarrollo local (paso a paso)

1. **Base de datos**, desde la raíz del repo:

   ```bash
   docker compose up mariadb -d
   ```

   **Sin Docker** (por ejemplo, si WSL no funciona en la PC): instalar
   MariaDB 11.4 desde [mariadb.org](https://mariadb.org/download/), o usar
   su ZIP portable, y crear la base y el usuario que espera el backend:

   ```sql
   CREATE DATABASE museodb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'museo'@'localhost' IDENTIFIED BY 'museo';
   GRANT ALL PRIVILEGES ON museodb.* TO 'museo'@'localhost';
   ```

   **Si ya tenías una base de una versión anterior** del proyecto, borrala y
   creala de nuevo: el modelo de eventos cambió y Hibernate no convierte la
   tabla vieja. Con Docker: `docker compose down -v` y `docker compose up -d`.
   Sin Docker: `DROP DATABASE museodb;` y volver a correr el `CREATE DATABASE`
   de arriba (se pierden solo datos de prueba).

2. **Backend Spring Boot + gRPC**, en otra terminal:

   ```bash
   cd api-rest-graphql
   ./mvnw spring-boot:run
   ```

   En Windows con cmd o PowerShell: `mvnw.cmd spring-boot:run`. Queda en
   `http://localhost:8000`. Al arrancar crea las tablas y carga los datos de
   ejemplo.

   **Puerto gRPC**: También escucha en `localhost:9090` para comunicación
   interna entre servicios.

3. **Microservicio NestJS** (opcional para Hito 2):

   ```bash
   cd nestjs-service
   npm install
   npm run start:dev
   ```

   Escucha en `localhost:9091` (gRPC).

4. **Gateway** (opcional para Hito 2):

   ```bash
   cd gateway
   ./mvnw spring-boot:run
   ```

   Queda en `http://localhost:8080`.

5. **Frontend**:

   ```bash
   cd frontend
   npm ci
   npm run dev
   ```

   Queda en `http://localhost:5173`.

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

## Documentación interactiva de las APIs

- **ReST (Swagger)**: `http://localhost:8000/swagger-ui.html` o via Gateway `http://localhost:8080/swagger-ui.html`
- **GraphQL (GraphiQL)**: `http://localhost:8000/graphiql`

Para probar rutas protegidas, iniciar sesión con `POST /api/auth/login`,
copiar el `accessToken` y pegarlo en **Authorize** (Swagger) o Headers (GraphiQL).

## Tests

```bash
cd api-rest-graphql
./mvnw test
```

Usan una base H2 en memoria: no necesitan Docker ni MariaDB.

## URLs de Servicios

| Servicio | URL Externa | Puerto Interno |
|----------|-------------|----------------|
| Frontend | http://localhost:5173 | 5173 |
| API Gateway | http://localhost:8080 | 8080 |
| Spring Boot API | http://localhost:8000 | 8000 |
| gRPC Spring Boot | - (interno) | 9090 |
| gRPC NestJS | - (interno) | 9091 |
| MariaDB | localhost:3306 | 3306 |

## Arquitectura de Comunicación

```
Frontend React
      │ HTTP REST
      ▼
┌─────────────────┐     ┌─────────────────┐
│  API Gateway    │────▶│  Spring Boot    │
│   (Puerto 8080) │ gRPC │   (Puerto 8000) │
│                 │     │                 │
│  REST Publico   │     │  @GrpcService   │
│  /api/v2/**     │     │     + gRPC      │
└─────────────────┘     └─────────────────┘
        │                      │
        │ gRPC                 │ gRPC
        ▼                      ▼
┌────────────────────────┐  ┌─────────────────┐
│  Otros servicios gRPC  │  │  NestJS Service │
│  (Extensibles)         │  │  (Puerto 9091)  │
└────────────────────────┘  │  TypeScript     │
                          │  Recomendaciones│
                          └─────────────────┘
```

## Licencia

Proyecto académico UNLa - Sistemas Distribuidos
