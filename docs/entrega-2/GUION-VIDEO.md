# Guion del video de la Entrega 2

La consigna pide un video **narrado** que muestre: la aplicación funcionando,
las cuatro funcionalidades nuevas, la interacción entre el gateway y los
servicios, la comunicación por gRPC, al menos un flujo que coordine varios
servicios y que la Entrega 1 sigue andando. Duración sugerida: 10 a 12
minutos. Los tiempos son orientativos.

## Antes de grabar

- Levantar todo como dice el README (base, backend, motor, gateway, front).
  Correr `node pruebas/e2e.mjs`: si da todo OK, está listo para grabar.
- Pestañas abiertas: `http://localhost:5173` y el documento de entrega
  (`docs/entrega-2/Entrega-2-Grupo-A.pdf`) en la página del diagrama.
- Cuatro terminales visibles (o una con pestañas): backend, motor, gateway y
  front, para mostrar los logs.
- Usuarios: `visitante@test.com` y `curador@test.com` (la clave está en
  `DataInitializer.java` y en la colección de Postman).

## 1. Arquitectura (1,5 min)

- Mostrar el diagrama: el front habla con el gateway para lo nuevo y con el
  backend para lo de la Entrega 1.
- El gateway (Java) solo orquesta: valida el JWT, llama por gRPC y arma la
  respuesta. No tiene base ni reglas de negocio.
- Servicios: Catálogo, Eventos y Usuarios dentro del backend, uno por módulo,
  cada uno dueño de sus tablas. El motor en NestJS (TypeScript) hace los
  cálculos y no tiene base.
- Mostrar la carpeta `proto/`: un contrato por servicio, y que el motor
  importa los mensajes de los otros tres.

## 2. Recorrido personalizado (2 min) — flujo con 3 servicios

- Entrar como visitante. Recorridos: 45 minutos, movimiento Impresionismo,
  técnica Óleo, tildar "Tener en cuenta mis preferencias".
- Mostrar el resultado: paradas numeradas, sala, minutos, motivo y la
  duración total que no pasa de 45.
- Señalar "Cómo se resolvió": UsuariosService → CatalogoService →
  MotorService, con los milisegundos. Ese es el flujo que coordina varios
  servicios.
- Cambiar la duración a 15 minutos: menos paradas y una nota que dice cuántas
  quedaron afuera. Explicar que esa decisión la toma el motor, no el gateway.

## 3. Comparación de obras (1,5 min)

- Comparar tres obras (por ejemplo La Gioconda, La noche estrellada y una del
  Renacimiento). Mostrar la tabla: filas resaltadas donde coinciden,
  observaciones (años que las separan, sala).
- Mostrar que también lo puede hacer un curador.

## 4. Recomendaciones (1,5 min) — flujo con 4 servicios

- Como visitante, Recomendaciones: lista con puntaje y motivos ("es del
  Impresionismo, uno de tus movimientos preferidos", "te anotaste a un evento
  sobre…").
- Cambiar las preferencias (por ejemplo, agregar Surrealismo) y guardar: la
  lista cambia.
- Entrar a la ficha de una obra y tocar "Me interesa"; volver: aparecen
  recomendaciones del mismo artista o movimiento.
- Trazas: Usuarios y Eventos en paralelo, después Catálogo y el motor.

## 5. Exploración (1,5 min)

- Explorar → Impresionismo: obras, artistas con sus años, eventos vinculados
  y "Seguir explorando" con los otros movimientos.
- Hacer click en un evento: lleva a la pantalla de eventos de la Entrega 1.
- Trazas: cuatro llamadas en paralelo a Catálogo y Eventos.

## 6. Comunicación gRPC y tolerancia a fallas (1,5 min)

- Mostrar el log del gateway o del backend mientras se genera un recorrido.
- Frenar el motor NestJS (Ctrl+C en su terminal):
  - Comparar obras → mensaje "El servicio MotorService no está disponible"
    (503).
  - Explorar un movimiento → sigue funcionando, porque no usa el motor.
- Volver a levantar el motor y repetir la comparación: anda. Puede tardar
  unos segundos (o hasta un minuto si el motor estuvo caído mucho rato):
  mientras un servicio no responde, el cliente gRPC del gateway reintenta
  la conexión esperando cada vez más entre intentos. Vale la pena decirlo en
  el video. Lo mismo pasa si se reinicia el backend con el gateway levantado.

## 7. La Entrega 1 sigue funcionando (1 min)

- Colección (GraphQL) con un filtro, ficha de una obra.
- Eventos: inscribirse a uno.
- Como curador: Reporte y exportar a Excel.

## 8. Pruebas (1 min)

- Correr `node pruebas/e2e.mjs` y mostrar el resumen.
- Mencionar los tests de cada proyecto (backend, gateway y motor) y cuántos
  pasan.
