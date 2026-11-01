package com.unla.museo.mensajeria;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MensajeProcesadoId implements Serializable {

    @Column(name = "consumidor", length = 50)
    private String consumidor;

    @Column(name = "id_mensaje", length = 100)
    private String idMensaje;
}
