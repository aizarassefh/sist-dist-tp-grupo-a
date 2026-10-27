import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions } from '@nestjs/microservices';
import { AppModule } from './app.module';
import { opcionesGrpc } from './grpc.config';

async function bootstrap() {
  const grpcPort = process.env.GRPC_PORT || 9091;

  const app = await NestFactory.createMicroservice<MicroserviceOptions>(AppModule, opcionesGrpc(grpcPort));

  await app.listen();
  console.log(`Motor de experiencias (gRPC, museo.motor) escuchando en el puerto ${grpcPort}`);
}

bootstrap();
