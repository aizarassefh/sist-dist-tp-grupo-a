import { compararObras } from '../src/motor/comparacion';
import { ArgumentoInvalidoError } from '../src/motor/errores';
import { obra } from './datos';

describe('compararObras', () => {
  it('con menos de dos obras lanza ArgumentoInvalidoError', () => {
    expect(() => compararObras({ obras: [obra(1)] })).toThrow(ArgumentoInvalidoError);
    expect(() => compararObras({ obras: [] })).toThrow('Se necesitan al menos dos obras para comparar');
  });

  it('compara tres obras con los 8 atributos en orden y valores en el orden recibido', () => {
    const monet = { id: 1, nombre: 'Claude Monet' };
    const r = compararObras({
      obras: [
        obra(1, { artista: monet, anio_creacion: 1872, ubicacion: 'Sala 1' }),
        obra(2, { artista: monet, anio_creacion: 1900, ubicacion: 'Sala 1', tecnica: 'ÓLEO SOBRE LIENZO' }),
        obra(3, { artista: monet, anio_creacion: 0, ubicacion: 'Sala 2', en_exhibicion: false }),
      ],
    });
    expect(r.atributos.map((a) => a.nombre)).toEqual([
      'Artista', 'Movimiento', 'Año', 'Técnica', 'Temática', 'Dimensiones', 'Ubicación', 'En exhibición',
    ]);
    const por = Object.fromEntries(r.atributos.map((a) => [a.nombre, a]));
    expect(por['Año'].valores).toEqual(['1872', '1900', 'Sin dato']);
    expect(por['Año'].coinciden).toBe(false);
    expect(por['Técnica'].coinciden).toBe(true); // iguales sin mayúsculas
    expect(por['Ubicación'].coinciden).toBe(false);
    expect(por['En exhibición'].valores).toEqual(['Sí', 'Sí', 'No']);
    expect(r.observaciones).toEqual([
      'Entre la más antigua y la más reciente hay 28 años',
      'Todas son de Claude Monet',
      'Comparten movimiento: Impresionismo',
      'Coinciden en 5 de 8 atributos',
    ]);
  });

  it('informa artistas distintos y sala compartida', () => {
    const r = compararObras({
      obras: [obra(1, { movimiento: 'Barroco' }), obra(2, { movimiento: 'Cubismo' })],
    });
    expect(r.observaciones).toContain('Hay 2 artistas distintos');
    expect(r.observaciones).toContain('Están en la misma sala: Sala 1');
    expect(r.observaciones.some((o) => o.startsWith('Comparten movimiento'))).toBe(false);
  });

  it('no inventa la diferencia de años si falta el dato', () => {
    const r = compararObras({ obras: [obra(1, { anio_creacion: 0 }), obra(2, { anio_creacion: 1900 })] });
    expect(r.observaciones.some((o) => o.startsWith('Entre la más antigua'))).toBe(false);
  });
});
