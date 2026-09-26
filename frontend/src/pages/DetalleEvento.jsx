import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ErrorApi, mensajeDeError, pedir } from '../api/cliente'
import { esGestor } from '../roles'
import { nombreTipo } from '../tiposEvento'
import { partesDeFecha, yaComenzo } from '../fechas'
import CupoEvento from '../components/CupoEvento'

function DetalleEvento({ usuario }) {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()

  const [evento, setEvento] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [noExiste, setNoExiste] = useState(false)
  const [backendNoDisponible, setBackendNoDisponible] = useState(false)
  const [errorGeneral, setErrorGeneral] = useState(null)

  const [accionEnCurso, setAccionEnCurso] = useState(false)
  const [errorAccion, setErrorAccion] = useState(null)

  useEffect(() => {
    let cancelado = false

    pedir(`/api/eventos/${id}`)
      .then((datos) => {
        if (cancelado) {
          return
        }
        setEvento(datos)
        setCargando(false)
      })
      .catch((error) => {
        if (cancelado) {
          return
        }
        if (error instanceof ErrorApi && error.estado === 404) {
          setNoExiste(true)
        } else if (error instanceof ErrorApi && error.estado >= 500) {
          setBackendNoDisponible(true)
        } else {
          setErrorGeneral(mensajeDeError(error))
        }
        setCargando(false)
      })

    return () => {
      cancelado = true
    }
  }, [id])

  async function inscribirse() {
    setAccionEnCurso(true)
    setErrorAccion(null)

    try {
      await pedir(`/api/eventos/${id}/inscripcion`, { metodo: 'POST' })
      const datos = await pedir(`/api/eventos/${id}`)
      setEvento(datos)
    } catch (error) {
      setErrorAccion(mensajeDeError(error, {
        409: 'No se pudo inscribir: el evento no tiene cupo, ya comenzó o ya estás inscripto.'
      }))
    } finally {
      setAccionEnCurso(false)
    }
  }

  async function cancelarInscripcion() {
    setAccionEnCurso(true)
    setErrorAccion(null)

    try {
      await pedir(`/api/eventos/${id}/inscripcion`, { metodo: 'DELETE' })
      const datos = await pedir(`/api/eventos/${id}`)
      setEvento(datos)
    } catch (error) {
      setErrorAccion(mensajeDeError(error))
    } finally {
      setAccionEnCurso(false)
    }
  }

  async function eliminar() {
    const pregunta = evento.cantidadInscriptos > 0
      ? `Este evento tiene ${evento.cantidadInscriptos} inscriptos. ¿Seguro que querés eliminarlo?`
      : '¿Seguro que querés eliminar este evento?'

    if (!window.confirm(pregunta)) {
      return
    }

    setAccionEnCurso(true)
    setErrorAccion(null)

    try {
      await pedir(`/api/eventos/${id}`, { metodo: 'DELETE' })
      navigate('/eventos')
    } catch (error) {
      setErrorAccion(mensajeDeError(error, {
        409: 'No se puede eliminar un evento con inscriptos.'
      }))
      setAccionEnCurso(false)
    }
  }

  if (cargando) {
    return <p className="ficha-mensaje">Cargando evento...</p>
  }

  if (noExiste) {
    return (
      <section className="sin-permiso">
        <h2>El evento no existe</h2>

        <Link className="boton-volver" to="/eventos">
          ← Volver a eventos
        </Link>
      </section>
    )
  }

  if (backendNoDisponible) {
    return (
      <p className="error">
        El servidor todavía no implementa la gestión de eventos.
      </p>
    )
  }

  if (errorGeneral) {
    return <p className="error">{errorGeneral}</p>
  }

  const comenzo = yaComenzo(evento.fechaHora)
  const completo = evento.cantidadInscriptos >= evento.cupoMaximo
  const { dia, diaSemana, hora, mesAnio } = partesDeFecha(evento.fechaHora)

  return (
    <article className="ficha-evento">

      {/* No se usa navigate(-1): si se llega desde el formulario o por URL
          directa, "atrás" no es el listado. El listado pasa sus filtros. */}
      <Link
        className="enlace-volver"
        to={location.state?.volverA ?? '/eventos'}
      >
        ← Eventos
      </Link>

      <header className="evento-cabecera">
        <div className="evento-fecha">
          <span className="evento-dia">{dia}</span>
          <span className="evento-mes">{mesAnio}</span>
          <span className="evento-hora">{diaSemana} · {hora} h</span>
        </div>

        <div>
          <p className="rotulo">{nombreTipo(evento.tipo)}</p>

          <h1 className="evento-titulo">{evento.titulo}</h1>

          <p className="evento-curador">
            Con {evento.curadorResponsable.nombre} · {evento.duracionMinutos} minutos
          </p>

          {(evento.inscripto || comenzo) && (
            <p className="agenda-estados">
              {evento.inscripto && (
                <span className="estado inscripto">Estás inscripto</span>
              )}
              {comenzo && (
                <span className="estado">Pasado</span>
              )}
            </p>
          )}
        </div>
      </header>

      <div className="evento-cuerpo">

        <div className="evento-texto">
          <p className="ficha-descripcion">
            {evento.descripcion}
          </p>

          {esGestor(usuario) && (
            <section className="ficha-seccion">
              <h2 className="rotulo">
                Inscriptos ({evento.inscriptos.length})
              </h2>

              {evento.inscriptos.length === 0 ? (
                <p className="ficha-vacio">No hay inscriptos todavía.</p>
              ) : (
                <ol className="inscriptos-lista">
                  {evento.inscriptos.map((inscripto) => (
                    <li key={inscripto.id}>{inscripto.nombre}</li>
                  ))}
                </ol>
              )}
            </section>
          )}
        </div>

        {/* Columna de acción: el cupo y lo que el usuario puede hacer. */}
        <aside className="evento-lateral">
          <h2 className="rotulo">Cupo</h2>

          <CupoEvento
            inscriptos={evento.cantidadInscriptos}
            cupo={evento.cupoMaximo}
          />

          {errorAccion && (
            <p className="error">{errorAccion}</p>
          )}

          {evento.inscripto ? (
            <button
              className="boton-secundario evento-accion"
              type="button"
              disabled={accionEnCurso}
              onClick={cancelarInscripcion}
            >
              Cancelar inscripción
            </button>
          ) : (
            <>
              <button
                className="evento-accion"
                type="button"
                disabled={accionEnCurso || comenzo || completo}
                onClick={inscribirse}
              >
                Inscribirme
              </button>

              {(comenzo || completo) && (
                <p className="motivo-deshabilitado">
                  {comenzo
                    ? 'El evento ya comenzó.'
                    : 'El evento está completo.'}
                </p>
              )}
            </>
          )}

          {esGestor(usuario) && (
            <div className="evento-gestion">
              <Link className="boton-limpiar" to={`/eventos/${id}/editar`}>
                Editar
              </Link>

              <button
                className="boton-peligro"
                type="button"
                disabled={accionEnCurso}
                onClick={eliminar}
              >
                Eliminar
              </button>
            </div>
          )}
        </aside>

      </div>

    </article>
  )
}

export default DetalleEvento
