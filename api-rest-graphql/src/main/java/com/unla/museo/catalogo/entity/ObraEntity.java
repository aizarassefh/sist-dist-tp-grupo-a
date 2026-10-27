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

    @Column(name = "TEMATICA")
    private String tematica;

    @Column(name = "UBICACION")
    private String ubicacion;

    @Column(name = "EN_EXHIBICION", nullable = false)
    private boolean enExhibicion;


}
