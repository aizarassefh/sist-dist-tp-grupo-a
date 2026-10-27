# Frontend — Museo Virtual

React 19 + Vite. Consume la API REST (`/api/...`) y la API GraphQL
(`/graphql`) del backend Spring Boot.

## Cómo levantarlo

Requiere Node 20.19+ o 22.12+ (lo exige Vite 8) y el backend corriendo (por defecto en el
puerto 8000).

```bash
npm ci
npm run dev
```

Queda en `http://localhost:5173`. El backend solo acepta CORS desde ese
origen, así que no cambies el puerto.

Si el backend corre en otra dirección, se indica con una variable de
entorno, sin tocar código:

```bash
VITE_API_URL=http://localhost:8080 npm run dev
```

Las funcionalidades de la Entrega 2 (recorridos, comparación,
recomendaciones y exploración) no van al backend sino al API Gateway, que por
defecto está en `http://localhost:8080`. Se cambia con otra variable:

```bash
VITE_GATEWAY_URL=http://localhost:8080 npm run dev
```

El contrato del gateway está en `docs/contrato-api-gateway.md`.

Los usuarios de prueba (visitante, curador y administrador) los crea el
backend al arrancar: están en `DataInitializer.java`.

## Pantallas

| Ruta | Quién | API |
|---|---|---|
| `/` (sin sesión) | todos | Login — `POST /api/auth/login` |
| `/registro` | todos | Registro, entra como VISITANTE — `POST /api/auth/register` |
| `/coleccion`, `/obra/:id` | logueados | Catálogo con filtros — GraphQL `obras` |
| `/eventos`, `/eventos/:id` | logueados | Listado con filtros, detalle, inscripción — REST |
| `/eventos/nuevo`, `/eventos/:id/editar` | curador, admin | Alta y edición de eventos — REST |
| `/favoritos` | logueados | Filtros favoritos: listar, aplicar, editar, borrar — REST |
| `/reporte` | curador, admin | Reporte de asistencia (GraphQL) y exportación a Excel (REST) |
| `/recorridos` | visitante | Recorrido por las salas según tiempo, categorías y preferencias — gateway `POST /api/v2/recorridos` |
| `/comparar` | logueados | Comparación de 2 a 6 obras, también desde `?obra=ID` — gateway `POST /api/v2/comparaciones` |
| `/recomendaciones` | visitante | Editor de preferencias y obras recomendadas — gateway `/api/v2/preferencias`, `/api/v2/recomendaciones` |
| `/explorar`, `/explorar/:movimiento` | logueados | Obras, artistas y eventos de un movimiento — gateway `/api/v2/exploraciones/{movimiento}` |

En `/obra/:id`, el visitante registra una vista al abrir la obra y puede
marcar "Me interesa" (gateway `POST /api/v2/interacciones`). Todas las
pantallas del gateway muestran al pie "Cómo se resolvió", con las llamadas
gRPC que hizo el gateway y su duración (`trazas` de cada respuesta).

## Estructura

```text
src/
├── api/cliente.js     # única salida al backend REST: URL, token, errores
├── api/gateway.js     # salida al API Gateway (reusa cliente.js)
├── api/useCarga.js    # hook para cargar datos con estado de carga y error
├── main.jsx           # Apollo (GraphQL) con el mismo token
├── App.jsx            # sesión, navegación y rutas
├── pages/             # una página por ruta
├── components/        # piezas reutilizadas por las páginas
├── graphql/queries.js # consultas GraphQL
└── roles.js, tiposEvento.js, fechas.js, filtrosEventos.js
```

El control de acceso real lo hace el backend. El frontend oculta lo que un
rol no puede usar y, si el token vence, vuelve al login.

## Qué está probado y contra qué

Todas las pantallas se probaron en el navegador contra el backend real con
MariaDB, con los tres roles: login, registro, sesión vencida, catálogo y
ficha de obra, agenda de eventos, inscripción y cancelación, alta y edición
de eventos, filtros favoritos, reporte y descarga del Excel.

Lo agregado en la Entrega 2 (pantallas del gateway, vista e interés en la
ficha de obra) **no se probó contra un gateway real**: se verificó que
compila y pasa el lint, pero no se ejecutó contra los servicios.

No hay tests automatizados del frontend; los del backend están en
`backend/src/test`.
