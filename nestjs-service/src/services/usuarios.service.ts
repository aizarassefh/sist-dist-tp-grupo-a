import { Injectable, NotFoundException } from '@nestjs/common';

/**
 * Interfaz para usuario
 */
interface Usuario {
  id: number;
  email: string;
  nombre: string;
  apellido: string;
  rol: string;
  preferencias: PreferenciasUsuario;
  fechaRegistro: string;
}

/**
 * Interfaz para preferencias
 */
interface PreferenciasUsuario {
  estilosFavoritos: string[];
  tecnicasFavoritas: string[];
  artistasFavoritos: number[];
  temasInteres: string[];
  umbralCalificacion: number;
}

/**
 * Interfaz para interacción
 */
interface Interaccion {
  id: number;
  usuarioId: number;
  obraId: number;
  tipoInteraccion: string;
  duracionSegundos: number;
  fecha: string;
}

/**
 * Servicio de Usuarios para NestJS.
 * Gestiona perfiles, preferencias e interacciones de usuarios.
 */
@Injectable()
export class UsuariosService {

  private usuarios: Map<number, Usuario> = new Map();
  private interacciones: Map<number, Interaccion[]> = new Map();
  private obrasFavoritas: Map<number, number[]> = new Map();
  private nextId = 1;
  private nextInteraccionId = 1;

  constructor() {
    this.inicializarUsuariosMock();
  }

  /**
   * Obtiene un usuario por ID o email
   */
  async obtenerUsuario(data: any): Promise<any> {
    const { id, email } = data;

    let usuario: Usuario | undefined;

    if (id) {
      usuario = this.usuarios.get(id);
    } else if (email) {
      usuario = Array.from(this.usuarios.values()).find(u => u.email === email);
    }

    if (!usuario) {
      throw new NotFoundException('Usuario no encontrado');
    }

    return {
      id: usuario.id,
      email: usuario.email,
      nombre: usuario.nombre,
      apellido: usuario.apellido,
      rol: usuario.rol,
      preferencias: this.convertirPreferencias(usuario.preferencias),
      fechaRegistro: usuario.fechaRegistro
    };
  }

  /**
   * Actualiza las preferencias de un usuario
   */
  async actualizarPreferencias(data: any): Promise<any> {
    const { usuarioId, estilosFavoritos, tecnicasFavoritas, artistasFavoritos, temasInteres } = data;

    const usuario = this.usuarios.get(usuarioId);
    if (!usuario) {
      throw new NotFoundException(`Usuario no encontrado: ${usuarioId}`);
    }

    if (estilosFavoritos) {
      usuario.preferencias.estilosFavoritos = estilosFavoritos;
    }
    if (tecnicasFavoritas) {
      usuario.preferencias.tecnicasFavoritas = tecnicasFavoritas;
    }
    if (artistasFavoritos) {
      usuario.preferencias.artistasFavoritos = artistasFavoritos;
    }
    if (temasInteres) {
      usuario.preferencias.temasInteres = temasInteres;
    }

    this.usuarios.set(usuarioId, usuario);

    return {
      id: usuario.id,
      email: usuario.email,
      nombre: usuario.nombre,
      apellido: usuario.apellido,
      rol: usuario.rol,
      preferencias: this.convertirPreferencias(usuario.preferencias),
      fechaRegistro: usuario.fechaRegistro
    };
  }

  /**
   * Obtiene las preferencias de un usuario
   */
  async obtenerPreferencias(data: any): Promise<any> {
    const { usuarioId } = data;

    const usuario = this.usuarios.get(usuarioId);
    if (!usuario) {
      throw new NotFoundException(`Usuario no encontrado: ${usuarioId}`);
    }

    return this.convertirPreferencias(usuario.preferencias);
  }

  /**
   * Registra una interacción del usuario
   */
  async registrarInteraccion(data: any): Promise<any> {
    const { usuarioId, obraId, tipoInteraccion, duracionSegundos } = data;

    const interaccion: Interaccion = {
      id: this.nextInteraccionId++,
      usuarioId,
      obraId,
      tipoInteraccion,
      duracionSegundos: duracionSegundos || 0,
      fecha: new Date().toISOString()
    };

    const interaccionesUsuario = this.interacciones.get(usuarioId) || [];
    interaccionesUsuario.push(interaccion);
    this.interacciones.set(usuarioId, interaccionesUsuario);

    // Agregar a favoritos si es del tipo FAVORITO
    if (tipoInteraccion === 'FAVORITO') {
      const favoritos = this.obrasFavoritas.get(usuarioId) || [];
      if (!favoritos.includes(obraId)) {
        favoritos.push(obraId);
        this.obrasFavoritas.set(usuarioId, favoritos);
      }
    }

    return {
      exito: true,
      mensaje: 'Interacción registrada correctamente'
    };
  }

  /**
   * Obtiene el historial de visitas de un usuario
   */
  async obtenerHistorialVisitas(data: any): Promise<any> {
    const { usuarioId, diasAtras = 30 } = {
      usuarioId: data.usuarioId,
      diasAtras: data.diasAtras
    };

    const interaccionesUsuario = this.interacciones.get(usuarioId) || [];

    const fechaDesde = new Date();
    fechaDesde.setDate(fechaDesde.getDate() - diasAtras);

    const interaccionesFiltradas = interaccionesUsuario.filter(i => {
      const fechaInteraccion = new Date(i.fecha);
      return fechaInteraccion >= fechaDesde || diasAtras <= 0;
    });

    const resultado = interaccionesFiltradas.map(i => ({
      id: i.id,
      obraId: i.obraId,
      tipoInteraccion: i.tipoInteraccion,
      duracionSegundos: i.duracionSegundos,
      fecha: i.fecha
    }));

    return {
      interacciones: resultado,
      total: resultado.length
    };
  }

  /**
   * Obtiene las obras favoritas de un usuario
   */
  async obtenerObrasFavoritas(data: any): Promise<any> {
    const { usuarioId } = data;

    const usuario = this.usuarios.get(usuarioId);
    if (!usuario) {
      return { obraIds: [] };
    }

    const favoritos = this.obrasFavoritas.get(usuarioId) || [];

    return {
      obraIds: favoritos.map(id => id.toString())
    };
  }

  /**
   * Convierte preferencias al formato gRPC
   */
  private convertirPreferencias(pref: PreferenciasUsuario): any {
    return {
      estilosFavoritos: pref.estilosFavoritos,
      tecnicasFavoritas: pref.tecnicasFavoritas,
      artistasFavoritos: pref.artistasFavoritos.map(id => id.toString()),
      temasInteres: pref.temasInteres,
      umbralCalificacion: pref.umbralCalificacion
    };
  }

  /**
   * Inicializa usuarios de ejemplo
   */
  private inicializarUsuariosMock(): void {
    const roles = ['VISITANTE', 'CURADOR', 'ADMINISTRADOR'];
    const nombres = ['Juan', 'María', 'Pedro', 'Laura', 'Carlos', 'Ana', 'Luis', 'Sofía'];
    const apellidos = ['García', 'Rodríguez', 'López', 'Martínez', 'Pérez', 'Gómez', 'Sánchez'];

    for (let i = 1; i <= 8; i++) {
      const estilosDisponibles = ['Impresionismo', 'Cubismo', 'Surrealismo', 'Realismo', 'Barroco', 'Renacimiento'];
      const tecnicasDisponibles = ['Óleo', 'Acuarela', 'Témpera', 'Escultura', 'Collage', 'Fresco'];

      const estilosFavoritos: string[] = [];
      const tecnicasFavoritas: string[] = [];

      // Agregar 2-3 estilos aleatorios
      for (let j = 0; j < 2 + Math.floor(Math.random() * 2); j++) {
        const estilo = estilosDisponibles[Math.floor(Math.random() * estilosDisponibles.length)];
        if (!estilosFavoritos.includes(estilo)) {
          estilosFavoritos.push(estilo);
        }
      }

      // Agregar 1-2 técnicas aleatorias
      for (let j = 0; j < 1 + Math.floor(Math.random() * 2); j++) {
        const tecnica = tecnicasDisponibles[Math.floor(Math.random() * tecnicasDisponibles.length)];
        if (!tecnicasFavoritas.includes(tecnica)) {
          tecnicasFavoritas.push(tecnica);
        }
      }

      const usuario: Usuario = {
        id: i,
        email: `usuario${i}@museo.com`,
        nombre: nombres[i - 1],
        apellido: apellidos[Math.floor(Math.random() * apellidos.length)],
        rol: i <= 3 ? roles[i - 1] : 'VISITANTE',
        preferencias: {
          estilosFavoritos,
          tecnicasFavoritas,
          artistasFavoritos: [1, 2, 3].filter(() => Math.random() > 0.3),
          temasInteres: ['Paisajes', 'Retratos', 'Histórico'].filter(() => Math.random() > 0.5),
          umbralCalificacion: parseFloat((3 + Math.random() * 2).toFixed(1))
        },
        fechaRegistro: new Date(Date.now() - Math.random() * 365 * 24 * 60 * 60 * 1000).toISOString()
      };

      this.usuarios.set(i, usuario);
    }

    // Inicializar interacciones de ejemplo
    this.interacciones.set(1, [
      {
        id: this.nextInteraccionId++,
        usuarioId: 1,
        obraId: 1,
        tipoInteraccion: 'VISITA',
        duracionSegundos: 300,
        fecha: new Date().toISOString()
      },
      {
        id: this.nextInteraccionId++,
        usuarioId: 1,
        obraId: 2,
        tipoInteraccion: 'FAVORITO',
        duracionSegundos: 0,
        fecha: new Date().toISOString()
      }
    ]);

    this.obrasFavoritas.set(1, [2, 5, 8]);
    this.obrasFavoritas.set(2, [1, 3, 7, 9]);
  }
}
