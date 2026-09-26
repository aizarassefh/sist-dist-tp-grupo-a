import { etiquetaTipo } from '../tiposEvento'

const ETIQUETA_ESTADO = {
  FUTUROS: 'Próximos',
  PASADOS: 'Pasados'
}

// AAAA-MM-DD (formato del contrato) a DD/MM/AAAA. No usa Date porque un
// input type="date" y el contrato ya dan la fecha sin hora ni zona: parsear
// con Date solo arriesgaría un corrimiento de un día por la zona horaria.
function formatearFecha(fecha) {
  const [anio, mes, dia] = fecha.split('-')
  return `${dia}/${mes}/${anio}`
}

function nombreCurador(curadorId, curadores) {
  const curador = curadores.find((c) => String(c.id) === String(curadorId))
  return curador ? curador.nombre : `curador #${curadorId}`
}

// Texto legible de un objeto "filtros" (formato del contrato de filtros
// favoritos), para mostrar qué se guardó o qué va a aplicar un favorito.
function ResumenFiltros({ filtros, curadores }) {
  const partes = []

  if (filtros.tipo) {
    partes.push(etiquetaTipo(filtros.tipo))
  }
  if (filtros.desde) {
    partes.push(`desde ${formatearFecha(filtros.desde)}`)
  }
  if (filtros.hasta) {
    partes.push(`hasta ${formatearFecha(filtros.hasta)}`)
  }
  if (filtros.curadorId) {
    partes.push(nombreCurador(filtros.curadorId, curadores))
  }
  if (filtros.estado && ETIQUETA_ESTADO[filtros.estado]) {
    partes.push(ETIQUETA_ESTADO[filtros.estado])
  }

  return (
    <p className="resumen-filtros">
      {partes.length > 0 ? partes.join(' · ') : 'Sin filtros'}
    </p>
  )
}

export default ResumenFiltros
