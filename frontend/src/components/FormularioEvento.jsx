import { useEffect, useState } from 'react'
import { pedir } from '../api/cliente'
import { TIPOS_EVENTO } from '../tiposEvento'

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

      <div className="campo">
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
          <p className="error">{erroresValidacion.titulo}</p>
        )}
      </div>

      <div className="campo">
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
          <p className="error">{erroresValidacion.descripcion}</p>
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
              {tipo.etiqueta}
            </option>
          ))}
        </select>

        {erroresValidacion.tipo && (
          <p className="error">{erroresValidacion.tipo}</p>
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
          <p className="error">{erroresValidacion.fechaHora}</p>
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
          <p className="error">{erroresValidacion.duracionMinutos}</p>
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
          <p className="error">{erroresValidacion.cupoMaximo}</p>
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
          <p className="error">{erroresValidacion.curadorId}</p>
        )}
      </div>

      {errorEnvio && (
        <p className="error">{errorEnvio}</p>
      )}

      <div className="formulario-evento-acciones">
        <button
          className="boton-generar"
          type="submit"
          disabled={enviando}
        >
          {enviando ? 'Guardando...' : textoBoton}
        </button>

        <button
          className="boton-volver"
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
