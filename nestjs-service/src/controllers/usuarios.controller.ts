import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import { UsuariosService } from '../services/usuarios.service';

/**
 * Controlador gRPC para el servicio de Usuarios.
 * Implementa los métodos definidos en usuarios.proto
 */
@Controller()
export class UsuariosController {
  constructor(private readonly usuariosService: UsuariosService) {}

  /**
   * Obtiene un usuario por ID o email
   */
  @GrpcMethod('UsuariosService', 'ObtenerUsuario')
  obtenerUsuario(data: any): Promise<any> {
    return this.usuariosService.obtenerUsuario(data);
  }

  /**
   * Actualiza las preferencias de un usuario
   */
  @GrpcMethod('UsuariosService', 'ActualizarPreferencias')
  actualizarPreferencias(data: any): Promise<any> {
    return this.usuariosService.actualizarPreferencias(data);
  }

  /**
   * Obtiene las preferencias de un usuario
   */
  @GrpcMethod('UsuariosService', 'ObtenerPreferencias')
  obtenerPreferencias(data: any): Promise<any> {
    return this.usuariosService.obtenerPreferencias(data);
  }

  /**
   * Registra una interacción del usuario con una obra
   */
  @GrpcMethod('UsuariosService', 'RegistrarInteraccion')
  registrarInteraccion(data: any): Promise<any> {
    return this.usuariosService.registrarInteraccion(data);
  }

  /**
   * Obtiene el historial de visitas de un usuario
   */
  @GrpcMethod('UsuariosService', 'ObtenerHistorialVisitas')
  obtenerHistorialVisitas(data: any): Promise<any> {
    return this.usuariosService.obtenerHistorialVisitas(data);
  }

  /**
   * Obtiene las obras favoritas de un usuario
   */
  @GrpcMethod('UsuariosService', 'ObtenerObrasFavoritas')
  obtenerObrasFavoritas(data: any): Promise<any> {
    return this.usuariosService.obtenerObrasFavoritas(data);
  }
}
