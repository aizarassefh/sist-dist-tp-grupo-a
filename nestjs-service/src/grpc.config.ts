import { join } from 'path';
import { GrpcOptions, Transport } from '@nestjs/microservices';

// Los .proto viven en la raíz del repo, compartidos con los servicios Java;
// desde dist/ son dos niveles arriba. PROTO_DIR permite otra ubicación (Docker).
export const dirProtoPorDefecto = () => process.env.PROTO_DIR || join(__dirname, '../../proto');

// Aparte de main.ts para que el test de integración levante el servidor con la
// misma configuración que producción, solo cambiando el puerto.
export function opcionesGrpc(puerto: number | string, dirProto: string = dirProtoPorDefecto()): GrpcOptions {
  return {
    transport: Transport.GRPC,
    options: {
      package: 'museo.motor',
      protoPath: join(dirProto, 'motor.proto'),
      url: `0.0.0.0:${puerto}`,
      loader: {
        // motor.proto importa catalogo/eventos/usuarios.proto de su carpeta.
        includeDirs: [dirProto],
        // Los campos viajan en snake_case tal cual el contrato.
        keepCase: true,
        longs: Number,
        enums: String,
        defaults: true,
        arrays: true,
      },
    },
  };
}
