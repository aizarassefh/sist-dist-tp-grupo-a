# Museo Virtual - Hito 2: Arquitectura gRPC

## Resumen Ejecutivo

Este proyecto implementa una arquitectura de microservicios distribuidos utilizando **gRPC** como protocolo de comunicación entre servicios, manteniendo compatibilidad con las APIs REST/GraphQL del Hito 1.

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTE WEB                                    │
│                         React 19 + Vite 8                                   │
│                    http://localhost:5173                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼ HTTP/REST
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SPRING CLOUD GATEWAY                                │
│                Puerto: 8080 | Orquestador gRPC                              │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │  ORQUESTACIÓN gRPC → Conversión REST a gRPC                         │ │
│  │  • Recorridos:     /api/v2/recorridos/**                             │ │
│  │  • Comparación:    /api/v2/comparar/**                               │ │
│  │  • Recomens:      /api/v2/recomendaciones/**                         │ │
│  │  • Exploración:    /api/v2/explorar/**                               │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
              ▼ gRPC                   ▼ gRPC                   ▼ gRPC
┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
│  SPRING BOOT API    │  │  NESTJS SERVICE     │  │  Otros Servicios    │
│  (Hito 1 + gRPC)    │  │  (Hito 2)           │  │  (Extensible)       │
│                     │  │                     │  │                     │
│  • Puerto REST:8000  │  │  • Puerto gRPC:9091 │  │                     │
│  • Puerto gRPC:9090 │  │                     │  │                     │
│                     │  │  Servicios:         │  │                     │
│  Servicios gRPC:    │  │  • Recomendaciones  │  │                     │
│  • Catalogo         │  │  • Usuarios         │  │                     │
│  • Eventos          │  │                     │  │                     │
│  • Usuarios         │  │  Tecnología: TS    │  │                     │
│  • Recorridos ✨    │  │                     │  │                     │
│  • Comparación ✨   │  │                     │  │                     │
│  • Exploración ✨   │  │                     │  │                     │
│                     │  │                     │  │                     │
│  Tecnología: Java 21│  │                     │  │                     │
└─────────────────────┘  └─────────────────────┘  └─────────────────────┘
         │                        │
         └────────────┐───────────┘
                      │
         ┌────────────┴────────────┐
         ▼                       ▼
┌─────────────────────┐  ┌─────────────────────┐
│     MARIADB         │  │  In-Memory/Cache    │
│   (Base de datos)   │  │  (Usuarios NestJS)  │
└─────────────────────┘  └─────────────────────┘
```

## Estructura del Proyecto

```
/
├── api-rest-graphql/              # Backend Spring Boot (Hito 1 + gRPC)
│   ├── src/main/java/
│   │   └── com/unla/museo/
│   │       ├── catalogo/grpc/     # Servicio gRPC Catálogo
│   │       ├── usuarios/grpc/     # Servicio gRPC Usuarios
│   │       ├── eventos/grpc/      # Servicio gRPC Eventos
│   │       ├── recorridos/grpc/ ✨ # Servicio Recorridos (Hito 2)
│   │       ├── comparacion/grpc/ ✨ # Servicio Comparación (Hito 2)
│   │       ├── recomendaciones/grpc/ ✨ # Servicio Recomendaciones (Hito 2)
│   │       └── exploracion/grpc/  ✨ # Servicio Exploración (Hito 2)
│   ├── Dockerfile
│   └── pom.xml (configurado con gRPC)
│
├── nestjs-service/                # Microservicio NestJS (Hito 2)
│   ├── src/
│   │   ├── controllers/
│   │   │   ├── recomendaciones.controller.ts
│   │   │   └── usuarios.controller.ts
│   │   └── services/
│   │       ├── recomendaciones.service.ts
│   │       └── usuarios.service.ts
│   ├── Dockerfile
│   └── package.json
│
├── gateway/                       # Spring Cloud Gateway
│   ├── src/main/java/
│   │   └── com/unla/museo/gateway/
│   │       └── controller/
│   │           ├── RecorridosController.java  ✨
│   │           ├── ComparacionController.java ✨
│   │           ├── RecomendacionesController.java ✨
│   │           └── ExploracionController.java  ✨
│   ├── Dockerfile
│   └── application.yml
│
├── proto/                         # Contratos gRPC compartidos
│   ├── catalogo.proto
│   ├── usuarios.proto
│   ├── eventos.proto
│   ├── recorridos.proto      ✨
│   ├── comparacion.proto     ✨
│   ├── recomendaciones.proto ✨
│   ├── exploracion.proto     ✨
│   ├── gateway.proto
│   └── common.proto
│
├── frontend/                      # Frontend React (sin cambios)
│   └── Dockerfile
│
└── docker-compose.yml             # Orquestación completa
```

## Funcionalidades del Hito 2 (End Points)

### 1. Recorridos Guiados Personalizados

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v2/recorridos/generar` | Genera un recorrido personalizado |
| `GET` | `/api/v2/recorridos` | Lista los recorridos del usuario |
| `GET` | `/api/v2/recorridos/{id}` | Obtiene un recorrido específico |
| `DELETE` | `/api/v2/recorridos/{id}` | Elimina un recorrido |
| `POST` | `/api/v2/recorridos/{id}/compartir` | Comparte un recorrido |

**Ejemplo de generación de recorrido:**
```json
POST /api/v2/recorridos/generar
{
  "usuarioId": 1,
  "criterios": {
    "tipo": "POR_ESTILO",
    "estilo": "Impresionismo",
    "duracionMinutos": 60
  },
  "nombrePersonalizado": "Mi recorrido por el Impresionismo",
  "guardarAutomaticamente": true
}
```

### 2. Comparación de Obras

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/v2/comparar?obraPrincipalId=1&obraSecundariaId=2` | Compara dos obras |
| `GET` | `/api/v2/comparar/similares?obraId=1&criterio=ESTILO` | Busca obras similares |
| `GET` | `/api/v2/comparar/estilo/{estilo}/analizar` | Analiza características del estilo |
| `POST` | `/api/v2/comparar/coleccion` | Compara con toda la colección |

**Ejemplo de comparación:**
```json
Response:
{
  "obraPrincipal": { "id": 1, "titulo": "Noche estrellada" },
  "obraSecundaria": { "id": 5, "titulo": "Los girasoles" },
  "tecnica": { "similitud": 100, "observacion": "Misma técnica: Óleo" },
  "estilo": { "similitud": 95, "observacion": "Post-Impresionismo" },
  "indiceSimilitud": 96.25,
  "analisis": "Ambas obras de Van Gogh..."
}
```

### 3. Recomendaciones Personalizadas

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/v2/recomendaciones/personalizadas` | Recomendaciones basadas en preferencias |
| `GET` | `/api/v2/recomendaciones/contextuales` | Sugerencias según obra actual |
| `GET` | `/api/v2/recomendaciones/populares` | Obras más populares |
| `GET` | `/api/v2/recomendaciones/tendencias` | Obras en tendencia |
| `GET` | `/api/v2/recomendaciones/{usuarioId}/historial` | Basado en historial |

**Ejemplo de recomendaciones:**
```json
GET /api/v2/recomendaciones/personalizadas?cantidad=5

Response:
{
  "recomendaciones": [
    {
      "obra": { "id": 3, "titulo": "Sol naciente" },
      "score": 94.5,
      "razon": "Basado en tu interés en Impresionismo",
      "tipoRecomendacion": "PERSONAL"
    }
  ]
}
```

### 4. Exploración del Catálogo

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/v2/explorar/espacio` | Visualización tipo "espacio" |
| `POST` | `/api/v2/explorar/descubrir` | Descubrimiento con filtros |
| `GET` | `/api/v2/explorar/cronologia` | Vista cronológica |
| `GET` | `/api/v2/explorar/conexiones/{obraId}` | Red de conexiones |
| `GET` | `/api/v2/explorar/estadisticas` | Estadísticas del catálogo |
| `GET` | `/api/v2/explorar/buscar` | Búsqueda global |

**Ejemplo de exploración espacial:**
```json
POST /api/v2/explorar/espacio
{
  "modo": "ESTILOS",
  "usuarioId": 1
}

Response:
{
  "espacio": {
    "clusters": [
      { "nombre": "Impresionismo", "totalObras": 45, "x": 25.5, "y": 30.2 },
      { "nombre": "Surrealismo", "totalObras": 28, "x": 75.3, "y": 45.8 }
    ]
  }
}
```

## Configuración de gRPC

### Puertos y Endpoints

| Servicio | Tecnología | Puerto gRPC | Puerto HTTP |
|----------|------------|-------------|-------------|
| Spring Boot API | Java 21 | 9090 | 8000 |
| NestJS Service | TypeScript | 9091 | - |
| Spring Gateway | Java 21 | - | 8080 |
| Frontend | React | - | 5173 |

### Contratos Proto

Los archivos `.proto` definen los contratos en `/proto/`:

- `catalogo.proto` - Catálogo de obras y artistas
- `usuarios.proto` - Gestión de usuarios y preferencias
- `eventos.proto` - Eventos e inscripciones
- `recorridos.proto` - Recorridos personalizados
- `comparacion.proto` - Comparación de obras
- `recomendaciones.proto` - Motor de recomendaciones
- `exploracion.proto` - Exploración del catálogo
- `common.proto` - Mensajes comunes
- `gateway.proto` - Agregación

## Ejecución

### Opción 1: Docker Compose (Recomendado)

```bash
# Levantar toda la arquitectura
docker compose up -d

# Ver logs
docker compose logs -f

# Reconstruir todo
docker compose down -v && docker compose up -d --build
```

### Opción 2: Desarrollo Local

```bash
# Terminal 1: Base de datos
docker compose up mariadb -d

# Terminal 2: Spring Boot API (con gRPC)
cd api-rest-graphql
./mvnw spring-boot:run

# Terminal 3: NestJS Service
cd nestjs-service
npm install
npm run start:dev

# Terminal 4: Spring Gateway
cd gateway
./mvnw spring-boot:run

# Terminal 5: Frontend
cd frontend
npm install
npm run dev
```

## URLs Accesibles

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Spring Boot API | http://localhost:8000 |
| gRPC Spring Boot | localhost:9090 |
| gRPC NestJS | localhost:9091 |
| Swagger UI | http://localhost:8000/swagger-ui.html |
| GraphiQL | http://localhost:8000/graphiql |
| MariaDB | localhost:3306 |

## Diagrama de Flujo de la Arquitectura

```
┌─────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   Frontend  │────────>│  API Gateway    │────────>│ Backend Spring  │
│   (React)   │         │   (Orquestador) │         │ (REST/GraphQL)  │
│             │         │                 │         │   Puerto:8000   │
└─────────────┘         │   Puerto:8080   │         └─────────────────┘
                      │                 │                   │
                      │  ┌───────────┐  │                   │ gRPC
                      │  │ REST→gRPC │  │                   ▼
                      │  │  Mapper   │  │         ┌─────────────────┐
                      │  └───────────┘  │         │  gRPC Server    │
                      │                 │         │   Puerto:9090   │
                      └────────┬────────┘         │                 │
                               │                  │ • Catalogo      │
                               │                  │ • Usuarios      │
                               │                  │ • Eventos       │
                               │                  │ • Recorridos    │
                               │ gRPC             │ • Comparación   │
                               ▼                  │ • Recomendaciones│
                      ┌─────────────────┐        │ • Exploración   │
                      │  NestJS Service │        └─────────────────┘
                      │   Puerto:9091   │
                      │                 │
                      │ • Recomendaciones│
                      │ • Usuarios      │
                      │                 │
                      └─────────────────┘

                         Mix Tecnológico:
                      Java ☕ + TypeScript 🔷
```

## Decisiones de Arquitectura

1. **gRPC para comunicación inter-servicios**: Protocolo binario eficiente con contratos tipados vía Protocol Buffers.

2. **Spring Cloud Gateway como orquestador**: Punto único de entrada que convierte REST a gRPC internamente.

3. **NestJS para Hito 2**: Tecnología diferente (TypeScript) que demuestra interoperabilidad.

4. **Mantenimiento de APIs Hito 1**: REST y GraphQL siguen disponibles en puerto 8000.

5. **Contratos Proto compartidos**: Los archivos en `/proto/` son usados tanto por Java como TypeScript.

## Licencia

Proyecto académico UNLa - Sistemas Distribuidos
