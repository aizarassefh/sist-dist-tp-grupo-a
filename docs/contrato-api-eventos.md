# Contrato REST de eventos y filtros favoritos

Este es el contrato contra el que está hecho el frontend. Las rutas son las
de la sección 5 de `PLAN.md`; este documento agrega el formato exacto de cada
JSON, que el plan no fijaba.

**Estado:** el frontend ya consume estas rutas. En el backend hoy solo
existen `GET /api/eventos` (sin filtros y con otro formato) y
`POST /api/eventos` (sin control de rol). Todo lo demás está pendiente.

Si el backend necesita cambiar algo de acá, avisar antes: cambia también el
frontend.

## Convenciones

- JSON plano, sin envoltorio JSend: el login y `/me` ya funcionan así.
- Fechas y horas en hora local de Argentina, formato ISO sin zona:
  `"2026-10-01T15:00:00"`. Filtros de fecha: `"2026-10-01"`, inclusivos.
- Todas las rutas requieren `Authorization: Bearer <token>`.
- Errores: el frontend decide el texto según el **código HTTP**, así que lo
  importante es devolver el código correcto (tabla al final).

## Tipos de evento

Tres valores fijos, los mismos que ya usa el reporte GraphQL:
`VISITA_GUIADA`, `TALLER`, `CHARLA`.

Usar estos valores también en el reporte y en el Excel es lo que hace que
los tres módulos hablen de los mismos eventos. El nombre legible ("Visitas
guiadas") lo pone el frontend, y el Excel usa el mismo nombre para la hoja.

## Evento

```json
{
  "id": 7,
  "titulo": "Recorrido por el arte barroco",
  "descripcion": "Visita comentada por la sala de arte europeo.",
  "tipo": "VISITA_GUIADA",
  "fechaHora": "2026-10-03T15:00:00",
  "duracionMinutos": 90,
  "cupoMaximo": 20,
  "curadorResponsable": { "id": 2, "nombre": "Juan Perez" },
  "cantidadInscriptos": 5,
  "inscripto": true,
  "inscriptos": [ { "id": 3, "nombre": "Carlos Hernandez" } ]
}
```

- `inscripto`: si el usuario del token está anotado. Lo calcula el backend
  con el `sub` del token; el frontend nunca manda su propio id.
- `inscriptos`: es la `lista_de_inscriptos` que pide la consigna. Va
  **solo en el detalle**; en el listado se omite y alcanza con
  `cantidadInscriptos`.
- `nombre` es nombre y apellido. No exponer email, teléfono ni hash.

## Endpoints de eventos

| Método y ruta | Rol | Respuesta |
|---|---|---|
| `GET /api/eventos` | cualquiera logueado | 200, página de eventos |
| `GET /api/eventos/{id}` | cualquiera logueado | 200 evento con `inscriptos`; 404 |
| `POST /api/eventos` | CURADOR, ADMINISTRADOR | 201 evento creado; 400; 403 |
| `PUT /api/eventos/{id}` | CURADOR, ADMINISTRADOR | 200 evento; 400; 403; 404; 409 si el cupo queda por debajo de los inscriptos |
| `DELETE /api/eventos/{id}` | CURADOR, ADMINISTRADOR | 204; 403; 404 (ver nota) |
| `POST /api/eventos/{id}/inscripcion` | cualquiera logueado | 201; 404; 409 sin cupo, ya inscripto o evento ya comenzado |
| `DELETE /api/eventos/{id}/inscripcion` | cualquiera logueado | 204 (también si no estaba inscripto); 404 |
| `GET /api/usuarios/curadores` | cualquiera logueado | 200 `[ { "id": 2, "nombre": "Juan Perez" } ]` |

`GET /api/usuarios/curadores` **no está en el PLAN**: hace falta para el
filtro "curador a cargo" y para elegir el curador al crear un evento. Sin
esto, el formulario tendría que pedir un id a mano.

**Borrar un evento con inscriptos:** `PLAN.md` dice 409 y `PLAN-2.0.md`
dice borrar las inscripciones en cascada. El frontend pide confirmación y
funciona con las dos: si recibe 409 muestra "no se puede borrar un evento
con inscriptos". Hay que decidir una.

### Filtros del listado

`GET /api/eventos?desde=2026-10-01&hasta=2026-10-31&tipo=TALLER&curadorId=2&estado=FUTUROS&pagina=0&tamanio=10`

Todos opcionales. `estado`: `PASADOS`, `FUTUROS` o `TODOS` (default).
`pagina` empieza en 0; `tamanio` default 10, máximo 100. Orden por
`fechaHora` y después `id`.

Respuesta:

```json
{
  "items": [ { "id": 7, "titulo": "...", "...": "evento sin inscriptos" } ],
  "pagina": 0,
  "tamanio": 10,
  "total": 23
}
```

### Crear y modificar

Mismo cuerpo para `POST` y `PUT`:

```json
{
  "titulo": "Recorrido por el arte barroco",
  "descripcion": "Visita comentada por la sala de arte europeo.",
  "tipo": "VISITA_GUIADA",
  "fechaHora": "2026-10-03T15:00:00",
  "duracionMinutos": 90,
  "cupoMaximo": 20,
  "curadorId": 2
}
```

`curadorId` tiene que ser un usuario con rol CURADOR; si no, 400.

## Filtros favoritos

Cada usuario ve y toca solo los suyos. Un id ajeno responde **404**, igual
que uno inexistente, para no revelar que existe.

```json
{
  "id": 4,
  "nombre": "Talleres de octubre",
  "descripcion": "Para organizar las vacaciones",
  "filtros": {
    "desde": "2026-10-01",
    "hasta": "2026-10-31",
    "tipo": "TALLER",
    "curadorId": null,
    "estado": "FUTUROS"
  }
}
```

`filtros` tiene los mismos campos que los filtros del listado de eventos.
Los que no se usan van en `null`.

| Método y ruta | Respuesta |
|---|---|
| `GET /api/filtros-favoritos` | 200 `[ favorito, ... ]` |
| `POST /api/filtros-favoritos` | 201 favorito creado; 400 |
| `PUT /api/filtros-favoritos/{id}` | 200 favorito; 400; 404 |
| `DELETE /api/filtros-favoritos/{id}` | 204; 404 |
| `GET /api/filtros-favoritos/{id}/eventos?pagina=0&tamanio=10` | 200 página de eventos, mismo formato que el listado; 404 |

El cuerpo de `POST` y `PUT` es el favorito sin `id`.

## Exportación a Excel

El frontend hoy llama a la ruta que ya existe, pasándole los filtros del
reporte:

`GET /api/eventos/exportar?desde&hasta&tipo&estado` → `.xlsx`

El backend todavía ignora esos parámetros. `PLAN.md` propone mover la
exportación a `GET /api/reportes/asistencia/excel`, porque la consigna la
ubica en el módulo de reportes; si se mueve, en el frontend es cambiar una
constante en `src/components/ReporteAsistencia.jsx`.

## Códigos que el frontend espera

| Código | Cuándo |
|---|---|
| 200 / 201 / 204 | OK, creado, borrado |
| 400 | Datos inválidos |
| 401 | Sin token, token vencido o alterado (el frontend vuelve al login) |
| 403 | Token válido pero rol insuficiente |
| 404 | No existe, o es un favorito de otro usuario |
| 409 | Sin cupo, ya inscripto, evento comenzado o cupo menor a inscriptos |

Hoy una ruta inexistente devuelve 500, no 404. El frontend lo trata como
"todavía no disponible en el servidor".
