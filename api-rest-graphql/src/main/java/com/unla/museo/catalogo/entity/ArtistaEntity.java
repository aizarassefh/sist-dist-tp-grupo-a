package com.unla.museo.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "ARTISTAS")
public class ArtistaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 200)
    private String nombre;

    @Column(name = "BIOGRAFIA", columnDefinition = "TEXT")
    private String biografia;

    @Column(name = "NACIONALIDAD", length = 100)
    private String nacionalidad;

    @Column(name = "ANIO_NACIMIENTO")
    private Integer anioNacimiento;

    @Column(name = "ANIO_FALLECIMIENTO")
    private Integer anioFallecimiento;

    @Column(name = "MOVIMIENTO", length = 100)
    private String movimiento;
}
