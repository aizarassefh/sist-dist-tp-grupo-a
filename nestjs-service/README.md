# Motor de experiencias (NestJS)

Microservicio gRPC del Museo Virtual. Implementa `MotorService` (`proto/motor.proto`):
`ArmarRecorrido`, `Recomendar` y `CompararObras`.

No tiene base de datos ni llama a otros servicios: el API Gateway le manda en cada
pedido los datos que juntó de Catálogo, Usuarios y Eventos, y el motor devuelve el
resultado del cálculo. Las reglas de negocio están en funciones puras en `src/motor/`
(`recorrido.ts`, `recomendacion.ts`, `comparacion.ts`); `motor.controller.ts` solo delega.

## Correr

    npm install
    npm run build
    npm start          # escucha en 0.0.0.0:9091

Variables de entorno:

- `GRPC_PORT`: puerto gRPC (por defecto 9091).
- `PROTO_DIR`: carpeta con los `.proto` (por defecto `../../proto` respecto de `dist/`,
  es decir la carpeta `proto/` de la raíz del repo).

## Tests

    npm test

Incluye unit tests de las tres funciones puras y un test de integración que levanta el
servidor en un puerto libre y lo llama por gRPC con un cliente generado desde `motor.proto`.

## Errores

`CompararObras` con menos de dos obras responde `INVALID_ARGUMENT`. Que no haya
candidatas en `ArmarRecorrido` no es error: devuelve paradas vacías y una nota.
