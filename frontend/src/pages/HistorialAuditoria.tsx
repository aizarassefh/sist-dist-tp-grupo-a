import { useState, type FormEvent, type ReactElement } from 'react'
import { Link } from 'react-router-dom'
import { mensajeDeError } from '../api/cliente'
import { obtenerHistorial } from '../api/mensajeria'
import { useCarga } from '../api/useCarga'
import { formatearFechaHora } from '../fechas'
import { nombreTipo } from '../tiposEvento'
import type { EventoHistorial, RegistroHistorial, TipoNovedad } from '../types/modelos'

const ETIQUETAS_OPERACION: Record<TipoNovedad, string> = {
  ALTA: 'Alta',
  MODIFICACION: 'Modificación',
  CANCELACION: 'Cancelación',
}

// Nombres legibles de los campos que viajan en camposModificados.
const ETIQUETAS_CAMPO: Record<string, string> = {
  titulo: 'Título',
  descripcion: 'Descripción',
  tipo: 'Tipo',
  fechaHora: 'Fecha y hora',
  duracionMinutos: 'Duración (minutos)',
  cupoMaximo: 'Cupo máximo',
  movimiento: 'Movimiento artístico',
  idCurador: 'Id. del curador',
  nombreCurador: 'Curador',
}

// Campos que se muestran en un alta o una cancelación, en este orden.
const CAMPOS_FOTO = [
  'titulo', 'descripcion', 'tipo', 'fechaHora', 'duracionMinutos',
  'cupoMaximo', 'movimiento', 'nombreCurador',
]

function mostrarValor(campo: string, evento: EventoHistorial | null): string {
  const valor = evento ? (evento as unknown as Record<string, unknown>)[campo] : undefined
  if (valor === null || valor === undefined || valor === '') return '—'
  if (campo === 'fechaHora') return formatearFechaHora(String(valor))
  if (campo === 'tipo') return nombreTipo(String(valor))
  return String(valor)
}

function etiquetaCampo(campo: string): string {
  return ETIQUETAS_CAMPO[campo] ?? campo
}

function DetalleRegistro({ registro }: { registro: RegistroHistorial }): ReactElement {
  if (registro.tipo === 'MODIFICACION') {
    if (registro.camposModificados.length === 0) {
      return <p className="historial-sin-detalle">No se registraron campos modificados.</p>
    }
    return (
      <dl className="historial-cambios">
        {registro.camposModificados.map((campo) => (
          <div key={campo}>
            <dt>{etiquetaCampo(campo)}</dt>
            <dd>
              <span className="historial-antes">{mostrarValor(campo, registro.datosAnteriores)}</span>
              <span aria-label="pasó a"> → </span>
              <span className="historial-despues">{mostrarValor(campo, registro.datosPosteriores)}</span>
            </dd>
          </div>
        ))}
      </dl>
    )
  }

  // El alta no tiene "antes" y la cancelación no tiene "después": se muestra
  // la foto del evento que sí existe.
  const foto = registro.tipo === 'ALTA' ? registro.datosPosteriores : registro.datosAnteriores
  if (!foto) {
    return <p className="historial-sin-detalle">No hay datos del evento en este registro.</p>
  }
  return (
    <dl className="historial-cambios">
      {CAMPOS_FOTO.map((campo) => (
        <div key={campo}>
          <dt>{etiquetaCampo(campo)}</dt>
          <dd>{mostrarValor(campo, foto)}</dd>
        </div>
      ))}
      {registro.tipo === 'CANCELACION' && (
        <div>
          <dt>Inscriptos al cancelar</dt>
          <dd>{foto.idsInscriptos?.length ?? 0}</dd>
        </div>
      )}
    </dl>
  )
}

function HistorialAuditoria(): ReactElement {
  // Lo escrito en el formulario no consulta hasta apretar "Filtrar": así no se
  // pide al backend en cada tecla del id.
  const [tipoEditado, setTipoEditado] = useState<TipoNovedad | ''>('')
  const [idEditado, setIdEditado] = useState('')
  const [filtros, setFiltros] = useState<{ tipo: TipoNovedad | ''; idEvento: string }>({
    tipo: '',
    idEvento: '',
  })

  const historial = useCarga(`${filtros.tipo}|${filtros.idEvento}`, () => obtenerHistorial(filtros))
  const registros = historial.datos ?? []
  const hayFiltros = filtros.tipo !== '' || filtros.idEvento !== ''

  function aplicar(e: FormEvent): void {
    e.preventDefault()
    setFiltros({ tipo: tipoEditado, idEvento: idEditado.trim() })
  }

  function limpiar(): void {
    setTipoEditado('')
    setIdEditado('')
    setFiltros({ tipo: '', idEvento: '' })
  }

  return (
    <section className="historial-auditoria">
      <header className="encabezado">
        <p className="rotulo">Administración del museo</p>
        <h1>Historial de actividad</h1>
        <p>Altas, modificaciones y cancelaciones de eventos, con lo que cambió en cada una.</p>
      </header>

      <form className="filtros" onSubmit={aplicar}>
        <div className="campo">
          <label htmlFor="historial-tipo">Tipo</label>
          <select
            id="historial-tipo"
            value={tipoEditado}
            onChange={(e) => setTipoEditado(e.target.value as TipoNovedad | '')}
          >
            <option value="">Todos</option>
            <option value="ALTA">Alta</option>
            <option value="MODIFICACION">Modificación</option>
            <option value="CANCELACION">Cancelación</option>
          </select>
        </div>

        <div className="campo">
          <label htmlFor="historial-evento">Id. de evento</label>
          <input
            id="historial-evento"
            type="number"
            min="1"
            value={idEditado}
            onChange={(e) => setIdEditado(e.target.value)}
          />
        </div>

        <button type="submit">Filtrar</button>
        <button type="button" className="boton-limpiar" onClick={limpiar}>Limpiar</button>
      </form>

      {historial.cargando && <p className="ficha-mensaje" role="status">Cargando historial...</p>}
      {historial.error && <p className="error" role="alert">{mensajeDeError(historial.error)}</p>}
      {historial.datos && registros.length === 0 && (
        <p className="ficha-vacio">
          {hayFiltros ? 'No hay actividad que coincida con los filtros.' : 'No hay actividad registrada todavía.'}
        </p>
      )}

      {registros.length > 0 && (
        <>
          <div className="historial-tabla-contenedor">
            <table className="historial-tabla">
              <thead>
                <tr>
                  <th>Fecha de operación</th>
                  <th>Tipo</th>
                  <th>Evento</th>
                  <th>Responsable</th>
                  <th>Detalle</th>
                </tr>
              </thead>
              <tbody>
                {registros.map((registro) => (
                  <tr key={registro.idMensaje}>
                    <td>{formatearFechaHora(registro.fechaOperacion)}</td>
                    <td>{ETIQUETAS_OPERACION[registro.tipo] ?? registro.tipo}</td>
                    <td>
                      {/* Un evento cancelado ya no existe: sin link. */}
                      {registro.tipo === 'CANCELACION' ? (
                        registro.tituloEvento
                      ) : (
                        <Link to={`/eventos/${registro.idEvento}`}>{registro.tituloEvento}</Link>
                      )}{' '}
                      <small>(#{registro.idEvento})</small>
                    </td>
                    <td>{registro.usuarioResponsable?.nombre ?? '—'}</td>
                    <td>
                      <details className="historial-detalle">
                        <summary>Ver detalle</summary>
                        <DetalleRegistro registro={registro} />
                      </details>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="historial-aclaracion">
            Se muestran hasta los 100 registros más recientes ({registros.length} en esta consulta).
          </p>
        </>
      )}
    </section>
  )
}

export default HistorialAuditoria
