// Muestra las llamadas gRPC que hizo el gateway para armar la respuesta. Es
// discreto a propósito (no es contenido del museo), pero queda a la vista
// porque demuestra que la operación coordinó varios servicios.
function Trazas({ trazas }) {
  if (!trazas || trazas.length === 0) {
    return null
  }

  return (
    <p className="trazas">
      <span className="trazas-rotulo">Cómo se resolvió:</span>
      {trazas.map((traza, indice) => (
        // Una misma llamada puede repetirse (p. ej. dos BuscarObras), así
        // que el índice es la única clave estable.
        <span key={indice} className="traza">
          {indice > 0 && <span className="traza-flecha" aria-hidden="true">→</span>}
          {traza.servicio}.{traza.metodo} · {traza.ms} ms
        </span>
      ))}
    </p>
  )
}

export default Trazas
