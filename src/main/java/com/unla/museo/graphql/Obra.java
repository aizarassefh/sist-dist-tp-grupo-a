package com.unla.museo.graphql;
import java.util.List;

public class Obra {

    private Long id;
    private String titulo;
    private Artista artista;
    private String imagenUrl;
    private Integer anioCreacion;
    private String tecnica;
    private String dimensiones;
    private String epoca;
    private String descripcion;
    private String ubicacion;
    private Boolean enExhibicion;
    private List<Comentario> comentarios;

    public Obra(Long id, String titulo, Artista artista, String imagenUrl,
                Integer anioCreacion, String tecnica, String dimensiones,
                String epoca, String descripcion, String ubicacion,
                Boolean enExhibicion,List<Comentario> comentarios) {

        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.imagenUrl = imagenUrl;
        this.anioCreacion = anioCreacion;
        this.tecnica = tecnica;
        this.dimensiones = dimensiones;
        this.epoca = epoca;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.enExhibicion = enExhibicion;
        this.comentarios = comentarios;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public Artista getArtista() {
        return artista;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public Integer getAnioCreacion() {
        return anioCreacion;
    }

    public String getTecnica() {
        return tecnica;
    }

    public String getDimensiones() {
        return dimensiones;
    }

    public String getEpoca() {
        return epoca;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public Boolean getEnExhibicion() {
        return enExhibicion;
    }
    public List<Comentario> getComentarios() {
        return comentarios;
    }
}