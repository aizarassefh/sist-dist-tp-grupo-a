import { Link } from 'react-router-dom'
import { nombreTipo } from '../tiposEvento'
import { partesDeFecha, yaComenzo } from '../fechas'
import CupoEvento from './CupoEvento'

// Los eventos llegan ordenados por fecha: alcanza con cortar cada vez que
// cambia el mes, sin volver a ordenar.
function agruparPorMes(eventos) {
  const grupos = []

  for (const evento of eventos) {
    const mes = partesDeFecha(evento.fechaHora).mesAnio
    const ultimo = grupos[grupos.length - 1]

    if (ultimo?.mes === mes) {
      ultimo.eventos.push(evento)
    } else {
      grupos.push({ mes, eventos: [evento] })
    }
  }

  return grupos
}

function FilaEvento({ evento, volverA }) {
  const { dia, diaSemana, hora } = partesDeFecha(evento.fechaHora)
  const pasado = yaComenzo(evento.fechaHora)

  return (
    <li className={pasado ? 'agenda-fila pasado' : 'agenda-fila'}>
      {/* El detalle recibe a qué listado volver, con los filtros actuales. */}
      <Link
        className="agenda-enlace"
        to={`/eventos/${evento.id}`}
        state={{ volverA }}
      >
        <div className="agenda-fecha">
          <span className="agenda-dia">{dia}</span>
          <span className="agenda-semana">{diaSemana}</span>
        </div>

        <div className="agenda-cuerpo">
          <p className="agenda-meta">
            {nombreTipo(evento.tipo)} · {hora} · {evento.duracionMinutos} min
          </p>

          <h3 className="agenda-titulo">{evento.titulo}</h3>

          <p className="agenda-curador">
            Con {evento.curadorResponsable.nombre}
          </p>

          {(evento.inscripto || pasado) && (
            <p className="agenda-estados">
              {evento.inscripto && (
                <span className="estado inscripto">Inscripto</span>
              )}
              {pasado && (
                <span className="estado">Pasado</span>
              )}
            </p>
          )}
        </div>

        <div className="agenda-cupo">
          <CupoEvento
            inscriptos={evento.cantidadInscriptos}
            cupo={evento.cupoMaximo}
          />
        </div>
      </Link>
    </li>
  )
}

// La agenda y no una grilla de tarjetas: un evento se elige por cuándo es,
// así que la fecha va primero y los eventos se agrupan por mes.
function AgendaEventos({ eventos, volverA }) {
  return (
    <div className="agenda">
      {agruparPorMes(eventos).map((grupo) => (
        <section className="agenda-mes" key={grupo.mes}>
          <h2 className="agenda-mes-titulo">{grupo.mes}</h2>

          <ol className="agenda-lista">
            {grupo.eventos.map((evento) => (
              <FilaEvento
                key={evento.id}
                evento={evento}
                volverA={volverA}
              />
            ))}
          </ol>
        </section>
      ))}
    </div>
  )
}

export default AgendaEventos
