package com.unla.museo.catalogo.dto;

/**
 * Forma que expone el resolver para "obras" y "obra(id)". No incluye artista
 * ni comentarios: esos campos los completa CatalogoResolver con @BatchMapping,
 * y artistaId es la clave para agruparlos sin una consulta por obra.
 */
public record ObraDTO(
        Long id,
        String titulo,
        Long artistaId,
        String imagenUrl,
        Integer anioCreacion,
        String tecnica,
        String dimensiones,
        String epoca,
        String descripcion,
        String ubicacion,
        boolean enExhibicion
) {
}
