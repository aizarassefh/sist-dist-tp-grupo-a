# Contrato de mensajería (Entrega 3)

Las operaciones sobre eventos (alta, modificación y cancelación) publican una
**novedad** en Kafka. Dos consumidores independientes la procesan: uno genera
las notificaciones de los visitantes y otro registra el historial de actividad.
La aplicación web consulta lo que los consumidores persistieron, nunca los
mensajes del broker.

## Componentes

| Componente | Proceso | Rol |
|---|---|---|
| Módulo `eventos` del backend | `api-rest-graphql` (perfil por defecto) | **Productor.** Cada operación guarda el evento y su novedad en `mensajes_salientes` en la misma transacción. |
| Publicador del outbox | `api-rest-graphql` (perfil por defecto) | Lee `mensajes_salientes` pendientes y los publica en Kafka. Los marca publicados recién cuando el broker confirma. |
| Kafka | broker KRaft, `localhost:9092` | Transporta los mensajes. |
| Consumidor de notificaciones | `api-rest-graphql` con perfil `consumidor-notificaciones` | Grupo `museo-notificaciones`. Genera una notificación por visitante destinatario. |
| Consumidor de auditoría | `api-rest-graphql` con perfil `consumidor-auditoria` | Grupo `museo-auditoria`. Registra el historial con los datos anteriores y posteriores. |
| Consumidor de fallidos | dentro de cada proceso consumidor | Grupo `museo-fallidos`. Guarda en `mensajes_fallidos` lo que llegó al DLT. |
| Consultas | `api-rest-graphql` (perfil por defecto) | REST: notificaciones del visitante, historial y estado de la mensajería. |

Los tres procesos usan el mismo código y la misma base, pero cada uno solo
escribe sus tablas: el productor `eventos` y `mensajes_salientes`, el
consumidor de notificaciones `notificaciones`, el de auditoría
`historial_actividad`, y ambos sus filas de `mensajes_procesados` y
`mensajes_fallidos`. Un consumidor nuevo se agrega con un grupo nuevo sobre el
mismo topic, sin tocar el productor.

## Topics

| Topic | Particiones | Clave | Contenido |
|---|---|---|---|
| `eventos.novedades` | 3 | id del evento (texto) | Novedad en JSON (abajo) |
| `eventos.novedades.DLT` | 1 | la del mensaje original | El mensaje original tal cual, con los headers de error de Spring Kafka (`kafka_dlt-exception-message`, `kafka_dlt-original-topic`, `kafka_dlt-original-partition`, `kafka_dlt-original-offset`) |

La clave es el id del evento para que todas las novedades de un mismo evento
caigan en la misma partición y se consuman en orden (alta antes que
modificación antes que cancelación).

## Novedad (JSON, versión 1)

| Campo | Tipo | Obligatorio | Significado |
|---|---|---|---|
| `idMensaje` | string (UUID) | sí | Identificador único del acontecimiento. Base de la idempotencia. |
| `versionEsquema` | int | sí | `1`. Permite cambiar el formato sin romper consumidores viejos. |
| `tipo` | string | sí | `ALTA`, `MODIFICACION` o `CANCELACION`. |
| `idEvento` | long | sí | Evento afectado. |
| `tituloEvento` | string | sí | Título del evento (el posterior en una modificación). |
| `fechaHoraEvento` | string (ISO local) | sí | Fecha y hora del evento. |
| `fechaOperacion` | string (ISO instante, UTC) | sí | Cuándo se hizo la operación. |
| `idUsuarioResponsable` | long | no | Usuario del token que hizo la operación. |
| `nombreUsuarioResponsable` | string | no | Nombre y apellido, para mostrar sin consultar la tabla de usuarios. |
| `eventoAnterior` | Evento | no | Estado antes de la operación. `null` en `ALTA`. |
| `eventoPosterior` | Evento | no | Estado después. `null` en `CANCELACION`. |
| `camposModificados` | string[] | sí | Nombres de los campos que cambiaron (vacío en `ALTA` y `CANCELACION`). |

**Evento** (foto completa del evento en ese momento):

| Campo | Tipo | Significado |
|---|---|---|
| `id` | long | |
| `titulo` | string | |
| `descripcion` | string | |
| `tipo` | string | `VISITA_GUIADA`, `TALLER` o `CHARLA` |
| `fechaHora` | string (ISO local) | |
| `duracionMinutos` | int | |
| `cupoMaximo` | int | |
| `movimiento` | string | puede ser `null` |
| `idCurador` | long | |
| `nombreCurador` | string | |
| `idsInscriptos` | long[] | usuarios inscriptos en ese momento |

La foto incluye los inscriptos porque la cancelación borra el evento con sus
inscripciones: sin ellos en el mensaje, el consumidor de notificaciones no
podría saber a quién avisar, y consultarlos le exigiría leer tablas del módulo
de eventos.

Ejemplo de modificación:

```json
{
  "idMensaje": "3f1c2a9e-6b1d-4c3e-9a77-2f0d5e8b1c44",
  "versionEsquema": 1,
  "tipo": "MODIFICACION",
  "idEvento": 7,
  "tituloEvento": "Charla: la luz impresionista",
  "fechaHoraEvento": "2026-11-12T18:30:00",
  "fechaOperacion": "2026-11-01T18:02:11.483Z",
  "idUsuarioResponsable": 2,
  "nombreUsuarioResponsable": "Juan Perez",
  "eventoAnterior": { "id": 7, "titulo": "Charla: la luz impresionista", "descripcion": "...", "tipo": "CHARLA",
                      "fechaHora": "2026-11-12T18:00:00", "duracionMinutos": 60, "cupoMaximo": 30,
                      "movimiento": "Impresionismo", "idCurador": 2, "nombreCurador": "Juan Perez", "idsInscriptos": [3, 5] },
  "eventoPosterior": { "id": 7, "titulo": "Charla: la luz impresionista", "descripcion": "...", "tipo": "CHARLA",
                       "fechaHora": "2026-11-12T18:30:00", "duracionMinutos": 60, "cupoMaximo": 40,
                       "movimiento": "Impresionismo", "idCurador": 2, "nombreCurador": "Juan Perez", "idsInscriptos": [3, 5] },
  "camposModificados": ["fechaHora", "cupoMaximo"]
}
```

Solo se publica una `MODIFICACION` si cambió algún campo de la foto, sin contar
`idsInscriptos`: inscribirse o desinscribirse no es una modificación del evento.

## Destinatarios de las notificaciones

| Tipo | Destinatarios | Texto |
|---|---|---|
| `ALTA` | Visitantes que tienen el movimiento del evento entre sus preferidos (consulta gRPC `UsuariosService.ListarVisitantesPorMovimiento`). Sin movimiento, nadie. | "Nuevo evento sobre {movimiento}: {título}, el {fecha}." |
| `MODIFICACION` | `eventoPosterior.idsInscriptos` | "Cambió {título}: {campos legibles}." Ej.: "nueva fecha 12/11 18:30, cupo 40". |
| `CANCELACION` | `eventoAnterior.idsInscriptos` | "Se canceló {título}, que era el {fecha}." |

## Procesamiento y confiabilidad

- **Entrega al menos una vez.** El productor publica con `acks=all` e
  idempotencia habilitada. El consumidor confirma el offset (`AckMode.RECORD`)
  después de que su transacción en la base hizo commit.
- **Idempotencia.** Cada consumidor inserta `(consumidor, id_mensaje)` en
  `mensajes_procesados` en la misma transacción que su efecto. Si el mensaje ya
  estaba, no hace nada. Un reenvío después de una caída entre el commit y la
  confirmación del offset no duplica nada.
- **Reintentos.** 3 reintentos con 2 segundos de espera. Los errores que no se
  arreglan reintentando (JSON inválido, campos obligatorios faltantes, versión
  desconocida) van directo al DLT.
- **DLT.** Agotados los reintentos, el mensaje va a `eventos.novedades.DLT`. El
  consumidor de fallidos lo guarda en `mensajes_fallidos` con el error. Desde
  la pantalla de mensajería, un administrador lo reprocesa: se vuelve a
  publicar en `eventos.novedades` con el mismo `idMensaje`, así que si algún
  consumidor ya lo había procesado, lo ignora.
- **Caída del productor o de Kafka.** La novedad queda en `mensajes_salientes`
  como pendiente; el publicador la reintenta cada segundo hasta que Kafka
  confirma. La operación del usuario no espera a Kafka.
- **Consistencia eventual.** `GET /api/mensajeria/consistencia` compara lo
  publicado con lo procesado por cada consumidor y lista lo que falta.

## API REST (backend, puerto 8000)

Todas con `Authorization: Bearer <token>`. Errores con el mismo `RespuestaError`
de la Entrega 1.

### Notificaciones (solo VISITANTE, siempre las del usuario del token)

`GET /api/notificaciones?soloNoLeidas=false&limite=50`

```json
{
  "noLeidas": 2,
  "notificaciones": [
    { "id": 15, "tipo": "MODIFICACION", "idEvento": 7, "tituloEvento": "Charla: la luz impresionista",
      "mensaje": "Cambió Charla: la luz impresionista: nueva fecha 12/11 18:30, cupo 40.",
      "fechaOperacion": "2026-11-01T18:02:11Z", "leida": false }
  ]
}
```

`PATCH /api/notificaciones/{id}/leida` → 204. 404 si no existe o es de otro usuario.
`PATCH /api/notificaciones/leidas` → 204, marca todas.

### Historial (CURADOR y ADMINISTRADOR)

`GET /api/auditoria/eventos?idEvento=7&tipo=MODIFICACION&limite=100`

```json
[
  { "idMensaje": "3f1c2a9e-...", "tipo": "MODIFICACION", "idEvento": 7,
    "tituloEvento": "Charla: la luz impresionista", "fechaHoraEvento": "2026-11-12T18:30:00",
    "fechaOperacion": "2026-11-01T18:02:11Z",
    "usuarioResponsable": { "id": 2, "nombre": "Juan Perez" },
    "camposModificados": ["fechaHora", "cupoMaximo"],
    "datosAnteriores": { "...": "Evento" }, "datosPosteriores": { "...": "Evento" },
    "fechaProcesamiento": "2026-11-01T18:02:12Z" }
]
```

### Mensajería (CURADOR y ADMINISTRADOR; reprocesar solo ADMINISTRADOR)

`GET /api/mensajeria/consistencia`

```json
{
  "generados": 12, "publicados": 12, "pendientesDePublicar": 0,
  "consumidores": [
    { "consumidor": "notificaciones", "procesados": 12, "faltantes": [] },
    { "consumidor": "auditoria", "procesados": 11, "faltantes": ["3f1c2a9e-..."] }
  ],
  "fallidosPendientes": 0,
  "consistente": false
}
```

`consistente` es `true` cuando no hay nada pendiente de publicar, ningún
consumidor tiene faltantes y no hay fallidos pendientes.

`GET /api/mensajeria/fallidos?estado=PENDIENTE` → lista de
`{ id, idMensaje, consumidor, error, contenido, fechaRecepcion, estado }`.

`POST /api/mensajeria/fallidos/{id}/reprocesar` → 202. Vuelve a publicar el
contenido original en `eventos.novedades` y marca el fallido `REPROCESADO`.

## Tablas nuevas

| Tabla | Dueño | Columnas |
|---|---|---|
| `mensajes_salientes` | eventos (productor) | `id_mensaje` PK, `topic`, `clave`, `contenido` TEXT, `fecha_creacion`, `fecha_publicacion` (null = pendiente), `intentos`, `ultimo_error` |
| `notificaciones` | consumidor de notificaciones | `id` PK, `id_mensaje`, `id_usuario`, `tipo`, `id_evento`, `titulo_evento`, `mensaje`, `fecha_operacion`, `leida`, `fecha_creacion`; único (`id_mensaje`, `id_usuario`) |
| `historial_actividad` | consumidor de auditoría | `id_mensaje` PK, `tipo`, `id_evento`, `titulo_evento`, `fecha_hora_evento`, `fecha_operacion`, `id_usuario_responsable`, `nombre_usuario_responsable`, `campos_modificados`, `datos_anteriores` TEXT (JSON), `datos_posteriores` TEXT (JSON), `fecha_procesamiento` |
| `mensajes_procesados` | cada consumidor | PK (`consumidor`, `id_mensaje`), `fecha_procesamiento` |
| `mensajes_fallidos` | consumidor de fallidos | `id` PK, `id_mensaje` (puede ser null si el mensaje no se pudo leer), `consumidor`, `error`, `contenido` TEXT, `fecha_recepcion`, `estado` (`PENDIENTE`, `REPROCESADO`) |

`id_usuario` e `id_evento` no tienen clave foránea: son ids de tablas de otros
módulos, y un evento cancelado ya no existe.
