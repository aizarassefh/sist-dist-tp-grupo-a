// Cuánto se llenó un evento, de un vistazo: una barra y los lugares libres.
function CupoEvento({ inscriptos, cupo }) {
  const lleno = inscriptos >= cupo
  const libres = Math.max(cupo - inscriptos, 0)
  const porcentaje = cupo > 0 ? Math.min(inscriptos / cupo, 1) * 100 : 0

  return (
    <div className={lleno ? 'cupo lleno' : 'cupo'}>
      <div
        className="cupo-barra"
        role="meter"
        aria-label="Cupo ocupado"
        aria-valuemin={0}
        aria-valuemax={cupo}
        aria-valuenow={inscriptos}
      >
        <span style={{ width: `${porcentaje}%` }} />
      </div>

      <p className="cupo-texto">
        {lleno
          ? 'Completo'
          : `${libres} ${libres === 1 ? 'lugar libre' : 'lugares libres'}`}
        <span> · {inscriptos} de {cupo}</span>
      </p>
    </div>
  )
}

export default CupoEvento
