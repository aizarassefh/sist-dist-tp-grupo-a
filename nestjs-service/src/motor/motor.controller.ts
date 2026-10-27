import { Controller } from '@nestjs/common';
import { GrpcMethod, RpcException } from '@nestjs/microservices';
import { status } from '@grpc/grpc-js';
import { armarRecorrido } from './recorrido';
import { recomendar } from './recomendacion';
import { compararObras } from './comparacion';
import { ArgumentoInvalidoError } from './errores';
import {
  ArmarRecorridoRequest,
  Comparacion,
  CompararObrasRequest,
  RecomendacionesResponse,
  RecomendarRequest,
  Recorrido,
} from './tipos';

// Delgado a propósito: toda la lógica está en funciones puras testeables sin
// levantar Nest ni gRPC. Acá solo se delega y se traducen los errores.
function conErrores<T>(calculo: () => T): T {
  try {
    return calculo();
  } catch (e) {
    if (e instanceof ArgumentoInvalidoError) {
      throw new RpcException({ code: status.INVALID_ARGUMENT, message: e.message });
    }
    throw e;
  }
}

@Controller()
export class MotorController {
  @GrpcMethod('MotorService', 'ArmarRecorrido')
  armarRecorrido(req: ArmarRecorridoRequest): Recorrido {
    return conErrores(() => armarRecorrido(req));
  }

  @GrpcMethod('MotorService', 'Recomendar')
  recomendar(req: RecomendarRequest): RecomendacionesResponse {
    return conErrores(() => recomendar(req));
  }

  @GrpcMethod('MotorService', 'CompararObras')
  compararObras(req: CompararObrasRequest): Comparacion {
    return conErrores(() => compararObras(req));
  }
}
