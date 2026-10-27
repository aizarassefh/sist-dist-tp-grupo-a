package com.unla.museo.gateway.json;

import com.unla.museo.catalogo.grpc.Artista;
import com.unla.museo.catalogo.grpc.Categoria;
import com.unla.museo.catalogo.grpc.Obra;
import com.unla.museo.eventos.grpc.Evento;
import com.unla.museo.usuarios.grpc.Preferencias;

import java.util.List;

/**
 * Único lugar que convierte mensajes proto en la forma JSON del contrato
 * (camelCase). Los controllers lo reutilizan para que una obra se vea igual en
 * todas las respuestas. Es solo forma: no decide nada sobre el contenido.
 */
public final class MapeadorJson {

    private MapeadorJson() {
    }

    /** Artista embebido en una obra: solo identidad y procedencia. */
    public record ArtistaResumenJson(long id, String nombre, String nacionalidad, String movimiento) {
    }

    public record ObraJson(long id, String titulo, String descripcion, ArtistaResumenJson artista,
                           Integer anioCreacion, String tecnica, String tematica, String movimiento,
                           String dimensiones, String ubicacion, boolean enExhibicion, String imagenUrl) {
    }

    public record ArtistaJson(long id, String nombre, String nacionalidad, String movimiento,
                              Integer anioNacimiento, Integer anioFallecimiento, int cantidadObras,
                              String biografia) {
    }

    public record EventoJson(long id, String titulo, String tipo, String fechaHora, int duracionMinutos,
                             int cupoMaximo, int inscriptos) {
    }

    public record CategoriaJson(String nombre, int cantidadObras) {
    }

    public record PreferenciasJson(List<String> movimientos, List<String> tecnicas, List<String> tematicas) {
    }

    public static ObraJson obra(Obra o) {
        ArtistaResumenJson artista = o.hasArtista()
                ? new ArtistaResumenJson(o.getArtista().getId(), o.getArtista().getNombre(),
                        o.getArtista().getNacionalidad(), o.getArtista().getMovimiento())
                : null;
        return new ObraJson(o.getId(), o.getTitulo(), o.getDescripcion(), artista,
                anio(o.getAnioCreacion()), o.getTecnica(), o.getTematica(), o.getMovimiento(),
                o.getDimensiones(), o.getUbicacion(), o.getEnExhibicion(), o.getImagenUrl());
    }

    public static List<ObraJson> obras(List<Obra> obras) {
        return obras.stream().map(MapeadorJson::obra).toList();
    }

    public static ArtistaJson artista(Artista a) {
        return new ArtistaJson(a.getId(), a.getNombre(), a.getNacionalidad(), a.getMovimiento(),
                anio(a.getAnioNacimiento()), anio(a.getAnioFallecimiento()), a.getCantidadObras(),
                a.getBiografia());
    }

    public static EventoJson evento(Evento e) {
        return new EventoJson(e.getId(), e.getTitulo(), e.getTipo(), e.getFechaHora(),
                e.getDuracionMinutos(), e.getCupoMaximo(), e.getInscriptos());
    }

    public static CategoriaJson categoria(Categoria c) {
        return new CategoriaJson(c.getNombre(), c.getCantidadObras());
    }

    public static List<CategoriaJson> categorias(List<Categoria> categorias) {
        return categorias.stream().map(MapeadorJson::categoria).toList();
    }

    public static PreferenciasJson preferencias(Preferencias p) {
        return new PreferenciasJson(List.copyOf(p.getMovimientosList()), List.copyOf(p.getTecnicasList()),
                List.copyOf(p.getTematicasList()));
    }

    /** Campos de lista que el cliente puede omitir: se tratan como vacíos. */
    public static List<String> listaSegura(List<String> lista) {
        return lista == null ? List.of() : lista;
    }

    // El proto3 no distingue "sin dato" de 0, y el contrato pide null para los años que no se conocen.
    private static Integer anio(int valor) {
        return valor == 0 ? null : valor;
    }
}
