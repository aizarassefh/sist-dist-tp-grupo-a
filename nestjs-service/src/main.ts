import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { join } from 'path';
import { AppModule } from './app.module';

async function bootstrap() {
  // Puerto del servidor gRPC
  const grpcPort = process.env.GRPC_PORT || 9091;

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
        join(__dirname, '../proto/recomendaciones.proto'),
        join(__dirname, '../proto/usuarios.proto'),
        join(__dirname, '../proto/common.proto'),
        join(__dirname, '../proto/catalogo.proto'),
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
