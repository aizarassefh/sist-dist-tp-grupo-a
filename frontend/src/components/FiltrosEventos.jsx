import { TIPOS_EVENTO } from '../tiposEvento'

// Controlado y sin URL ni router: lo reusa el listado de eventos y, después,
// la pantalla de filtros favoritos (docs/contrato-api-eventos.md).
function FiltrosEventos({ filtros, curadores, onCambiarFiltro }) {
  return (
    <section className="filtros">

      <h2>Buscar eventos</h2>

      <div className="filtros-grupo">

        <div className="campo">
          <label htmlFor="desde">
            Desde
          </label>

          <input
            id="desde"
            type="date"
            value={filtros.desde}
            onChange={(e) =>
              onCambiarFiltro('desde', e.target.value)
            }
          />
        </div>

        <div className="campo">
          <label htmlFor="hasta">
            Hasta
          </label>

          <input
            id="hasta"
            type="date"
            value={filtros.hasta}
            onChange={(e) =>
              onCambiarFiltro('hasta', e.target.value)
            }
          />
        </div>

        <div className="campo">
          <label htmlFor="tipo">
            Tipo de evento
          </label>

          <select
            id="tipo"
            value={filtros.tipo}
            onChange={(e) =>
              onCambiarFiltro('tipo', e.target.value)
            }
          >
            <option value="">Todos</option>

            {TIPOS_EVENTO.map((tipo) => (
              <option key={tipo.valor} value={tipo.valor}>
                {tipo.etiqueta}
              </option>
            ))}
          </select>
        </div>

        <div className="campo">
          <label htmlFor="curadorId">
            Curador a cargo
          </label>

          <select
            id="curadorId"
            value={filtros.curadorId}
            onChange={(e) =>
              onCambiarFiltro('curadorId', e.target.value)
            }
          >
            <option value="">Todos</option>

            {curadores.map((curador) => (
              <option key={curador.id} value={curador.id}>
                {curador.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className="campo">
          <label htmlFor="estado">
            Estado
          </label>

          <select
            id="estado"
            value={filtros.estado}
            onChange={(e) =>
              onCambiarFiltro('estado', e.target.value)
            }
          >
            <option value="">Todos</option>
            <option value="FUTUROS">Próximos</option>
            <option value="PASADOS">Pasados</option>
          </select>
        </div>

      </div>

      <button
        className="boton-limpiar"
        type="button"
        onClick={() => {
          onCambiarFiltro('desde', '')
          onCambiarFiltro('hasta', '')
          onCambiarFiltro('tipo', '')
          onCambiarFiltro('curadorId', '')
          onCambiarFiltro('estado', '')
        }}
      >
        Limpiar filtros
      </button>

    </section>
  )
}

export default FiltrosEventos
