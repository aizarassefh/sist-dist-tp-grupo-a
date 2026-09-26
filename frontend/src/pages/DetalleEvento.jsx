import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ErrorApi, mensajeDeError, pedir } from '../api/cliente'
import { esGestor } from '../roles'
import { etiquetaTipo } from '../tiposEvento'
import { formatearFechaHora, yaComenzo } from '../fechas'

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
    return <p>Cargando evento...</p>
  }

  if (noExiste) {
    return (
      <section className="detalle-obra">
        <h2>El evento no existe.</h2>

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

  return (
    <section className="detalle-obra">

      {/* No se usa navigate(-1): si se llega desde el formulario o por URL
          directa, "atrás" no es el listado. El listado pasa sus filtros. */}
      <Link
        className="boton-volver"
        to={location.state?.volverA ?? '/eventos'}
      >
        ← Volver a eventos
      </Link>

      <h2>{evento.titulo}</h2>

      <p className="obra-artista">
        {etiquetaTipo(evento.tipo)}
      </p>

      <p>
        <strong>Fecha y hora:</strong>{' '}
        {formatearFechaHora(evento.fechaHora)}
      </p>

      <p>
        <strong>Duración:</strong>{' '}
        {evento.duracionMinutos} minutos
      </p>

      <p>
        <strong>Curador responsable:</strong>{' '}
        {evento.curadorResponsable.nombre}
      </p>

      <p>
        <strong>Cupo:</strong>{' '}
        {evento.cantidadInscriptos} / {evento.cupoMaximo} inscriptos
      </p>

      <div className="etiquetas-evento">
        {comenzo && (
          <span className="estado finalizado">Finalizado</span>
        )}
        {completo && (
          <span className="estado completo">Completo</span>
        )}
        {evento.inscripto && (
          <span className="estado inscripto">Inscripto</span>
        )}
      </div>

      <h3>Descripción</h3>

      <p className="detalle-descripcion">
        {evento.descripcion}
      </p>

      {esGestor(usuario) ? (
        <div className="detalle-comentarios">
          <h3>Inscriptos ({evento.inscriptos.length})</h3>

          {evento.inscriptos.length === 0 ? (
            <p className="sin-comentarios">No hay inscriptos todavía.</p>
          ) : (
            <ul>
              {evento.inscriptos.map((inscripto) => (
                <li key={inscripto.id}>{inscripto.nombre}</li>
              ))}
            </ul>
          )}
        </div>
      ) : null}

      {errorAccion && (
        <p className="error">{errorAccion}</p>
      )}

      <div className="detalle-evento-acciones">
        {evento.inscripto ? (
          <button
            type="button"
            disabled={accionEnCurso}
            onClick={cancelarInscripcion}
          >
            Cancelar inscripción
          </button>
        ) : (
          <>
            <button
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
          <>
            <Link className="boton-detalle" to={`/eventos/${id}/editar`}>
              Editar
            </Link>

            <button
              type="button"
              disabled={accionEnCurso}
              onClick={eliminar}
            >
              Eliminar
            </button>
          </>
        )}
      </div>

    </section>
  )
}

export default DetalleEvento
