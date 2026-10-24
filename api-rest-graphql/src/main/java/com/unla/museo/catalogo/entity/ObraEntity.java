package com.unla.museo.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "OBRAS", indexes = {
        @Index(name = "idx_obra_artista_id", columnList = "ARTISTA_ID")
})
public class ObraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TITULO", nullable = false, length = 300)
    private String titulo;

    @Column(name = "DESCRIPCION", columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ARTISTA_ID", nullable = false)
    private ArtistaEntity artista;

    @Column(name = "IMAGEN_URL")
    private String imagenUrl;

    @Column(name = "ANIO_CREACION")
    private Integer anioCreacion;

    @Column(name = "TECNICA")
    private String tecnica;

    @Column(name = "DIMENSIONES")
    private String dimensiones;

    @Column(name = "EPOCA")
    private String epoca;

    @Column(name = "ESTILO")
    private String estilo;

    @Column(name = "UBICACION")
    private String ubicacion;

    @Column(name = "EN_EXHIBICION", nullable = false)
    private boolean enExhibicion;

    @Column(name = "VISITAS")
    private Integer visitas = 0;

    @Column(name = "CALIFICACION_PROMEDIO")
    private Double calificacionPromedio = 0.0;

    @Column(name = "TOTAL_VALORACIONES")
    private Integer totalValoraciones = 0;

    // Estas tres columnas se agregaron con ddl-auto=update: en una base que ya
    // tenía obras quedan en NULL, y el valor inicial de arriba solo aplica a
    // las obras nuevas. Sin esto, cualquier unboxing tira NullPointerException.
    public Integer getVisitas() {
        return visitas != null ? visitas : 0;
    }

    public Double getCalificacionPromedio() {
        return calificacionPromedio != null ? calificacionPromedio : 0.0;
    }

    public Integer getTotalValoraciones() {
        return totalValoraciones != null ? totalValoraciones : 0;
    }

    /**
     * Agrega una nueva valoración y recalcula el promedio
     */
    public void agregarValoracion(int valoracion) {
        if (this.totalValoraciones == null) {
            this.totalValoraciones = 0;
        }
        if (this.calificacionPromedio == null) {
            this.calificacionPromedio = 0.0;
        }

        double sumaTotal = this.calificacionPromedio * this.totalValoraciones;
        this.totalValoraciones++;
        this.calificacionPromedio = (sumaTotal + valoracion) / this.totalValoraciones;
    }

    /**
     * Incrementa el contador de visitas
     */
    public void incrementarVisitas() {
        if (this.visitas == null) {
            this.visitas = 0;
        }
        this.visitas++;
    }

    /**
     * Getter alias para anioCreacion (para compatibilidad con gRPC)
     */
    public Integer getAnio() {
        return anioCreacion;
    }

    /**
     * Setter alias para anioCreacion (para compatibilidad con gRPC)
     */
    public void setAnio(Integer anio) {
        this.anioCreacion = anio;
    }
}
