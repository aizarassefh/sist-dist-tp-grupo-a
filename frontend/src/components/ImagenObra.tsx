import { useState } from 'react'

// El catálogo puede nombrar imágenes que no están en public/obras. Antes la
// tarjeta escondía la imagen rota y el detalle mostraba el ícono roto del
// navegador; así, falte la que falte, se ve el mismo marco vacío.
// "diferida" solo en la galería: en la ficha la obra es lo primero que se ve.
function ImagenObra({ src, alt, className, diferida = false }) {
  const [rota, setRota] = useState(false)

  if (!src || rota) {
    return (
      <div
        className={`${className} imagen-no-disponible`}
        role="img"
        aria-label={alt}
      >
        <span>Imagen no disponible</span>
      </div>
    )
  }

  return (
    <img
      className={className}
      src={src}
      alt={alt}
      loading={diferida ? 'lazy' : 'eager'}
      onError={() => setRota(true)}
    />
  )
}

export default ImagenObra
