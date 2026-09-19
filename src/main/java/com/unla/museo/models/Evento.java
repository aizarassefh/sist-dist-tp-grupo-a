package com.unla.museo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter 
@Setter 
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String tipoEvento; // ej. Visitas Guiadas, Talleres
    private Integer inscriptos;
    private Integer cupoMaximo;
    private LocalDateTime fechaHora;

    
}