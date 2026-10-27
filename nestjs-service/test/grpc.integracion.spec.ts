import { createServer } from 'net';
import { join } from 'path';
import { INestMicroservice } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions } from '@nestjs/microservices';
import { credentials, loadPackageDefinition, status } from '@grpc/grpc-js';
import { loadSync } from '@grpc/proto-loader';
import { AppModule } from '../src/app.module';
import { opcionesGrpc } from '../src/grpc.config';
import { obra } from './datos';

const dirProto = join(__dirname, '../../proto');

// Pide un puerto libre al sistema operativo; con puerto fijo el test chocaría
// con el motor real si está corriendo en 9091.
function puertoLibre(): Promise<number> {
  return new Promise((resolve, reject) => {
    const s = createServer();
    s.once('error', reject);
    s.listen(0, '127.0.0.1', () => {
      const { port } = s.address() as { port: number };
      s.close(() => resolve(port));
    });
  });
}

describe('MotorService por gRPC (integración real)', () => {
  let app: INestMicroservice;
  let cliente: any;

  beforeAll(async () => {
    const puerto = await puertoLibre();
    app = await NestFactory.createMicroservice<MicroserviceOptions>(AppModule, {
      ...opcionesGrpc(puerto, dirProto),
      logger: false,
    });
    await app.listen();

    // El cliente carga el .proto por su cuenta, igual que lo haría el gateway Java.
    const definicion = loadSync(join(dirProto, 'motor.proto'), {
      includeDirs: [dirProto],
      keepCase: true,
      longs: Number,
      enums: String,
      defaults: true,
      arrays: true,
    });
    const paquete: any = loadPackageDefinition(definicion);
    cliente = new paquete.museo.motor.MotorService(`127.0.0.1:${puerto}`, credentials.createInsecure());
  });

  afterAll(async () => {
    cliente?.close();
    await app?.close();
  });

  const llamar = (metodo: string, pedido: object): Promise<any> =>
    new Promise((resolve, reject) =>
      cliente[metodo](pedido, (err: any, res: any) => (err ? reject(err) : resolve(res))),
    );

  it('ArmarRecorrido', async () => {
    const r = await llamar('ArmarRecorrido', {
      candidatas: [obra(1, { ubicacion: 'Sala B' }), obra(2, { ubicacion: 'Sala A' })],
      duracion_minutos: 60,
      cantidad_maxima: 0,
      preferencias: { movimientos: ['Impresionismo'], tecnicas: [], tematicas: [] },
    });
    expect(r.paradas.map((p: any) => p.obra_id)).toEqual([2, 1]);
    expect(r.paradas[0]).toMatchObject({ orden: 1, sala: 'Sala A', minutos_sugeridos: 6 });
    expect(r.duracion_total_minutos).toBe(14);
  });

  it('Recomendar', async () => {
    const r = await llamar('Recomendar', {
      candidatas: [obra(1), obra(2, { movimiento: 'Cubismo' })],
      preferencias: { movimientos: ['Cubismo'], tecnicas: [], tematicas: [] },
      interacciones: [],
      obras_con_interaccion: [],
      eventos_inscriptos: [],
      cantidad: 0,
    });
    expect(r.criterio).toBe('PERSONALIZADO');
    expect(r.recomendaciones).toHaveLength(1);
    expect(r.recomendaciones[0]).toMatchObject({ obra_id: 2, puntaje: 3 });
  });

  it('CompararObras', async () => {
    const r = await llamar('CompararObras', { obras: [obra(1), obra(2)] });
    expect(r.atributos).toHaveLength(8);
    expect(r.atributos[0].nombre).toBe('Artista');
    expect(r.observaciones.length).toBeGreaterThan(0);
  });

  it('CompararObras con una sola obra responde INVALID_ARGUMENT', async () => {
    await expect(llamar('CompararObras', { obras: [obra(1)] })).rejects.toMatchObject({
      code: status.INVALID_ARGUMENT,
      details: 'Se necesitan al menos dos obras para comparar',
    });
  });
});
