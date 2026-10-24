import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { join } from 'path';
import { AppModule } from './app.module';

async function bootstrap() {
  // Puerto del servidor gRPC
  const grpcPort = process.env.GRPC_PORT || 9091;
  // Los .proto viven en la raíz del repo, compartidos con los servicios Java;
  // desde dist/ son dos niveles arriba. PROTO_DIR permite otra ubicación (Docker).
  const dirProto = process.env.PROTO_DIR || join(__dirname, '../../proto');

  const app = await NestFactory.createMicroservice<MicroserviceOptions>(AppModule, {
    transport: Transport.GRPC,
    options: {
      package: [
        'museo.recomendaciones',
        'museo.usuarios',
        'museo.common',
        'museo.catalogo'
      ],
      protoPath: [
        join(dirProto, 'recomendaciones.proto'),
        join(dirProto, 'usuarios.proto'),
        join(dirProto, 'common.proto'),
        join(dirProto, 'catalogo.proto'),
      ],
      url: `0.0.0.0:${grpcPort}`,
      loader: {
        arrays: true,
        keepCase: true,
        longs: String,
        enums: String,
        defaults: true,
        oneofs: true,
      },
    },
  });

  await app.listen();
  console.log(`🚀 NestJS gRPC Microservicio corriendo en puerto ${grpcPort}`);
  console.log('📦 Paquetes cargados: recomendaciones, usuarios, common, catalogo');
}

bootstrap();
