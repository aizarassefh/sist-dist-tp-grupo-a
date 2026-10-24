package com.unla.museo.seguridad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@MappedSuperclass
public abstract class AuditoriaEntity {

    @Column(name = "CREACION")
    private LocalDateTime creacion =  LocalDateTime.now();

    @Column(name = "CREADO_POR")
    private String creadoPor;

}
