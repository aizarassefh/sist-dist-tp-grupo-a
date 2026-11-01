import { useEffect, useState, type ReactElement } from 'react'
import { NavLink } from 'react-router-dom'
import { EVENTO_NOTIFICACIONES_CAMBIARON, obtenerNotificaciones } from '../api/mensajeria'

const INTERVALO_MS = 20_000

// Link de la navegación con la cantidad de notificaciones sin leer.
//
// Se consulta por polling y no por push: las notificaciones las genera un
// consumidor de Kafka unos segundos después de la operación (consistencia
// eventual), así que no hay un momento exacto en que "debería" aparecer y
// consultar cada 20 segundos alcanza. Se pausa con la pestaña oculta para no
// pedirle datos al backend a quien no está mirando.
function IndicadorNotificaciones(): ReactElement {
  const [noLeidas, setNoLeidas] = useState(0)

  useEffect(() => {
    let cancelado = false

    function actualizar(): void {
      // limite=1: solo interesa el contador, no la lista.
      obtenerNotificaciones(false, 1)
        .then((respuesta) => {
          if (!cancelado) setNoLeidas(respuesta.noLeidas)
        })
        .catch(() => {
          // Un fallo de red no debe romper la navegación: se conserva el
          // último valor y se reintenta en el próximo ciclo.
        })
    }

    function actualizarSiVisible(): void {
      if (document.visibilityState === 'visible') actualizar()
    }

    actualizar()
    const intervalo = window.setInterval(actualizarSiVisible, INTERVALO_MS)
    // Al volver a la pestaña se actualiza enseguida en vez de esperar el ciclo.
    document.addEventListener('visibilitychange', actualizarSiVisible)
    window.addEventListener(EVENTO_NOTIFICACIONES_CAMBIARON, actualizar)

    return () => {
      cancelado = true
      window.clearInterval(intervalo)
      document.removeEventListener('visibilitychange', actualizarSiVisible)
      window.removeEventListener(EVENTO_NOTIFICACIONES_CAMBIARON, actualizar)
    }
  }, [])

  return (
    <NavLink
      to="/notificaciones"
      className="navegacion-link"
      aria-label={`Notificaciones${noLeidas > 0 ? `, ${noLeidas} sin leer` : ''}`}
    >
      Notificaciones
      {noLeidas > 0 && <span className="contador-notificaciones">{noLeidas}</span>}
    </NavLink>
  )
}

export default IndicadorNotificaciones
