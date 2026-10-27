# Contrato REST del API Gateway (Entrega 2)

El gateway (`gateway/`, puerto **8080**) es el único punto de entrada de las
funcionalidades de la Entrega 2. Recibe REST + JSON del front, valida el JWT y
resuelve cada operación llamando por gRPC a los servicios internos:

| Servicio | Proceso | Puerto gRPC | Contrato |
|---|---|---|---|
| CatalogoService | backend Java (`api-rest-graphql`) | 9090 | `proto/catalogo.proto` |
| EventosService | backend Java | 9090 | `proto/eventos.proto` |
| UsuariosService | backend Java | 9090 | `proto/usuarios.proto` |
| MotorService | NestJS (`nestjs-service`) | 9091 | `proto/motor.proto` |

El gateway **no accede a ninguna base** y no tiene reglas de negocio: decide a
qué servicios llamar, en qué orden, les pasa los datos de uno a otro y arma la
respuesta. Las funcionalidades de la Entrega 1 siguen en el backend (puerto
8000) sin cambios.

## Autenticación

Todas las rutas piden `Authorization: Bearer <token>`, el mismo JWT que emite
`POST /api/auth/login` del backend (firma HMAC con `jwt.secret`, claims
`sub` = email y `role` = `VISITANTE | CURADOR | ADMINISTRADOR`).

- Sin token, vencido o alterado: **401**.
- Rol no permitido: **403**.

Roles por operación, según la consigna:

| Operación | Roles |
|---|---|
| Recorridos, recomendaciones, preferencias, interacciones | VISITANTE |
| Comparación, exploración, categorías | VISITANTE, CURADOR, ADMINISTRADOR |

## Errores

Mismo cuerpo que la Entrega 1 (`RespuestaError`), sin stack traces:

```json
{ "codigo": 404, "estado": "NOT_FOUND", "mensaje": "No existe la obra 99", "fechaHora": "27-10-2026 18:30:00" }
```

| Origen | HTTP |
|---|---|
| Validación del pedido en el gateway | 400 |
| gRPC `INVALID_ARGUMENT` | 400 |
| gRPC `NOT_FOUND` | 404 |
| gRPC `UNAVAILABLE` o `DEADLINE_EXCEEDED` (servicio caído o lento) | 503, mensaje "El servicio X no está disponible" |
| Cualquier otro | 500, mensaje genérico |

## Trazas

Toda respuesta exitosa de las cuatro funcionalidades trae `trazas`: las
llamadas gRPC que hizo el gateway, en orden, con su duración. Sirve para ver
en el front (y en el video) que la operación coordinó varios servicios.

```json
"trazas": [
  { "servicio": "UsuariosService", "metodo": "ObtenerPerfil", "ms": 12 },
  { "servicio": "CatalogoService", "metodo": "BuscarObras", "ms": 25 },
  { "servicio": "MotorService", "metodo": "ArmarRecorrido", "ms": 8 }
]
```

Las llamadas que corren en paralelo aparecen en el orden en que terminaron.

## Obra (JSON)

Todas las respuestas usan la misma forma para una obra (camelCase de
`catalogo.Obra`):

```json
{
  "id": 1, "titulo": "La Gioconda", "descripcion": "...",
  "artista": { "id": 1, "nombre": "Leonardo da Vinci", "nacionalidad": "Italiana", "movimiento": "Renacimiento" },
  "anioCreacion": 1503, "tecnica": "Óleo sobre tabla", "tematica": "Retrato",
  "movimiento": "Renacimiento", "dimensiones": "77 cm x 53 cm",
  "ubicacion": "Sala de Arte Europeo", "enExhibicion": true, "imagenUrl": "/obras/gioconda.jpg"
}
```

`anioCreacion`, `anioNacimiento` y `anioFallecimiento` van en `null` cuando el
proto trae 0.

---

## 1. Generar recorrido personalizado

`POST /api/v2/recorridos` · VISITANTE

```json
{
  "duracionMinutos": 60,
  "movimientos": ["Impresionismo"],
  "tecnicas": ["Óleo"],
  "tematicas": ["Paisaje"],
  "cantidadObras": 8,
  "usarPreferencias": true
}
```

Todos los campos son opcionales. `duracionMinutos` entre 15 y 480;
`cantidadObras` entre 1 y 30 (fuera de rango: 400).

Orquestación:

1. Si `usarPreferencias`: `UsuariosService.ObtenerPerfil(email del token)`.
2. `CatalogoService.BuscarObras` con los criterios del pedido tal cual y
   `solo_en_exhibicion = true` (solo se recorre lo que está en sala). Sin
   criterios, son todas las obras en exhibición.
3. `MotorService.ArmarRecorrido(candidatas, duración, cantidad, preferencias)`.
   El motor decide qué entra, en qué orden y cuánto tiempo; las preferencias
   solo priorizan entre candidatas. Con cero candidatas devuelve un
   recorrido vacío con una nota.

El gateway no toma ninguna decisión sobre el contenido: llama siempre a los
mismos servicios y pasa los datos de uno al otro.

Respuesta 200:

```json
{
  "paradas": [
    { "orden": 1, "sala": "Sala de Arte Europeo", "minutosSugeridos": 6,
      "motivo": "Coincide con tu interés en Impresionismo",
      "obra": { "...": "Obra completa" } }
  ],
  "duracionTotalMinutos": 52,
  "notas": ["Quedaron 2 obras afuera para no pasarte de 60 minutos"],
  "trazas": []
}
```

## 2. Comparar obras

`POST /api/v2/comparaciones` · todos los roles

```json
{ "obraIds": [1, 2, 5] }
```

Entre 2 y 6 ids distintos (si no: 400).

Orquestación: `CatalogoService.ObtenerObras(ids)` →
`MotorService.CompararObras(obras)`.

Respuesta 200:

```json
{
  "obras": [ { "...": "Obra completa, en el orden pedido" } ],
  "atributos": [
    { "nombre": "Movimiento", "valores": ["Renacimiento", "Postimpresionismo", "Renacimiento"], "coinciden": false }
  ],
  "observaciones": ["Entre la más antigua y la más reciente hay 386 años"],
  "trazas": []
}
```

## 3. Recomendaciones

`GET /api/v2/recomendaciones?cantidad=6` · VISITANTE

`cantidad` entre 1 y 20, por defecto 6.

Orquestación (los pasos 1 y 2 en paralelo):

1. `UsuariosService.ObtenerPerfil(email)` y
   `UsuariosService.ListarInteracciones(email)`.
2. `EventosService.ListarEventosDeUsuario(email)`.
3. `CatalogoService.BuscarObras` sin filtros (todas las obras: candidatas) y,
   si hubo interacciones, `CatalogoService.ObtenerObras(ids de esas obras)`.
4. `MotorService.Recomendar(...)`.

Respuesta 200:

```json
{
  "recomendaciones": [
    { "obra": { "...": "Obra completa" }, "puntaje": 7.5,
      "motivos": ["Es del Impresionismo, uno de tus movimientos preferidos", "Te anotaste a una charla sobre Impresionismo"] }
  ],
  "criterio": "PERSONALIZADO",
  "preferencias": { "movimientos": [], "tecnicas": [], "tematicas": [] },
  "trazas": []
}
```

## 4. Preferencias e interacciones (soporte de 1 y 3)

`GET /api/v2/preferencias` · VISITANTE → `UsuariosService.ObtenerPerfil`

```json
{ "movimientos": ["Impresionismo"], "tecnicas": ["Óleo"], "tematicas": ["Paisaje"] }
```

`PUT /api/v2/preferencias` · VISITANTE → `UsuariosService.ActualizarPreferencias`.
Mismo cuerpo; responde 200 con las preferencias guardadas.

`POST /api/v2/interacciones` · VISITANTE → `CatalogoService.ObtenerObras([obraId])`
(404 si no existe) → `UsuariosService.RegistrarInteraccion`

```json
{ "obraId": 3, "tipo": "INTERES" }
```

`tipo`: `VISTA` o `INTERES`. Responde 201 con
`{ "obraId": 3, "tipo": "INTERES", "fecha": "2026-10-27T18:30:00" }`.

## 5. Exploración por movimiento

`GET /api/v2/exploraciones/{movimiento}` · todos los roles

Orquestación (en paralelo): `CatalogoService.BuscarObras(movimientos=[m])`,
`CatalogoService.ListarArtistas(m)`,
`EventosService.ListarEventosPorMovimiento(m, solo_futuros=true)` y
`CatalogoService.ListarCategorias()`.

Un movimiento sin datos devuelve 200 con las listas vacías.

Respuesta 200:

```json
{
  "movimiento": "Impresionismo",
  "obras": [ { "...": "Obra completa" } ],
  "artistas": [ { "id": 4, "nombre": "Claude Monet", "nacionalidad": "Francesa", "movimiento": "Impresionismo",
                  "anioNacimiento": 1840, "anioFallecimiento": 1926, "cantidadObras": 2, "biografia": "..." } ],
  "eventos": [ { "id": 7, "titulo": "Charla: la luz impresionista", "tipo": "CHARLA",
                 "fechaHora": "2026-11-05T18:00:00", "duracionMinutos": 60, "cupoMaximo": 30, "inscriptos": 12 } ],
  "otrosMovimientos": [ { "nombre": "Postimpresionismo", "cantidadObras": 2 } ],
  "trazas": []
}
```

`otrosMovimientos`: los demás movimientos de `ListarCategorias`, para seguir
navegando.

## 6. Categorías

`GET /api/v2/categorias` · todos los roles → `CatalogoService.ListarCategorias`

```json
{ "movimientos": [{ "nombre": "Renacimiento", "cantidadObras": 3 }], "tecnicas": [], "tematicas": [] }
```
