import { Obra } from '../src/motor/tipos';

// Arma una obra con los defaults que entrega el loader (defaults: true).
export function obra(id: number, campos: Partial<Obra> = {}): Obra {
  return {
    id,
    titulo: `Obra ${id}`,
    descripcion: '',
    artista: { id: 100 + id, nombre: `Artista ${id}` },
    anio_creacion: 1900 + id,
    tecnica: 'Óleo sobre lienzo',
    tematica: 'Paisaje',
    movimiento: 'Impresionismo',
    dimensiones: '50 x 60 cm',
    ubicacion: 'Sala 1',
    en_exhibicion: true,
    imagen_url: '',
    ...campos,
  };
}
