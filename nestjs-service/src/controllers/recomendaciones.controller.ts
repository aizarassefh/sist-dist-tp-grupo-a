import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import { RecomendacionesService } from '../services/recomendaciones.service';

/**
 * Controlador gRPC para el servicio de Recomendaciones.
 * Implementa los métodos definidos en recomendaciones.proto
 */
@Controller()
export class RecomendacionesController {
  constructor(private readonly recomendacionesService: RecomendacionesService) {}

  /**
   * Obtiene recomendaciones personalizadas para un usuario
   */
  @GrpcMethod('RecomendacionesService', 'ObtenerRecomendaciones')
  obtenerRecomendaciones(data: any): Promise<any> {
    return this.recomendacionesService.obtenerRecomendaciones(data);
  }

  /**
   * Obtiene recomendaciones basadas en usuarios similares (filtrado colaborativo)
   */
  @GrpcMethod('RecomendacionesService', 'ObtenerRecomendacionesColaborativas')
  obtenerRecomendacionesColaborativas(data: any): Promise<any> {
    return this.recomendacionesService.obtenerRecomendacionesColaborativas(data);
  }

  /**
   * Obtiene las obras más populares del período
   */
  @GrpcMethod('RecomendacionesService', 'ObtenerObrasPopulares')
  obtenerObrasPopulares(data: any): Promise<any> {
    return this.recomendacionesService.obtenerObrasPopulares(data);
  }

  /**
   * Obtiene obras en tendencia (con aumento reciente en visitas)
   */
  @GrpcMethod('RecomendacionesService', 'ObtenerObrasTendencia')
  obtenerObrasTendencia(data: any): Promise<any> {
    return this.recomendacionesService.obtenerObrasTendencia(data);
  }

  /**
   * Obtiene sugerencias contextuales basadas en la obra actual
   */
  @GrpcMethod('RecomendacionesService', 'ObtenerSugerenciasContextuales')
  obtenerSugerenciasContextuales(data: any): Promise<any> {
    return this.recomendacionesService.obtenerSugerenciasContextuales(data);
  }
}
