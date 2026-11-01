import { useEffect, useState, type ReactElement } from 'react'
import { mensajeDeError, pedir } from '../api/cliente'
import type { TipoNovedad } from '../types/modelos'

interface RegistroAuditoria {
  messageId: string
  type: TipoNovedad
  eventId: number | string
  title: string
  fechaHora: string | null
  occurredAt: string
  responsibleUserId: number | string | null
  messageJson: string
}

interface PayloadNovedad {
  previousData?: string | null
  newData?: string | null
}

const etiquetas: Record<TipoNovedad, string> = {
  ALTA: 'Alta de evento',
  MODIFICACION: 'Modificación de evento',
  CANCELACION: 'Cancelación de evento',
}

function fecha(value: string | null): string {
  if (!value) return '—'
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? value : new Intl.DateTimeFormat('es-AR', {
    dateStyle: 'short', timeStyle: 'short',
  }).format(parsed)
}

function extraerCambios(messageJson: string): PayloadNovedad {
  try {
    const parsed = JSON.parse(messageJson) as PayloadNovedad
    return {
      previousData: parsed.previousData ?? null,
      newData: parsed.newData ?? null,
    }
  } catch {
    return {}
  }
}

function HistorialAuditoria(): ReactElement {
  const [registros, setRegistros] = useState<RegistroAuditoria[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelado = false
    async function cargar() {
      try {
        const respuesta = await pedir('/api/auditoria/eventos?limite=100')
        if (!cancelado) {
          setRegistros(Array.isArray(respuesta) ? respuesta as RegistroAuditoria[] : [])
          setError('')
        }
      } catch (errorApi) {
        if (!cancelado) setError(mensajeDeError(errorApi))
      } finally {
        if (!cancelado) setCargando(false)
      }
    }
    cargar()
    return () => { cancelado = true }
  }, [])

  return (
    <section className="historial-auditoria">
      <header className="encabezado">
        <p className="rotulo">Administración del museo</p>
        <h1>Historial de actividad</h1>
        <p>Registro de altas, modificaciones y cancelaciones de eventos procesadas por Kafka.</p>
      </header>
      {cargando && <p role="status">Cargando historial...</p>}
      {!cargando && error && <p className="historial-error" role="alert">{error}</p>}
      {!cargando && !error && registros.length === 0 && <p>No hay actividad registrada todavía.</p>}
      {!cargando && !error && registros.length > 0 && (
        <div className="historial-tabla-contenedor">
          <table className="historial-tabla">
            <thead>
              <tr>
                <th>Operación</th>
                <th>Evento</th>
                <th>Fecha del evento</th>
                <th>Detalle de cambios</th>
                <th>Fecha de operación</th>
                <th>Usuario responsable</th>
                <th>Id. mensaje</th>
              </tr>
            </thead>
            <tbody>
              {registros.map((registro) => {
                const { previousData, newData } = extraerCambios(registro.messageJson)
                return (
                  <tr key={registro.messageId}>
                    <td>{etiquetas[registro.type] ?? registro.type}</td>
                    <td>{registro.title} <small>(#{registro.eventId})</small></td>
                    <td>{fecha(registro.fechaHora)}</td>
                    <td>
                      {previousData && <div><small><strong>Antes:</strong> {previousData}</small></div>}
                      {newData && <div><small><strong>Después:</strong> {newData}</small></div>}
                      {!previousData && !newData && '—'}
                    </td>
                    <td>{fecha(registro.occurredAt)}</td>
                    <td>{registro.responsibleUserId ?? '—'}</td>
                    <td><code>{registro.messageId}</code></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
      {!cargando && !error && registros.length > 0 && (
        <p className="historial-aclaracion">Se muestran los {registros.length} registros más recientes.</p>
      )}
    </section>
  )
}

export default HistorialAuditoria
