package com.unla.museo.mensajeria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "mensajes_fallidos")
public class MensajeFallidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null si el contenido no se pudo leer como JSON.
    @Column(name = "id_mensaje", length = 36)
    private String idMensaje;

    // null si el DLT no trae el grupo de origen.
    @Column(name = "consumidor", length = 50)
    private String consumidor;

    @Column(name = "error", length = 2000)
    private String error;

    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "fecha_recepcion", nullable = false)
    private Instant fechaRecepcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoFallido estado;
}
