import { normalizar } from './normalizar';
import { ArgumentoInvalidoError } from './errores';
import { AtributoComparado, Comparacion, CompararObrasRequest, Obra } from './tipos';

function atributo(nombre: string, valores: string[]): AtributoComparado {
  const base = normalizar(valores[0]);
  return { nombre, valores, coinciden: valores.every((v) => normalizar(v) === base) };
}

export function compararObras(req: CompararObrasRequest): Comparacion {
  const obras: Obra[] = req.obras ?? [];
  if (obras.length < 2) {
    throw new ArgumentoInvalidoError('Se necesitan al menos dos obras para comparar');
  }

  const atributos = [
    atributo('Artista', obras.map((o) => o.artista?.nombre ?? '')),
    atributo('Movimiento', obras.map((o) => o.movimiento)),
    atributo('Año', obras.map((o) => (o.anio_creacion > 0 ? String(o.anio_creacion) : 'Sin dato'))),
    atributo('Técnica', obras.map((o) => o.tecnica)),
    atributo('Temática', obras.map((o) => o.tematica)),
    atributo('Dimensiones', obras.map((o) => o.dimensiones)),
    atributo('Ubicación', obras.map((o) => o.ubicacion)),
    atributo('En exhibición', obras.map((o) => (o.en_exhibicion ? 'Sí' : 'No'))),
  ];
  const porNombre = new Map(atributos.map((a) => [a.nombre, a]));
  const observaciones: string[] = [];

  // Solo se compara la distancia entre obras con año conocido; con menos de dos
  // el dato no existe, y una diferencia de 0 años no dice nada.
  const anios = obras.map((o) => o.anio_creacion).filter((a) => a > 0);
  if (anios.length >= 2) {
    const dif = Math.max(...anios) - Math.min(...anios);
    if (dif > 0) observaciones.push(`Entre la más antigua y la más reciente hay ${dif} años`);
  }

  const artista = porNombre.get('Artista');
  const nombresArtistas = new Set(artista.valores.map(normalizar).filter((n) => n !== ''));
  if (artista.coinciden && artista.valores[0] !== '') {
    observaciones.push(`Todas son de ${artista.valores[0]}`);
  } else if (nombresArtistas.size > 1) {
    observaciones.push(`Hay ${nombresArtistas.size} artistas distintos`);
  }

  const movimiento = porNombre.get('Movimiento');
  if (movimiento.coinciden && movimiento.valores[0] !== '') {
    observaciones.push(`Comparten movimiento: ${movimiento.valores[0]}`);
  }

  const sala = porNombre.get('Ubicación');
  if (sala.coinciden && sala.valores[0] !== '') {
    observaciones.push(`Están en la misma sala: ${sala.valores[0]}`);
  }

  const coincidencias = atributos.filter((a) => a.coinciden).length;
  if (coincidencias > 0) {
    observaciones.push(`Coinciden en ${coincidencias} de ${atributos.length} atributos`);
  }

  return { atributos, observaciones };
}
