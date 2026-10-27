import { useState } from 'react'
import { Link } from 'react-router-dom'
import { generarRecorrido, mensajeDeErrorGateway, obtenerCategorias } from '../api/gateway'
import { useCarga } from '../api/useCarga'
import ImagenObra from '../components/ImagenObra'
import SelectorMultiple from '../components/SelectorMultiple'
import Trazas from '../components/Trazas'

const FORMULARIO_INICIAL = {
  duracionMinutos: '60',
  cantidadObras: '',
  movimientos: [],
  tecnicas: [],
  tematicas: [],
  usarPreferencias: false
}

// Todos los campos son opcionales en el contrato: lo que está vacío no se
// manda, para que el motor use sus valores por defecto.
function armarPedido(formulario) {
  const pedido = { usarPreferencias: formulario.usarPreferencias }

  if (formulario.duracionMinutos !== '') {
    pedido.duracionMinutos = Number(formulario.duracionMinutos)
  }
  if (formulario.cantidadObras !== '') {
    pedido.cantidadObras = Number(formulario.cantidadObras)
  }
  for (const campo of ['movimientos', 'tecnicas', 'tematicas']) {
    if (formulario[campo].length > 0) {
      pedido[campo] = formulario[campo]
    }
  }
  return pedido
}

function Recorridos() {
  const categorias = useCarga('categorias', obtenerCategorias)
  const [formulario, setFormulario] = useState(FORMULARIO_INICIAL)
  const [generando, setGenerando] = useState(false)
  const [errorRecorrido, setErrorRecorrido] = useState(null)
  const [recorrido, setRecorrido] = useState(null)

  function cambiar(campo, valor) {
    setFormulario((actual) => ({ ...actual, [campo]: valor }))
  }

  async function generar(evento) {
    evento.preventDefault()
    setGenerando(true)
    setErrorRecorrido(null)

    try {
      setRecorrido(await generarRecorrido(armarPedido(formulario)))
    } catch (error) {
      setRecorrido(null)
      setErrorRecorrido(mensajeDeErrorGateway(error))
    } finally {
      setGenerando(false)
    }
  }

  const opciones = categorias.datos

  return (
    <>
      <header className="encabezado">
        <h1>Recorridos</h1>
        <p>
          Indicá cuánto tiempo tenés y qué te interesa, y armamos un
          recorrido por las salas con las obras en exhibición.
        </p>
      </header>

      {categorias.error && (
        <p className="error">
          No se pudieron cargar las categorías: {mensajeDeErrorGateway(categorias.error)}
        </p>
      )}

      <form className="formulario-gateway" onSubmit={generar}>
        <div className="formulario-gateway-numeros">
          <div className="campo">
            <label htmlFor="duracion">Duración (minutos)</label>
            <input
              id="duracion"
              type="number"
              min="15"
              max="480"
              value={formulario.duracionMinutos}
              onChange={(e) => cambiar('duracionMinutos', e.target.value)}
            />
          </div>

          <div className="campo">
            <label htmlFor="cantidad">Cantidad de obras</label>
            <input
              id="cantidad"
              type="number"
              min="1"
              max="30"
              placeholder="A criterio del museo"
              value={formulario.cantidadObras}
              onChange={(e) => cambiar('cantidadObras', e.target.value)}
            />
          </div>
        </div>

        {categorias.cargando && <p className="ficha-mensaje">Cargando categorías...</p>}

        {opciones && (
          <>
            <SelectorMultiple
              titulo="Movimientos"
              opciones={opciones.movimientos}
              seleccion={formulario.movimientos}
              onCambiar={(valor) => cambiar('movimientos', valor)}
            />
            <SelectorMultiple
              titulo="Técnicas"
              opciones={opciones.tecnicas}
              seleccion={formulario.tecnicas}
              onCambiar={(valor) => cambiar('tecnicas', valor)}
            />
            <SelectorMultiple
              titulo="Temáticas"
              opciones={opciones.tematicas}
              seleccion={formulario.tematicas}
              onCambiar={(valor) => cambiar('tematicas', valor)}
            />
          </>
        )}

        <label className="formulario-gateway-check">
          <input
            type="checkbox"
            checked={formulario.usarPreferencias}
            onChange={(e) => cambiar('usarPreferencias', e.target.checked)}
          />
          Tener en cuenta mis preferencias
        </label>

        <div className="formulario-gateway-acciones">
          <button type="submit" disabled={generando}>
            {generando ? 'Armando recorrido...' : 'Generar recorrido'}
          </button>
        </div>
      </form>

      {errorRecorrido && <p className="error">{errorRecorrido}</p>}

      {recorrido && (
        <section className="resultado" aria-live="polite">
          {recorrido.paradas.length === 0 ? (
            <p className="ficha-vacio">
              No se pudo armar un recorrido con esos criterios.
            </p>
          ) : (
            <>
              <p className="resultado-resumen">
                {recorrido.paradas.length} paradas · {recorrido.duracionTotalMinutos} minutos en total
              </p>

              <ol className="recorrido-paradas">
                {recorrido.paradas.map((parada) => (
                  <li key={parada.orden} className="parada">
                    <span className="parada-numero">{parada.orden}</span>

                    <ImagenObra
                      className="parada-imagen"
                      src={parada.obra.imagenUrl}
                      alt={`${parada.obra.titulo}, de ${parada.obra.artista.nombre}`}
                    />

                    <div className="parada-texto">
                      <h2 className="parada-titulo">
                        <Link to={`/obra/${parada.obra.id}`}>{parada.obra.titulo}</Link>
                      </h2>
                      <p className="parada-artista">{parada.obra.artista.nombre}</p>
                      <p className="parada-datos">
                        {parada.sala} · {parada.minutosSugeridos} min
                      </p>
                      <p className="parada-motivo">{parada.motivo}</p>
                    </div>
                  </li>
                ))}
              </ol>
            </>
          )}

          {recorrido.notas?.length > 0 && (
            <ul className="notas">
              {recorrido.notas.map((nota, indice) => (
                <li key={indice}>{nota}</li>
              ))}
            </ul>
          )}

          <Trazas trazas={recorrido.trazas} />
        </section>
      )}
    </>
  )
}

export default Recorridos
