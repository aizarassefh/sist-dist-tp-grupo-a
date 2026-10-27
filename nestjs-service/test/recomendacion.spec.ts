import { recomendar } from '../src/motor/recomendacion';
import { obra } from './datos';

const sinPrefs = { movimientos: [], tecnicas: [], tematicas: [] };
const pedido = (extra = {}) => ({
  candidatas: [],
  preferencias: sinPrefs,
  interacciones: [],
  obras_con_interaccion: [],
  eventos_inscriptos: [],
  cantidad: 0,
  ...extra,
});

describe('recomendar', () => {
  it('suma preferencias de movimiento, técnica y temática con sus motivos', () => {
    const r = recomendar(
      pedido({
        candidatas: [obra(1)],
        preferencias: { movimientos: ['impresionismo'], tecnicas: ['oleo'], tematicas: ['PAISAJE'] },
      }),
    );
    expect(r.criterio).toBe('PERSONALIZADO');
    expect(r.recomendaciones[0].puntaje).toBe(7);
    expect(r.recomendaciones[0].motivos).toEqual([
      'Es del Impresionismo, uno de tus movimientos preferidos',
      'Su técnica (Óleo sobre lienzo) es una de las que preferís',
      'Trata el tema Paisaje, uno de tus temas preferidos',
    ]);
  });

  it('excluye las obras con cualquier interacción, vista o de interés', () => {
    const r = recomendar(
      pedido({
        candidatas: [obra(1), obra(2), obra(3)],
        preferencias: { ...sinPrefs, movimientos: ['Impresionismo'] },
        interacciones: [
          { obra_id: 1, tipo: 'VISTA' },
          { obra_id: 2, tipo: 'INTERES' },
        ],
        obras_con_interaccion: [obra(1), obra(2)],
      }),
    );
    expect(r.recomendaciones.map((x) => x.obra_id)).toEqual([3]);
  });

  it('suma señales de interacciones: mismo artista (+2) y movimiento de interés (+1.5)', () => {
    const monet = { id: 7, nombre: 'Claude Monet' };
    const r = recomendar(
      pedido({
        candidatas: [obra(10, { artista: monet, movimiento: 'Impresionismo' })],
        interacciones: [{ obra_id: 1, tipo: 'INTERES' }],
        obras_con_interaccion: [obra(1, { artista: monet, movimiento: 'Impresionismo' })],
      }),
    );
    expect(r.recomendaciones[0].puntaje).toBe(3.5);
    expect(r.recomendaciones[0].motivos).toEqual([
      'Es de Claude Monet, como otra obra que te interesó',
      'Es del Impresionismo, igual que una obra que te interesó',
    ]);
  });

  it('una obra vista aporta +1 por movimiento pero no por artista', () => {
    const monet = { id: 7, nombre: 'Claude Monet' };
    const r = recomendar(
      pedido({
        candidatas: [obra(10, { artista: monet })],
        interacciones: [{ obra_id: 1, tipo: 'VISTA' }],
        obras_con_interaccion: [obra(1, { artista: monet })],
      }),
    );
    expect(r.recomendaciones[0].puntaje).toBe(1);
    expect(r.recomendaciones[0].motivos).toEqual(['Es del Impresionismo, como una obra que ya viste']);
  });

  it('suma +1 si el movimiento coincide con un evento inscripto', () => {
    const r = recomendar(
      pedido({
        candidatas: [obra(1, { movimiento: 'Cubismo' })],
        eventos_inscriptos: [{ id: 5, movimiento: 'cubismo' }],
      }),
    );
    expect(r.recomendaciones[0].puntaje).toBe(1);
    expect(r.recomendaciones[0].motivos).toEqual(['Te anotaste a un evento sobre cubismo']);
  });

  it('ordena por puntaje desc y luego id asc, y corta en la cantidad pedida', () => {
    const r = recomendar(
      pedido({
        candidatas: [
          obra(3, { movimiento: 'Cubismo' }),
          obra(2, { movimiento: 'Impresionismo' }),
          obra(1, { movimiento: 'Impresionismo' }),
          obra(4, { movimiento: 'Barroco', tecnica: 'Fresco' }),
        ],
        preferencias: { movimientos: ['Impresionismo'], tecnicas: ['fresco'], tematicas: [] },
        cantidad: 2,
      }),
    );
    // 1 y 2 empatan en 3 puntos (obra 4 también suma 2): quedan 1 y 2.
    expect(r.recomendaciones.map((x) => x.obra_id)).toEqual([1, 2]);
  });

  it('cantidad 0 usa 6 por defecto', () => {
    const candidatas = Array.from({ length: 9 }, (_, i) => obra(i + 1));
    const r = recomendar(pedido({ candidatas, preferencias: { ...sinPrefs, movimientos: ['Impresionismo'] } }));
    expect(r.recomendaciones).toHaveLength(6);
  });

  it('sin puntaje positivo pasa a EXPLORATORIO: la obra más antigua de cada movimiento', () => {
    const r = recomendar(
      pedido({
        candidatas: [
          obra(1, { movimiento: 'Barroco', anio_creacion: 1650 }),
          obra(2, { movimiento: 'Barroco', anio_creacion: 1600 }),
          obra(3, { movimiento: 'Cubismo', anio_creacion: 1910 }),
          obra(4, { movimiento: 'Renacimiento', anio_creacion: 1500 }),
        ],
        cantidad: 2,
      }),
    );
    expect(r.criterio).toBe('EXPLORATORIO');
    expect(r.recomendaciones.map((x) => x.obra_id)).toEqual([4, 2]);
    expect(r.recomendaciones[0].puntaje).toBe(0);
    expect(r.recomendaciones[0].motivos).toEqual(['Para empezar a conocer el Renacimiento']);
  });

  it('sin candidatas devuelve EXPLORATORIO vacío', () => {
    const r = recomendar(pedido());
    expect(r).toEqual({ recomendaciones: [], criterio: 'EXPLORATORIO' });
  });
});
