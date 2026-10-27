import { useEffect, useRef, useState } from 'react'

/**
 * Carga datos al montar o cuando cambia "clave". Para volver a pedir sin
 * cambiar de pantalla, la clave lleva un contador que la pantalla incrementa.
 *
 * "cargando" se deriva de comparar la clave del último resultado con la
 * actual, en vez de apagarlo y prenderlo con setState dentro del efecto:
 * así nunca se muestra el resultado de una clave vieja como si fuera el nuevo.
 */
export function useCarga(clave, cargar) {
  const [resultado, setResultado] = useState(null)

  // La función se guarda en una ref porque cada render crea una nueva; si
  // fuera dependencia del efecto, se volvería a pedir en cada render.
  const cargarRef = useRef(cargar)
  useEffect(() => {
    cargarRef.current = cargar
  })

  useEffect(() => {
    let cancelado = false

    cargarRef.current()
      .then((datos) => {
        if (!cancelado) {
          setResultado({ clave, datos, error: null })
        }
      })
      .catch((error) => {
        if (!cancelado) {
          setResultado({ clave, datos: null, error })
        }
      })

    return () => {
      cancelado = true
    }
  }, [clave])

  const vigente = resultado?.clave === clave

  return {
    cargando: !vigente,
    datos: vigente ? resultado.datos : null,
    error: vigente ? resultado.error : null
  }
}
