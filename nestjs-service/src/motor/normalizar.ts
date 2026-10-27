// Las comparaciones del motor ignoran mayúsculas y acentos para ser
// consistentes con el catálogo ("impresionismo" == "Impresionismo",
// "Mitologia" == "Mitología").
export function normalizar(texto?: string): string {
  return (texto ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}

// Un valor vacío no coincide con nada: evita que una técnica '' "contenga" todo.
export function contieneAlguna(texto: string, buscados: string[] = []): boolean {
  const base = normalizar(texto);
  return buscados.some((b) => {
    const n = normalizar(b);
    return n !== '' && base.includes(n);
  });
}

export function estaEn(valor: string, lista: string[] = []): boolean {
  const n = normalizar(valor);
  return n !== '' && lista.some((x) => normalizar(x) === n);
}

// Año 0 significa "no se conoce" en el contrato: va al final al ordenar ascendente.
export function compararAnio(a: number, b: number): number {
  const ka = a > 0 ? a : Number.MAX_SAFE_INTEGER;
  const kb = b > 0 ? b : Number.MAX_SAFE_INTEGER;
  return ka === kb ? 0 : ka < kb ? -1 : 1;
}
