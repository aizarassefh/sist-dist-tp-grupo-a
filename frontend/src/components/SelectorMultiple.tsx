// Multi-selección de categorías (movimientos, técnicas, temáticas). Las
// opciones vienen de /api/v2/categorias como { nombre, cantidadObras }.
function SelectorMultiple({ titulo, opciones, seleccion, onCambiar }) {
  // Una preferencia guardada puede no estar ya en las categorías (si se
  // borraron las obras): se agrega igual para que se pueda ver y destildar.
  const nombres = opciones.map((opcion) => (typeof opcion === 'string' ? opcion : opcion.nombre))
  const cantidades = new Map(
    opciones.filter((opcion) => typeof opcion !== 'string').map((opcion) => [opcion.nombre, opcion.cantidadObras])
  )
  const todos = [...nombres, ...seleccion.filter((nombre) => !nombres.includes(nombre))]

  function alternar(nombre) {
    onCambiar(
      seleccion.includes(nombre)
        ? seleccion.filter((elegido) => elegido !== nombre)
        : [...seleccion, nombre]
    )
  }

  return (
    <fieldset className="selector">
      <legend>{titulo}</legend>

      {todos.length === 0 ? (
        <p className="selector-vacio">No hay opciones para elegir.</p>
      ) : (
        <div className="selector-opciones">
          {todos.map((nombre) => (
            <label key={nombre} className="selector-opcion">
              <input
                type="checkbox"
                checked={seleccion.includes(nombre)}
                onChange={() => alternar(nombre)}
              />
              {nombre}
              {cantidades.has(nombre) && (
                <span className="selector-cantidad">{cantidades.get(nombre)}</span>
              )}
            </label>
          ))}
        </div>
      )}
    </fieldset>
  )
}

export default SelectorMultiple
