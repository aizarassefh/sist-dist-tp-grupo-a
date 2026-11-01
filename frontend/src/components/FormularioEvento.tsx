import { useEffect, useState } from 'react'
import { pedir } from '../api/cliente'
import { obtenerCategorias } from '../api/gateway'
import { TIPOS_EVENTO } from '../tiposEvento'

const MAX_MOVIMIENTO = 100

function validar(valores) {
  const errores = {}

  if (!valores.titulo.trim()) {
    errores.titulo = 'El título es obligatorio.'
  }
  if (!valores.descripcion.trim()) {
    errores.descripcion = 'La descripción es obligatoria.'
  }
  if (!valores.tipo) {
    errores.tipo = 'Elegí un tipo de evento.'
  }
  if (!valores.fechaHora) {
    errores.fechaHora = 'La fecha y hora son obligatorias.'
  }
  if (!valores.duracionMinutos || Number(valores.duracionMinutos) <= 0) {
    errores.duracionMinutos = 'La duración tiene que ser un número mayor a 0.'
  }
  if (!valores.cupoMaximo || Number(valores.cupoMaximo) <= 0) {
    errores.cupoMaximo = 'El cupo tiene que ser un número mayor a 0.'
  }
  if (valores.movimiento.trim().length > MAX_MOVIMIENTO) {
    errores.movimiento = `El movimiento no puede superar los ${MAX_MOVIMIENTO} caracteres.`
  }
  if (!valores.curadorId) {
    errores.curadorId = 'Elegí un curador responsable.'
  }

  return errores
}

// Formulario de alta y edición de eventos. Carga sus propios curadores (a
// diferencia de FiltrosEventos, que los recibe por prop) porque solo lo usa
// EditarEvento y no hace falta compartir ese estado con nadie más.
function FormularioEvento({
  valores,
  onCambiarCampo,
  onEnviar,
  enviando,
  errorEnvio,
  textoBoton,
  onCancelar
}) {
  const [curadores, setCuradores] = useState([])
  const [erroresValidacion, setErroresValidacion] = useState({})
  // null mientras carga; si el gateway falla queda null y se usa texto libre.
  const [movimientos, setMovimientos] = useState(null)
  const [gatewayFallo, setGatewayFallo] = useState(false)

  useEffect(() => {
    let cancelado = false

    pedir('/api/usuarios/curadores')
      .then((datos) => {
        if (!cancelado) {
          setCuradores(datos ?? [])
        }
      })
      .catch(() => {
        // Sin curadores el select queda vacío; no rompe el formulario.
      })

    return () => {
      cancelado = true
    }
  }, [])

  useEffect(() => {
    let cancelado = false

    obtenerCategorias()
      .then((datos) => {
        if (!cancelado) {
          setMovimientos((datos?.movimientos ?? []).map((movimiento) => movimiento.nombre))
        }
      })
      .catch(() => {
        // El movimiento es opcional: si el gateway no responde se pasa a un
        // input de texto libre para no bloquear el alta del evento.
        if (!cancelado) {
          setGatewayFallo(true)
        }
      })

    return () => {
      cancelado = true
    }
  }, [])

  // Si el evento ya tiene un movimiento que no figura en las categorías, se
  // agrega como opción para que editar otro campo no lo borre sin querer.
  const opcionesMovimiento = [...(movimientos ?? [])]
  if (valores.movimiento && !opcionesMovimiento.includes(valores.movimiento)) {
    opcionesMovimiento.push(valores.movimiento)
  }

  function manejarSubmit(e) {
    e.preventDefault()
    const erroresNuevos = validar(valores)
    setErroresValidacion(erroresNuevos)

    if (Object.keys(erroresNuevos).length === 0) {
      onEnviar()
    }
  }

  return (
    <form className="formulario-evento" onSubmit={manejarSubmit}>

      <div className="campo campo-ancho">
        <label htmlFor="titulo">
          Título
        </label>

        <input
          id="titulo"
          type="text"
          value={valores.titulo}
          onChange={(e) => onCambiarCampo('titulo', e.target.value)}
        />

        {erroresValidacion.titulo && (
          <p className="campo-error">{erroresValidacion.titulo}</p>
        )}
      </div>

      <div className="campo campo-ancho">
        <label htmlFor="descripcion">
          Descripción
        </label>

        <textarea
          id="descripcion"
          rows={4}
          value={valores.descripcion}
          onChange={(e) => onCambiarCampo('descripcion', e.target.value)}
        />

        {erroresValidacion.descripcion && (
          <p className="campo-error">{erroresValidacion.descripcion}</p>
        )}
      </div>

      <div className="campo">
        <label htmlFor="tipo">
          Tipo de evento
        </label>

        <select
          id="tipo"
          value={valores.tipo}
          onChange={(e) => onCambiarCampo('tipo', e.target.value)}
        >
          {TIPOS_EVENTO.map((tipo) => (
            <option key={tipo.valor} value={tipo.valor}>
              {tipo.singular}
            </option>
          ))}
        </select>

        {erroresValidacion.tipo && (
          <p className="campo-error">{erroresValidacion.tipo}</p>
        )}
      </div>

      <div className="campo">
        <label htmlFor="fechaHora">
          Fecha y hora
        </label>

        <input
          id="fechaHora"
          type="datetime-local"
          value={valores.fechaHora}
          onChange={(e) => onCambiarCampo('fechaHora', e.target.value)}
        />

        {erroresValidacion.fechaHora && (
          <p className="campo-error">{erroresValidacion.fechaHora}</p>
        )}
      </div>

      <div className="campo">
        <label htmlFor="duracionMinutos">
          Duración (minutos)
        </label>

        <input
          id="duracionMinutos"
          type="number"
          min="1"
          value={valores.duracionMinutos}
          onChange={(e) => onCambiarCampo('duracionMinutos', e.target.value)}
        />

        {erroresValidacion.duracionMinutos && (
          <p className="campo-error">{erroresValidacion.duracionMinutos}</p>
        )}
      </div>

      <div className="campo">
        <label htmlFor="cupoMaximo">
          Cupo máximo
        </label>

        <input
          id="cupoMaximo"
          type="number"
          min="1"
          value={valores.cupoMaximo}
          onChange={(e) => onCambiarCampo('cupoMaximo', e.target.value)}
        />

        {erroresValidacion.cupoMaximo && (
          <p className="campo-error">{erroresValidacion.cupoMaximo}</p>
        )}
      </div>

      <div className="campo">
        <label htmlFor="movimiento">
          Movimiento artístico (opcional)
        </label>

        {gatewayFallo ? (
          <input
            id="movimiento"
            type="text"
            maxLength={MAX_MOVIMIENTO}
            value={valores.movimiento}
            onChange={(e) => onCambiarCampo('movimiento', e.target.value)}
          />
        ) : (
          <select
            id="movimiento"
            value={valores.movimiento}
            onChange={(e) => onCambiarCampo('movimiento', e.target.value)}
          >
            <option value="">Sin movimiento</option>

            {opcionesMovimiento.map((nombre) => (
              <option key={nombre} value={nombre}>
                {nombre}
              </option>
            ))}
          </select>
        )}

        {erroresValidacion.movimiento && (
          <p className="campo-error">{erroresValidacion.movimiento}</p>
        )}
      </div>

      <div className="campo">
        <label htmlFor="curadorId">
          Curador responsable
        </label>

        <select
          id="curadorId"
          value={valores.curadorId}
          onChange={(e) => onCambiarCampo('curadorId', e.target.value)}
        >
          <option value="">Elegí un curador</option>

          {curadores.map((curador) => (
            <option key={curador.id} value={curador.id}>
              {curador.nombre}
            </option>
          ))}
        </select>

        {erroresValidacion.curadorId && (
          <p className="campo-error">{erroresValidacion.curadorId}</p>
        )}
      </div>

      {errorEnvio && (
        <p className="error">{errorEnvio}</p>
      )}

      <div className="formulario-evento-acciones">
        <button
          type="submit"
          disabled={enviando}
        >
          {enviando ? 'Guardando...' : textoBoton}
        </button>

        <button
          className="boton-limpiar"
          type="button"
          onClick={onCancelar}
        >
          Cancelar
        </button>
      </div>

    </form>
  )
}

export default FormularioEvento
