function FiltrosObras({ filtros, onCambiarFiltro }) {
  return (
    <section className="filtros">

      <h2>Buscar obras</h2>

      <div className="filtros-grupo">

        <div className="campo">
          <label htmlFor="palabraClave">
            Palabra clave
          </label>

          <input
            id="palabraClave"
            type="text"
            value={filtros.palabraClave}
            onChange={(e) =>
              onCambiarFiltro(
                'palabraClave',
                e.target.value
              )
            }
            placeholder="Título, descripción o artista"
          />
        </div>

        <div className="campo">
          <label htmlFor="epoca">
            Época
          </label>

          <input
            id="epoca"
            type="text"
            value={filtros.epoca}
            onChange={(e) =>
              onCambiarFiltro(
                'epoca',
                e.target.value
              )
            }
            placeholder="Ej. Renacimiento"
          />
        </div>

        <div className="campo">
          <label htmlFor="tecnica">
            Técnica
          </label>

          <input
            id="tecnica"
            type="text"
            value={filtros.tecnica}
            onChange={(e) =>
              onCambiarFiltro(
                'tecnica',
                e.target.value
              )
            }
            placeholder="Ej. óleo"
          />
        </div>

        <div className="campo">
          <label htmlFor="ubicacion">
            Ubicación
          </label>

          <input
            id="ubicacion"
            type="text"
            value={filtros.ubicacion}
            onChange={(e) =>
              onCambiarFiltro(
                'ubicacion',
                e.target.value
              )
            }
            placeholder="Ej. Sala de Arte"
          />
        </div>

        <div className="campo">
          <label htmlFor="enExhibicion">
            Exhibición
          </label>

          <select
            id="enExhibicion"
            value={
              filtros.enExhibicion === null
                ? ''
                : String(filtros.enExhibicion)
            }
            onChange={(e) => {
              const valor = e.target.value

              onCambiarFiltro(
                'enExhibicion',
                valor === ''
                  ? null
                  : valor === 'true'
              )
            }}
          >
            <option value="">Todas</option>
            <option value="true">En exhibición</option>
            <option value="false">En depósito</option>
          </select>
        </div>

      </div>

      <button
        className="boton-limpiar"
        type="button"
        onClick={() => {
          onCambiarFiltro('palabraClave', '')
          onCambiarFiltro('epoca', '')
          onCambiarFiltro('tecnica', '')
          onCambiarFiltro('ubicacion', '')
          onCambiarFiltro('enExhibicion', null)
        }}
      >
        Limpiar filtros
      </button>

    </section>
  )
}

export default FiltrosObras