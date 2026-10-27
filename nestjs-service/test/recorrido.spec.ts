import { armarRecorrido } from '../src/motor/recorrido';
import { obra } from './datos';

const sinPrefs = { movimientos: [], tecnicas: [], tematicas: [] };
const pedido = (candidatas, extra = {}) => ({
  candidatas,
  duracion_minutos: 0,
  cantidad_maxima: 0,
  preferencias: sinPrefs,
  ...extra,
});

describe('armarRecorrido', () => {
  it('sin candidatas devuelve paradas vacías y una nota, no un error', () => {
    const r = armarRecorrido(pedido([]));
    expect(r.paradas).toEqual([]);
    expect(r.duracion_total_minutos).toBe(0);
    expect(r.notas).toEqual(['No hay obras en exhibición que cumplan los criterios elegidos']);
  });

  it('suma afinidad: movimiento +3, técnica +2 (contiene), temática +2, y da 6 minutos', () => {
    const prefs = { movimientos: ['impresionismo'], tecnicas: ['OLEO'], tematicas: ['paisaje'] };
    const r = armarRecorrido(pedido([obra(1)], { preferencias: prefs }));
    expect(r.paradas[0].minutos_sugeridos).toBe(6);
    expect(r.paradas[0].motivo).toBe(
      'Coincide con tu interés en Impresionismo y en Óleo sobre lienzo y en Paisaje',
    );
  });

  it('sin afinidad da 4 minutos y el motivo genérico', () => {
    const r = armarRecorrido(pedido([obra(1)]));
    expect(r.paradas[0].minutos_sugeridos).toBe(4);
    expect(r.paradas[0].motivo).toBe('Cumple los criterios que elegiste');
  });

  it('con afinidad por una sola preferencia el motivo nombra solo esa', () => {
    const r = armarRecorrido(pedido([obra(1)], { preferencias: { ...sinPrefs, movimientos: ['Impresionismo'] } }));
    expect(r.paradas[0].motivo).toBe('Coincide con tu interés en Impresionismo');
  });

  it('prioriza por afinidad y, a igual afinidad, por año (0 al final) y luego id', () => {
    const candidatas = [
      obra(1, { anio_creacion: 0, movimiento: 'Cubismo', ubicacion: 'A' }),
      obra(2, { anio_creacion: 1950, movimiento: 'Cubismo', ubicacion: 'A' }),
      obra(3, { anio_creacion: 1980, movimiento: 'Barroco', ubicacion: 'A' }),
    ];
    // Con cantidad 2 quedan las dos primeras por prioridad: la de afinidad (3) y la de año conocido.
    const r = armarRecorrido(pedido(candidatas, { cantidad_maxima: 2, preferencias: { ...sinPrefs, movimientos: ['barroco'] } }));
    expect(r.paradas.map((p) => p.obra_id)).toEqual([2, 3]);
  });

  it('respeta la duración contando los traslados entre salas', () => {
    const candidatas = [
      obra(1, { ubicacion: 'Sala A', anio_creacion: 1900 }),
      obra(2, { ubicacion: 'Sala B', anio_creacion: 1910 }),
      obra(3, { ubicacion: 'Sala C', anio_creacion: 1920 }),
    ];
    // 4 + 2 + 4 = 10 entra justo; sumar la tercera daría 16.
    const r = armarRecorrido(pedido(candidatas, { duracion_minutos: 10 }));
    expect(r.paradas.map((p) => p.obra_id)).toEqual([1, 2]);
    expect(r.duracion_total_minutos).toBe(10);
    expect(r.notas).toEqual(['Quedó 1 obra afuera para no pasarte de 10 minutos']);
  });

  it('una obra que no entra no impide que entre una posterior de la misma sala', () => {
    const candidatas = [
      obra(1, { ubicacion: 'Sala A', anio_creacion: 1900 }),
      obra(2, { ubicacion: 'Sala B', anio_creacion: 1910 }), // 4 + 2 + 4 = 10 > 8
      obra(3, { ubicacion: 'Sala A', anio_creacion: 1920 }), // 4 + 4 = 8
    ];
    const r = armarRecorrido(pedido(candidatas, { duracion_minutos: 8 }));
    expect(r.paradas.map((p) => p.obra_id)).toEqual([1, 3]);
    expect(r.duracion_total_minutos).toBe(8);
  });

  it('duración 0 significa sin límite', () => {
    const candidatas = [1, 2, 3, 4, 5].map((i) => obra(i, { ubicacion: `Sala ${i}` }));
    const r = armarRecorrido(pedido(candidatas));
    expect(r.paradas).toHaveLength(5);
    expect(r.duracion_total_minutos).toBe(5 * 4 + 4 * 2);
    expect(r.notas).toEqual([]);
  });

  it('respeta la cantidad máxima y lo avisa en las notas', () => {
    const candidatas = [1, 2, 3, 4].map((i) => obra(i));
    const r = armarRecorrido(pedido(candidatas, { cantidad_maxima: 2 }));
    expect(r.paradas).toHaveLength(2);
    expect(r.notas).toEqual(['Quedaron 2 obras afuera por el límite de 2 obras']);
  });

  it('ordena las paradas por sala alfabética y, dentro de la sala, por año', () => {
    const candidatas = [
      obra(1, { ubicacion: 'Sala B', anio_creacion: 1800 }),
      obra(2, { ubicacion: 'Sala A', anio_creacion: 1900 }),
      obra(3, { ubicacion: 'Sala A', anio_creacion: 1850 }),
      obra(4, { ubicacion: 'Sala B', anio_creacion: 1700 }),
    ];
    const r = armarRecorrido(pedido(candidatas));
    expect(r.paradas.map((p) => p.obra_id)).toEqual([3, 2, 4, 1]);
    expect(r.paradas.map((p) => p.orden)).toEqual([1, 2, 3, 4]);
    expect(r.paradas.map((p) => p.sala)).toEqual(['Sala A', 'Sala A', 'Sala B', 'Sala B']);
    // 4 obras * 4 min + un solo cambio de sala
    expect(r.duracion_total_minutos).toBe(18);
  });

  it('rechaza límites negativos', () => {
    expect(() => armarRecorrido(pedido([obra(1)], { duracion_minutos: -1 }))).toThrow();
  });
});
