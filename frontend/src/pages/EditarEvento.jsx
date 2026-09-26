import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ErrorApi, mensajeDeError, pedir } from '../api/cliente'
import { aFechaHoraContrato, aValorInput } from '../fechas'
import FormularioEvento from '../components/FormularioEvento'

const VALORES_INICIALES = {
  titulo: '',
  descripcion: '',
  tipo: 'VISITA_GUIADA',
  fechaHora: '',
  duracionMinutos: '',
  cupoMaximo: '',
  curadorId: ''
}

// Una sola página para /eventos/nuevo y /eventos/:id/editar: la diferencia
// entre alta y edición es solo si hay :id, si se precarga el evento y si se
// llama POST o PUT.
function EditarEvento() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [valores, setValores] = useState(VALORES_INICIALES)
  const [cargando, setCargando] = useState(Boolean(id))
  const [errorCarga, setErrorCarga] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [errorEnvio, setErrorEnvio] = useState(null)

  useEffect(() => {
    if (!id) {
      return
    }

    let cancelado = false

    pedir(`/api/eventos/${id}`)
      .then((datos) => {
        if (cancelado) {
          return
        }
        setValores({
          titulo: datos.titulo,
          descripcion: datos.descripcion,
          tipo: datos.tipo,
          fechaHora: aValorInput(datos.fechaHora),
          duracionMinutos: String(datos.duracionMinutos),
          cupoMaximo: String(datos.cupoMaximo),
          curadorId: String(datos.curadorResponsable.id)
        })
        setCargando(false)
      })
      .catch((error) => {
        if (cancelado) {
          return
        }
        if (error instanceof ErrorApi && error.estado === 404) {
          setErrorCarga('El evento no existe.')
        } else if (error instanceof ErrorApi && error.estado >= 500) {
          setErrorCarga('El servidor todavía no implementa la gestión de eventos.')
        } else {
          setErrorCarga(mensajeDeError(error))
        }
        setCargando(false)
      })

    return () => {
      cancelado = true
    }
  }, [id])

  function cambiarCampo(nombre, valor) {
    setValores((valoresActuales) => ({
      ...valoresActuales,
      [nombre]: valor
    }))
  }

  async function guardar() {
    setEnviando(true)
    setErrorEnvio(null)

    const cuerpo = {
      titulo: valores.titulo.trim(),
      descripcion: valores.descripcion.trim(),
      tipo: valores.tipo,
      fechaHora: aFechaHoraContrato(valores.fechaHora),
      duracionMinutos: Number(valores.duracionMinutos),
      cupoMaximo: Number(valores.cupoMaximo),
      curadorId: Number(valores.curadorId)
    }

    const mensajesPropios = {
      400: 'Revisá los datos del evento.',
      403: 'Solo curadores y administradores pueden gestionar eventos.'
    }
    // El 409 por cupo insuficiente solo puede pasar al editar: el contrato
    // no lo lista como respuesta posible de POST.
    if (id) {
      mensajesPropios[409] = 'El cupo no puede ser menor que la cantidad de inscriptos.'
    }

    try {
      const eventoGuardado = id
        ? await pedir(`/api/eventos/${id}`, { metodo: 'PUT', cuerpo })
        : await pedir('/api/eventos', { metodo: 'POST', cuerpo })

      // replace: que "atrás" desde el detalle no vuelva al formulario ya enviado.
      navigate(`/eventos/${eventoGuardado.id}`, { replace: true })
    } catch (error) {
      setErrorEnvio(mensajeDeError(error, mensajesPropios))
      setEnviando(false)
    }
  }

  function cancelar() {
    navigate(-1)
  }

  return (
    <section className="detalle-obra">

      <h2>{id ? 'Editar evento' : 'Nuevo evento'}</h2>

      {cargando && (
        <p>Cargando evento...</p>
      )}

      {errorCarga && (
        <p className="error">{errorCarga}</p>
      )}

      {!cargando && !errorCarga && (
        <FormularioEvento
          valores={valores}
          onCambiarCampo={cambiarCampo}
          onEnviar={guardar}
          enviando={enviando}
          errorEnvio={errorEnvio}
          textoBoton={id ? 'Guardar cambios' : 'Crear evento'}
          onCancelar={cancelar}
        />
      )}

    </section>
  )
}

export default EditarEvento
