// Tipos de los mensajes del contrato (proto/motor.proto y los que importa).
// Reflejan el loader con keepCase, longs: Number, defaults: true: llegan
// siempre completos y en snake_case.

export interface Artista {
  id: number;
  nombre: string;
  biografia?: string;
  nacionalidad?: string;
  movimiento?: string;
  anio_nacimiento?: number;
  anio_fallecimiento?: number;
  cantidad_obras?: number;
}

export interface Obra {
  id: number;
  titulo: string;
  descripcion?: string;
  artista?: Artista;
  anio_creacion: number;
  tecnica: string;
  tematica: string;
  movimiento: string;
  dimensiones: string;
  ubicacion: string;
  en_exhibicion: boolean;
  imagen_url?: string;
}

export interface Preferencias {
  movimientos: string[];
  tecnicas: string[];
  tematicas: string[];
}

export interface Interaccion {
  obra_id: number;
  tipo: string; // VISTA | INTERES
  fecha?: string;
}

export interface Evento {
  id: number;
  titulo?: string;
  movimiento: string;
}

export interface ArmarRecorridoRequest {
  candidatas: Obra[];
  duracion_minutos: number;
  cantidad_maxima: number;
  preferencias?: Preferencias;
}

export interface Parada {
  orden: number;
  obra_id: number;
  sala: string;
  minutos_sugeridos: number;
  motivo: string;
}

export interface Recorrido {
  paradas: Parada[];
  duracion_total_minutos: number;
  notas: string[];
}

export interface RecomendarRequest {
  candidatas: Obra[];
  preferencias?: Preferencias;
  interacciones: Interaccion[];
  obras_con_interaccion: Obra[];
  eventos_inscriptos: Evento[];
  cantidad: number;
}

export interface Recomendacion {
  obra_id: number;
  puntaje: number;
  motivos: string[];
}

export interface RecomendacionesResponse {
  recomendaciones: Recomendacion[];
  criterio: 'PERSONALIZADO' | 'EXPLORATORIO';
}

export interface CompararObrasRequest {
  obras: Obra[];
}

export interface AtributoComparado {
  nombre: string;
  valores: string[];
  coinciden: boolean;
}

export interface Comparacion {
  atributos: AtributoComparado[];
  observaciones: string[];
}
