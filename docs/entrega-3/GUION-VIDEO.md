# Guion del video de la Entrega 3

La consigna pide mostrar: la publicación de novedades, el procesamiento de los
mensajes, la generación de notificaciones, el registro del historial, al menos
un escenario de recuperación o tratamiento de errores, y la continuidad de las
Entregas 1 y 2. Duración sugerida: 10 a 12 minutos.

## Antes de grabar

- Levantar todo como dice el README: MariaDB, Kafka (`kafka\levantar-kafka.cmd`
  y, la primera vez, `kafka\crear-topics.cmd`), el backend principal, los dos
  consumidores, el motor, el gateway y el front.
- Correr `node pruebas/e2e-kafka.mjs` y `node pruebas/e2e.mjs`: si dan todo OK,
  está listo. Esperar unos 30 segundos después de levantar el backend antes de
  grabar: los clientes gRPC del gateway y del consumidor de notificaciones
  tardan un poco en conectarse.
- Terminales visibles: Kafka, backend principal, consumidor de notificaciones y
  consumidor de auditoría (sus logs muestran cada mensaje procesado).
- Pestañas: el front con el curador en una ventana y el visitante en otra
  (ventana privada), y el documento abierto en el diagrama de arquitectura.
- Como visitante, poner Impresionismo entre las preferencias (pantalla
  Recomendaciones), así le llegan las altas de ese movimiento.

## 1. Arquitectura (1,5 min)

- Diagrama: el módulo de eventos es el productor; guarda la novedad en el
  outbox en la misma transacción que el evento, y el publicador la manda a
  Kafka. Dos consumidores en procesos propios, cada uno con su grupo.
- Mostrar que son tres procesos del mismo jar con distinto perfil (las tres
  terminales).
- Mostrar `docs/contrato-mensajeria.md`: el formato de la novedad y los topics.

## 2. Alta → notificación (1,5 min)

- Como curador: Eventos → nuevo evento, con movimiento Impresionismo.
- Señalar en los logs: el publicador lo manda, cada consumidor lo procesa.
- Como visitante: aparece el contador en Notificaciones y el aviso "Nuevo
  evento sobre Impresionismo…". Decir que es asincrónico: llega un instante
  después.

## 3. Modificación y cancelación (2 min)

- Como visitante, inscribirse al evento.
- Como curador, cambiarle la fecha y el cupo. El visitante recibe "Cambió …:
  nueva fecha …, cupo …".
- Como curador, Historial: abrir el detalle y mostrar anterior → posterior de
  cada campo y el responsable.
- Borrar el evento: el visitante recibe la cancelación aunque el evento ya no
  existe (la foto del evento viaja en el mensaje). En el historial aparece la
  cancelación con los datos que tenía.

## 4. Duplicados y mensajes inválidos (1,5 min)

- Explicar: Kafka entrega al menos una vez; cada consumidor guarda el
  `idMensaje` en `mensajes_procesados` en la misma transacción.
- Correr `kafka\publicar-mensaje-invalido.cmd`: en Mensajería aparece como
  fallido de cada consumidor, con el error, sin generar notificaciones ni
  historial.
- Opcional: correr `node pruebas/e2e-kafka.mjs` y mostrar la línea del
  duplicado inyectado 3 veces.

## 5. Recuperación (2,5 min)

- **Consumidor caído:** Ctrl+C en el consumidor de auditoría. Modificar un
  evento dos veces: las notificaciones llegan igual. En Mensajería, a
  auditoría le faltan 2. Levantarlo de nuevo: retoma desde su offset y la
  consistencia vuelve a verde para ese consumidor.
- **Kafka caído:** Ctrl+C en Kafka. Crear un evento: responde al instante y en
  Mensajería queda 1 pendiente de publicar. Levantar Kafka: se publica solo y
  los consumidores lo procesan.
- **Reproceso (opcional):** con el backend principal apagado, el consumidor de
  notificaciones no puede resolver los destinatarios de un alta, reintenta y
  la manda a fallidos. Al levantar el backend, esperar unos segundos y
  reprocesarla como administrador desde Mensajería. Ver la sección 7.3 del
  documento.

## 6. Continuidad (1 min)

- Colección y ficha de obra (GraphQL), reporte y Excel (curador).
- Un recorrido o una comparación (gateway y gRPC de la Entrega 2).
- Correr `node pruebas/e2e.mjs`: 14 de 14.

## 7. Pruebas (1 min)

- Mostrar el resultado de `./mvnw test` del backend (124, con los de Kafka
  embebido) y de `node pruebas/e2e-kafka.mjs`.
