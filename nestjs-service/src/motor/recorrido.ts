import { compararAnio, contieneAlguna, estaEn, normalizar } from './normalizar';
import { ArgumentoInvalidoError } from './errores';
import { ArmarRecorridoRequest, Obra, Parada, Preferencias, Recorrido } from './tipos';

const MINUTOS_CON_AFINIDAD = 6;
const MINUTOS_SIN_AFINIDAD = 4;
const MINUTOS_TRASLADO = 2;

interface Puntuada {
  obra: Obra;
  afinidad: number;
  motivo: string;
}

function puntuar(obra: Obra, pref: Preferencias | undefined): Puntuada {
  let afinidad = 0;
  const coincidencias: string[] = [];
  if (estaEn(obra.movimiento, pref?.movimientos)) {
    afinidad += 3;
    coincidencias.push(obra.movimiento);
  }
  if (contieneAlguna(obra.tecnica, pref?.tecnicas)) {
    afinidad += 2;
    coincidencias.push(obra.tecnica);
  }
  if (estaEn(obra.tematica, pref?.tematicas)) {
    afinidad += 2;
    coincidencias.push(obra.tematica);
  }
  const motivo = coincidencias.length
    ? `Coincide con tu interés en ${coincidencias.join(' y en ')}`
    : 'Cumple los criterios que elegiste';
  return { obra, afinidad, motivo };
}

// Orden de visita: salas alfabéticas (pocas idas y vueltas) y, dentro de cada
// una, cronológico. El id es desempate final para que el resultado sea
// determinista aunque los datos lleguen en otro orden.
function ordenarVisita(elegidas: Puntuada[]): Puntuada[] {
  return [...elegidas].sort(
    (a, b) =>
      normalizar(a.obra.ubicacion).localeCompare(normalizar(b.obra.ubicacion)) ||
      compararAnio(a.obra.anio_creacion, b.obra.anio_creacion) ||
      a.obra.id - b.obra.id,
  );
}

function minutosDe(p: Puntuada): number {
  return p.afinidad > 0 ? MINUTOS_CON_AFINIDAD : MINUTOS_SIN_AFINIDAD;
}

function duracionTotal(visita: Puntuada[]): number {
  let total = 0;
  visita.forEach((p, i) => {
    total += minutosDe(p);
    // Cada cambio de sala entre paradas consecutivas cuesta un traslado.
    if (i > 0 && normalizar(visita[i - 1].obra.ubicacion) !== normalizar(p.obra.ubicacion)) {
      total += MINUTOS_TRASLADO;
    }
  });
  return total;
}

function obrasAfuera(n: number): string {
  return n === 1 ? 'Quedó 1 obra afuera' : `Quedaron ${n} obras afuera`;
}

export function armarRecorrido(req: ArmarRecorridoRequest): Recorrido {
  const limiteMinutos = req.duracion_minutos ?? 0;
  const limiteCantidad = req.cantidad_maxima ?? 0;
  if (limiteMinutos < 0 || limiteCantidad < 0) {
    throw new ArgumentoInvalidoError('La duración y la cantidad máxima no pueden ser negativas');
  }

  const candidatas = req.candidatas ?? [];
  if (candidatas.length === 0) {
    // Que no haya obras para esos criterios es un resultado válido, no un error.
    return {
      paradas: [],
      duracion_total_minutos: 0,
      notas: ['No hay obras en exhibición que cumplan los criterios elegidos'],
    };
  }

  const porPrioridad = candidatas
    .map((o) => puntuar(o, req.preferencias))
    .sort(
      (a, b) =>
        b.afinidad - a.afinidad ||
        compararAnio(a.obra.anio_creacion, b.obra.anio_creacion) ||
        a.obra.id - b.obra.id,
    );

  // Selección voraz: una obra que no entra no corta la búsqueda, porque una
  // posterior (p. ej. de una sala ya incluida, sin traslado extra) puede entrar.
  let elegidas: Puntuada[] = [];
  let afueraPorCantidad = 0;
  let afueraPorTiempo = 0;
  for (const p of porPrioridad) {
    if (limiteCantidad > 0 && elegidas.length >= limiteCantidad) {
      afueraPorCantidad++;
      continue;
    }
    // Se recalcula todo el orden con la obra nueva: agregarla puede reordenar la
    // visita y cambiar la cantidad de traslados de las ya elegidas.
    const tentativa = ordenarVisita([...elegidas, p]);
    if (limiteMinutos > 0 && duracionTotal(tentativa) > limiteMinutos) {
      afueraPorTiempo++;
      continue;
    }
    elegidas = tentativa;
  }

  const paradas: Parada[] = elegidas.map((p, i) => ({
    orden: i + 1,
    obra_id: p.obra.id,
    sala: p.obra.ubicacion,
    minutos_sugeridos: minutosDe(p),
    motivo: p.motivo,
  }));

  const notas: string[] = [];
  if (afueraPorTiempo > 0) {
    notas.push(`${obrasAfuera(afueraPorTiempo)} para no pasarte de ${limiteMinutos} minutos`);
  }
  if (afueraPorCantidad > 0) {
    notas.push(`${obrasAfuera(afueraPorCantidad)} por el límite de ${limiteCantidad} ${limiteCantidad === 1 ? 'obra' : 'obras'}`);
  }

  return { paradas, duracion_total_minutos: duracionTotal(elegidas), notas };
}
