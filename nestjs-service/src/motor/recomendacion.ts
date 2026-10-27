import { compararAnio, contieneAlguna, estaEn, normalizar } from './normalizar';
import { Obra, Recomendacion, RecomendacionesResponse, RecomendarRequest } from './tipos';

const CANTIDAD_POR_DEFECTO = 6;

// Lo que se aprende de las interacciones y los eventos del visitante.
interface Senales {
  artistasInteres: Set<number>;
  movimientosInteres: Set<string>;
  movimientosVistos: Set<string>;
  movimientosEventos: Map<string, string>; // normalizado -> nombre original
}

function reunirSenales(req: RecomendarRequest): Senales {
  // El tipo vive en Interaccion y los datos de la obra en obras_con_interaccion:
  // se cruzan por obra_id. Una interacción cuya obra no vino no aporta señal.
  const obrasPorId = new Map((req.obras_con_interaccion ?? []).map((o) => [o.id, o]));
  const s: Senales = {
    artistasInteres: new Set(),
    movimientosInteres: new Set(),
    movimientosVistos: new Set(),
    movimientosEventos: new Map(),
  };
  for (const i of req.interacciones ?? []) {
    const obra = obrasPorId.get(i.obra_id);
    if (!obra) continue;
    const tipo = (i.tipo ?? '').toUpperCase();
    if (tipo === 'INTERES') {
      if (obra.artista) s.artistasInteres.add(obra.artista.id);
      if (obra.movimiento) s.movimientosInteres.add(normalizar(obra.movimiento));
    } else if (tipo === 'VISTA') {
      if (obra.movimiento) s.movimientosVistos.add(normalizar(obra.movimiento));
    }
  }
  for (const e of req.eventos_inscriptos ?? []) {
    if (e.movimiento) s.movimientosEventos.set(normalizar(e.movimiento), e.movimiento);
  }
  return s;
}

function puntuar(obra: Obra, req: RecomendarRequest, s: Senales): Recomendacion {
  const pref = req.preferencias;
  const mov = normalizar(obra.movimiento);
  let puntaje = 0;
  const motivos: string[] = [];
  const sumar = (puntos: number, motivo: string) => {
    puntaje += puntos;
    motivos.push(motivo);
  };

  if (estaEn(obra.movimiento, pref?.movimientos)) {
    sumar(3, `Es del ${obra.movimiento}, uno de tus movimientos preferidos`);
  }
  if (contieneAlguna(obra.tecnica, pref?.tecnicas)) {
    sumar(2, `Su técnica (${obra.tecnica}) es una de las que preferís`);
  }
  if (estaEn(obra.tematica, pref?.tematicas)) {
    sumar(2, `Trata el tema ${obra.tematica}, uno de tus temas preferidos`);
  }
  if (obra.artista && s.artistasInteres.has(obra.artista.id)) {
    sumar(2, `Es de ${obra.artista.nombre}, como otra obra que te interesó`);
  }
  if (mov !== '' && s.movimientosInteres.has(mov)) {
    sumar(1.5, `Es del ${obra.movimiento}, igual que una obra que te interesó`);
  }
  if (mov !== '' && s.movimientosVistos.has(mov)) {
    sumar(1, `Es del ${obra.movimiento}, como una obra que ya viste`);
  }
  if (mov !== '' && s.movimientosEventos.has(mov)) {
    sumar(1, `Te anotaste a un evento sobre ${s.movimientosEventos.get(mov)}`);
  }
  return { obra_id: obra.id, puntaje, motivos };
}

function esMasAntigua(a: Obra, b: Obra): boolean {
  return compararAnio(a.anio_creacion, b.anio_creacion) < 0 ||
    (compararAnio(a.anio_creacion, b.anio_creacion) === 0 && a.id < b.id);
}

export function recomendar(req: RecomendarRequest): RecomendacionesResponse {
  const cantidad = req.cantidad > 0 ? req.cantidad : CANTIDAD_POR_DEFECTO;

  // Las obras con cualquier interacción (vista o de interés) ya las conoce.
  const conocidas = new Set((req.interacciones ?? []).map((i) => i.obra_id));
  const candidatas = (req.candidatas ?? []).filter((o) => !conocidas.has(o.id));

  const senales = reunirSenales(req);
  const puntuadas = candidatas
    .map((o) => puntuar(o, req, senales))
    .filter((r) => r.puntaje > 0)
    .sort((a, b) => b.puntaje - a.puntaje || a.obra_id - b.obra_id);

  if (puntuadas.length > 0) {
    return { recomendaciones: puntuadas.slice(0, cantidad), criterio: 'PERSONALIZADO' };
  }

  // Sin señal del visitante se le da un punto de entrada por movimiento: la obra
  // más antigua de cada uno. Entre movimientos se ordena por esa obra (año, id)
  // para que el resultado sea estable.
  const masAntiguaPorMovimiento = new Map<string, Obra>();
  for (const o of candidatas) {
    const clave = normalizar(o.movimiento);
    if (clave === '') continue;
    const actual = masAntiguaPorMovimiento.get(clave);
    if (!actual || esMasAntigua(o, actual)) masAntiguaPorMovimiento.set(clave, o);
  }
  const exploratorias: Recomendacion[] = [...masAntiguaPorMovimiento.values()]
    .sort((a, b) => (esMasAntigua(a, b) ? -1 : 1))
    .slice(0, cantidad)
    .map((o) => ({
      obra_id: o.id,
      puntaje: 0,
      motivos: [`Para empezar a conocer el ${o.movimiento}`],
    }));
  return { recomendaciones: exploratorias, criterio: 'EXPLORATORIO' };
}
