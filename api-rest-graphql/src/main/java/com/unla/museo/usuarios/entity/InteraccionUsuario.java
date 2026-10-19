package com.unla.museo.usuarios.entity;

import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.eventos.entity.EventoEntity;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa una interacción del usuario con el sistema.
 * Almacena el historial de visitas, favoritos, valoraciones y otras interacciones.
 */
@Entity
@Table(name = "interacciones_usuario")
@Data
@NoArgsConstructor
public class InteraccionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OBRA_ID")
    private ObraEntity obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVENTO_ID")
    private EventoEntity evento;

    @Column(name = "TIPO_INTERACCION", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TipoInteraccion tipoInteraccion;

    @Column(name = "FECHA_INTERACCION", nullable = false)
    private LocalDateTime fechaInteraccion;

    @Column(name = "DURACION_SEGUNDOS")
    private Integer duracionSegundos;

    @Column(name = "VALORACION")
    private Integer valoracion;

    @Column(name = "COMENTARIO", length = 500)
    private String comentario;

    @Column(name = "DISPOSITIVO", length = 100)
    private String dispositivo;

    public enum TipoInteraccion {
        VISITA,
        FAVORITO,
        VALORACION,
        COMPARTIR,
        COMENTARIO
    }
}
