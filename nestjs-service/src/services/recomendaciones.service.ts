import { Injectable } from '@nestjs/common';

/**
 * Servicio de Recomendaciones para NestJS.
 * Implementa lógica de recomendaciones colaborativas y basadas en contenido.
 */
@Injectable()
export class RecomendacionesService {

  // Mock de datos de obras (en producción esto vendría de una base de datos)
  private obras: Map<string, any> = new Map();
  private usuarios: Map<string, any> = new Map();
  private interacciones: Map<string, any[]> = new Map();

  constructor() {
    this.inicializarDatosMock();
  }

  /**
   * Obtiene recomendaciones personalizadas para un usuario
   */
  async obtenerRecomendaciones(data: any): Promise<any> {
    const { usuarioId, cantidad = 10, excluirObrasIds = [], contexto = 'COLECCION' } = data;

    const usuario = this.usuarios.get(String(usuarioId));
    const preferencias = usuario?.preferencias || {};

    // Filtrar obras
    let candidatas = Array.from(this.obras.values())
      .filter(obra => !excluirObrasIds.includes(String(obra.id)));

    // Calcular scores según preferencias
    const recomendaciones: any[] = [];

    for (const obra of candidatas) {
      let score = 0;
      let razones: string[] = [];

      // Estilos favoritos (peso: 30)
      if (preferencias.estilosFavoritos?.includes(obra.estilo)) {
        score += 30;
        razones.push(`Coincide con tu interés en ${obra.estilo}`);
      }

      // Técnicas favoritas (peso: 25)
      if (preferencias.tecnicasFavoritas?.includes(obra.tecnica)) {
        score += 25;
        razones.push(`Utiliza ${obra.tecnica}, una de tus técnicas favoritas`);
      }

      // Artistas favoritos (peso: 40)
      if (preferencias.artistasFavoritos?.includes(String(obra.artistaId))) {
        score += 40;
        razones.push('De un artista que sigues');
      }

      // Calificación (peso: 10)
      if (obra.calificacionPromedio >= 4.5) {
        score += 10;
        razones.push('Altamente valorada por otros visitantes');
      }

      // Popularidad
      score += (obra.visitas / 500);

      if (score > 0) {
        recomendaciones.push({
          obra: this.convertirObra(obra),
          score: Math.min(score, 100),
          razon: razones[0] || 'Basada en tus preferencias',
          tipoRecomendacion: 'PERSONAL'
        });
      }
    }

    // Ordenar por score
    recomendaciones.sort((a, b) => b.score - a.score);

    return {
      recomendaciones: recomendaciones.slice(0, cantidad),
      preferenciasUsadas: preferencias,
      totalDisponibles: recomendaciones.length
    };
  }

  /**
   * Recomendaciones colaborativas (filtrado colaborativo)
   */
  async obtenerRecomendacionesColaborativas(data: any): Promise<any> {
    const { usuarioId, cantidad = 10, usuariosSimilares = 5 } = data;

    // Algoritmo simplificado de filtrado colaborativo
    // En producción se usaría una matriz de factorización o similaridad coseno

    const usuarioActual = this.usuarios.get(String(usuarioId));
    if (!usuarioActual) {
      return { recomendaciones: [], totalDisponibles: 0 };
    }

    // Simular similitudes entre usuarios
    const recomendaciones: any[] = [];
    const obrasVistas = this.obtenerObrasVistasPorUsuario(String(usuarioId));

    // Usuarios "similares" habrían visto estas obras
    const obrasSimilares = Array.from(this.obras.values())
      .filter(obra => !obrasVistas.includes(String(obra.id)))
      .slice(0, cantidad);

    for (const obra of obrasSimilares) {
      recomendaciones.push({
        obra: this.convertirObra(obra),
        score: 75 + Math.random() * 20, // Simular score
        razon: 'Usuarios con gustos similares vieron esta obra',
        tipoRecomendacion: 'COLABORATIVA'
      });
    }

    return {
      recomendaciones: recomendaciones.slice(0, cantidad),
      totalDisponibles: recomendaciones.length
    };
  }

  /**
   * Obtiene las obras más populares
   */
  async obtenerObrasPopulares(data: any): Promise<any> {
    const { cantidad = 10, periodo = 'mes', filtroEstilo = '' } = data;

    let obras = Array.from(this.obras.values());

    if (filtroEstilo) {
      obras = obras.filter(o => o.estilo === filtroEstilo);
    }

    const populares = obras
      .sort((a, b) => b.visitas - a.visitas)
      .slice(0, cantidad)
      .map((obra, index) => ({
        obra: this.convertirObra(obra),
        visitas: obra.visitas,
        calificacionPromedio: obra.calificacionPromedio,
        posicionRanking: index + 1
      }));

    return {
      obras: populares,
      periodo
    };
  }

  /**
   * Obtiene obras en tendencia (con aumento reciente)
   */
  async obtenerObrasTendencia(data: any): Promise<any> {
    const { cantidad = 10, periodoComparacion = 'semana' } = data;

    // Simular tendencias
    const tendencias = Array.from(this.obras.values())
      .map(obra => ({
        obra: this.convertirObra(obra),
        variacionVisitas: Math.random() * 200 - 50, // -50% a +150%
        nuevosComentarios: Math.floor(Math.random() * 20),
        scoreTendencia: obra.calificacionPromedio * 10 + obra.visitas / 100
      }))
      .sort((a, b) => b.scoreTendencia - a.scoreTendencia)
      .slice(0, cantidad);

    const tipoTendencia = this.detectarTendencia(tendencias);

    return {
      obras: tendencias,
      tendenciaCalculada: tipoTendencia
    };
  }

  /**
   * Sugerencias contextuales basadas en la obra actual
   */
  async obtenerSugerenciasContextuales(data: any): Promise<any> {
    const { usuarioId, obraActualId, cantidad = 5, tipoSugerencia = 'SIGUIENTE_OBRA' } = data;

    const obraActual = this.obras.get(String(obraActualId));
    if (!obraActual) {
      return { recomendaciones: [], totalDisponibles: 0 };
    }

    let sugerencias: any[] = [];

    switch (tipoSugerencia) {
      case 'MISMO_ESTILO':
        sugerencias = Array.from(this.obras.values())
          .filter(o => o.estilo === obraActual.estilo && o.id !== obraActual.id)
          .map(obra => ({
            obra: this.convertirObra(obra),
            score: 85,
            razon: `Del mismo estilo: ${obraActual.estilo}`,
            tipoRecomendacion: 'CONTEXTUAL'
          }));
        break;

      case 'MISMO_ARTISTA':
        sugerencias = Array.from(this.obras.values())
          .filter(o => o.artistaId === obraActual.artistaId && o.id !== obraActual.id)
          .map(obra => ({
            obra: this.convertirObra(obra),
            score: 95,
            razon: `Otra obra del artista`,
            tipoRecomendacion: 'CONTEXTUAL'
          }));
        break;

      case 'COMPLETAR_VISITA':
        // Obras variadas para completar la visita
        sugerencias = Array.from(this.obras.values())
          .filter(o => o.id !== obraActual.id)
          .sort(() => Math.random() - 0.5)
          .slice(0, cantidad)
          .map(obra => ({
            obra: this.convertirObra(obra),
            score: 70,
            razon: 'Para completar tu experiencia',
            tipoRecomendacion: 'COMPLETAR'
          }));
        break;

      case 'SIGUIENTE_OBRA':
      default:
        // Siguiente obra recomendada (mejor calificada)
        sugerencias = Array.from(this.obras.values())
          .filter(o => o.id !== obraActual.id)
          .sort((a, b) => b.calificacionPromedio - a.calificacionPromedio)
          .slice(0, cantidad)
          .map(obra => ({
            obra: this.convertirObra(obra),
            score: obra.calificacionPromedio * 20,
            razon: 'Siguiente obra recomendada',
            tipoRecomendacion: 'SIGUIENTE'
          }));
    }

    return {
      recomendaciones: sugerencias.slice(0, cantidad),
      totalDisponibles: sugerencias.length
    };
  }

  /**
   * Convierte una obra interna al formato gRPC
   */
  private convertirObra(obra: any): any {
    return {
      id: String(obra.id),
      titulo: obra.titulo,
      descripcion: obra.descripcion,
      tecnica: obra.tecnica,
      estilo: obra.estilo,
      imagenUrl: obra.imagenUrl,
      anio: obra.anio,
      artista: obra.artista ? {
        id: String(obra.artista.id),
        nombre: obra.artista.nombre,
        nacionalidad: obra.artista.nacionalidad
      } : undefined,
      visitas: obra.visitas,
      calificacionPromedio: obra.calificacionPromedio
    };
  }

  /**
   * Obtiene las obras vistas por un usuario
   */
  private obtenerObrasVistasPorUsuario(usuarioId: string): string[] {
    const interaccionesUsuario = this.interacciones.get(usuarioId) || [];
    return interaccionesUsuario
      .filter(i => i.tipo === 'VISITA')
      .map(i => String(i.obraId));
  }

  /**
   * Detecta el tipo de tendencia basado en las obras
   */
  private detectarTendencia(obras: any[]): string {
    const estilos = obras.map(o => o.obra.estilo);
    const moda = this.calcularModa(estilos);
    return moda ? `Obras de ${moda} en auge` : 'Obras populares';
  }

  /**
   * Calcula la moda de un array
   */
  private calcularModa(arr: string[]): string | undefined {
    if (arr.length === 0) return undefined;
    const conteo: Record<string, number> = {};
    let maxCount = 0;
    let moda = arr[0];

    for (const item of arr) {
      conteo[item] = (conteo[item] || 0) + 1;
      if (conteo[item] > maxCount) {
        maxCount = conteo[item];
        moda = item;
      }
    }

    return moda;
  }

  /**
   * Inicializa datos mock para testing
   */
  private inicializarDatosMock(): void {
    // Obras de ejemplo
    for (let i = 1; i <= 20; i++) {
      const estilos = ['Impresionismo', 'Cubismo', 'Surrealismo', 'Realismo', 'Abstracto'];
      const tecnicas = ['Óleo', 'Acuarela', 'Escultura', 'Collage', 'Digital'];

      this.obras.set(String(i), {
        id: i,
        titulo: `Obra ${i}`,
        descripcion: `Descripción de la obra ${i}`,
        tecnica: tecnicas[Math.floor(Math.random() * tecnicas.length)],
        estilo: estilos[Math.floor(Math.random() * estilos.length)],
        imagenUrl: `https://museo.com/obras/${i}.jpg`,
        anio: 1800 + Math.floor(Math.random() * 224),
        artistaId: Math.floor(Math.random() * 10) + 1,
        artista: {
          id: Math.floor(Math.random() * 10) + 1,
          nombre: `Artista ${Math.floor(Math.random() * 10) + 1}`,
          nacionalidad: ['Argentina', 'Española', 'Francesa', 'Italiana'][Math.floor(Math.random() * 4)]
        },
        visitas: Math.floor(Math.random() * 5000),
        calificacionPromedio: parseFloat((Math.random() * 2 + 3).toFixed(1))
      });
    }

    // Usuarios de ejemplo
    for (let i = 1; i <= 5; i++) {
      this.usuarios.set(String(i), {
        id: i,
        email: `usuario${i}@example.com`,
        nombre: `Usuario ${i}`,
        apellido: `Apellido ${i}`,
        rol: i === 1 ? 'ADMINISTRADOR' : (i === 2 ? 'CURADOR' : 'VISITANTE'),
        preferencias: {
          estilosFavoritos: ['Impresionismo', 'Cubismo'].filter(() => Math.random() > 0.5),
          tecnicasFavoritas: ['Óleo', 'Acuarela'].filter(() => Math.random() > 0.3),
          artistasFavoritos: [1, 2, 3].filter(() => Math.random() > 0.5),
          temasInteres: ['Naturaleza', 'Retratos'],
          umbralCalificacion: 3.5
        }
      });
    }

    // Interacciones de ejemplo
    this.interacciones.set('1', [
      { obraId: 1, tipo: 'VISITA', fecha: new Date().toISOString() },
      { obraId: 2, tipo: 'FAVORITO', fecha: new Date().toISOString() },
    ]);
  }
}
