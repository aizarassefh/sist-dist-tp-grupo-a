# Museo Virtual Interactivo

TP de Desarrollo de Software en Sistemas Distribuidos, UNLa. Grupo A.

- **Entrega 1 (Web Services):** catálogo de obras y reporte de asistencia por
  **GraphQL**; eventos, inscripciones, filtros favoritos, exportación a Excel
  y autenticación por **REST**, con JWT y permisos por rol (visitante, curador
  y administrador).
- **Entrega 2 (RPC):** recorridos personalizados, comparación de obras,
  recomendaciones y exploración por movimiento, que entran por un **API
  Gateway** y se resuelven con llamadas **gRPC** entre servicios.
- **Entrega 3 (mensajería):** las altas, modificaciones y cancelaciones de
  eventos publican novedades en **Kafka**; dos consumidores independientes
  generan las notificaciones de los visitantes y el historial de actividad.

## Estructura

```text
api-rest-graphql/   Backend Spring Boot. Un mismo jar con tres perfiles:
                    - por defecto: REST + GraphQL (:8000), gRPC (:9090) y
                      productor de novedades (outbox + publicador)
                    - consumidor-notificaciones / consumidor-auditoria:
                      procesos sin web que consumen de Kafka
nestjs-service/     Motor de experiencias en NestJS/TypeScript (gRPC :9091)
gateway/            API Gateway en Java/WebFlux (:8080)
proto/              Contratos gRPC compartidos por Java y TypeScript
frontend/           React + TypeScript + Vite (:5173)
kafka/              Scripts para levantar Kafka sin Docker
pruebas/            Pruebas de punta a punta contra todo levantado
docs/               Contratos REST y de mensajería, y colección de Postman
```

```text
             ┌─ REST + GraphQL ─▶ backend :8000 ── outbox ─▶ Kafka :9092 ─┬─▶ consumidor de notificaciones ─┐
React :5173 ─┤                      │  ▲ gRPC :9090                       └─▶ consumidor de auditoría ──────┤
             └─ REST /api/v2 ─▶ gateway :8080 ─▶ motor NestJS :9091                                         │
                                    │                                             MariaDB ◀──────────────────┘
```

## Cómo levantarlo

Hace falta **JDK 21**, **Node 20.19+ o 22.12+**, **MariaDB 11.4** y **Kafka
3.7**. MariaDB y Kafka pueden ir con Docker o sin él. Cada componente va en su
propia terminal, en este orden.

### 1. Base de datos

Con Docker, desde la raíz: `docker compose up -d mariadb`.

Sin Docker: instalar MariaDB 11.4 desde
[mariadb.org](https://mariadb.org/download/) (sirve el ZIP portable) y crear
la base y el usuario que espera el backend:

```sql
CREATE DATABASE museodb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'museo'@'localhost' IDENTIFIED BY 'museo';
GRANT ALL PRIVILEGES ON museodb.* TO 'museo'@'localhost';
```

Una base de las entregas anteriores sirve: al arrancar, el backend agrega las
columnas y tablas nuevas y completa los datos de ejemplo que falten. Si la base
corrió versiones intermedias de las ramas, pueden quedar tablas sin uso
(`preferencias_usuario`, `interacciones_usuario`, `NOVEDADES`,
`HISTORIAL_EVENTOS`): no molestan y se pueden borrar.

### 2. Kafka

**Sin Docker (Windows, probado):** bajar
[`kafka_2.13-3.7.0.tgz`](https://archive.apache.org/dist/kafka/3.7.0/kafka_2.13-3.7.0.tgz)
y descomprimirlo dentro de `kafka/`, de modo que quede `kafka/kafka_2.13-3.7.0/`
(está en el `.gitignore`). Solo necesita Java. Después:

```bash
kafka\levantar-kafka.cmd
```

En otra terminal, una sola vez:

```bash
kafka\crear-topics.cmd
```

Queda en `localhost:9092`, en modo KRaft (sin Zookeeper). La primera vez
formatea `kafka/datos`. El script llama a Java directo porque los `.bat` de
Kafka arman una línea más larga de lo que acepta `cmd`. `kafka/server.properties`
desactiva el borrado de topics, la retención y la compactación: en Windows
esas operaciones hacen que el broker se apague (lo comprobamos borrando un
topic). Para empezar de cero, apagar Kafka y borrar `kafka/datos`.

**Con Docker:** `docker compose up -d kafka`. Crea los topics solo al publicar,
con 3 particiones. Esta variante no la pudimos probar.

### 3. Backend y consumidores

```bash
cd api-rest-graphql
./mvnw package -DskipTests
```

Después, tres procesos con el mismo jar. Primero el principal, que crea el
esquema de la base:

```bash
java -jar target/museo-0.0.1-SNAPSHOT.jar --spring.profiles.active=default
```

```bash
java -jar target/museo-0.0.1-SNAPSHOT.jar --spring.profiles.active=consumidor-notificaciones
```

```bash
java -jar target/museo-0.0.1-SNAPSHOT.jar --spring.profiles.active=consumidor-auditoria
```

En Windows con cmd o PowerShell: `mvnw.cmd`. El principal queda en
`http://localhost:8000` (REST y GraphQL) y `localhost:9090` (gRPC). Los
consumidores no abren puertos: solo leen de Kafka y escriben en la base.

Variables: `DB_URL`, `DB_USUARIO`, `DB_CLAVE` (por defecto
`jdbc:mariadb://localhost:3306/museodb` con `museo`/`museo`),
`KAFKA_BOOTSTRAP_SERVERS` (`localhost:9092`), `APP_KAFKA_ENABLED` (`true`;
en `false` el backend no publica, para trabajar sin Kafka) y, para el
consumidor de notificaciones, `BACKEND_GRPC` (`localhost:9090`).

Sin `--spring.profiles.active` se activa el perfil `local`, que apunta a
`root`/`root` en el 3306; usarlo solo si la base local está configurada así.

### 4. Motor NestJS

```bash
cd nestjs-service
npm ci
npm run build
npm start
```

Escucha gRPC en `localhost:9091`. Lee los contratos de `../proto`.

### 5. API Gateway

```bash
cd gateway
./mvnw spring-boot:run
```

Queda en `http://localhost:8080`. Variables: `BACKEND_GRPC`
(`localhost:9090`), `MOTOR_GRPC` (`localhost:9091`), `JWT_SECRET` (la misma
que el backend), `FRONTEND_URL` (para CORS).

Si se reinicia el backend o el motor con el gateway o el consumidor de
notificaciones levantados, sus clientes gRPC pueden fallar durante unos
segundos: reintentan la conexión esperando cada vez más entre intentos.

### 6. Frontend

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
- **Entrega 3:** topics, formato de las novedades, consumidores y endpoints de
  notificaciones, historial y mensajería en
  [`docs/contrato-mensajeria.md`](docs/contrato-mensajeria.md).

## Pruebas

Tests de cada proyecto (no necesitan base ni broker: el backend usa H2 y un
Kafka embebido, y los servicios gRPC se prueban con servidores en memoria):

```bash
cd api-rest-graphql && ./mvnw test
cd gateway && ./mvnw test
cd nestjs-service && npm test
```

Pruebas de punta a punta, con todo levantado:

```bash
node pruebas/e2e.mjs
node pruebas/e2e-kafka.mjs
```

La primera recorre las Entregas 1 y 2. La segunda crea, modifica y cancela un
evento y espera la notificación y el historial que generan los consumidores;
además inyecta en Kafka un mensaje duplicado y uno inválido, y verifica la
consistencia entre lo publicado y lo procesado.

Para ver el tratamiento de errores a mano, `kafka\publicar-mensaje-invalido.cmd`
publica un mensaje sin `idMensaje`: los consumidores lo rechazan, va al DLT y
aparece en la pantalla **Mensajería**.
