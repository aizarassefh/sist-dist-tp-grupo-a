package com.unla.museo.seguridad.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Data
@Table(name = "USUARIOS")
public class UsuarioEntity extends AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "EMAIL", unique = true, nullable = false)
    private String email;

    @Column(name = "NOMBRE")
    private String nombre;

    @Column(name = "APELLIDO")
    private String apellido;

    @Column(name = "TELEFONO")
    private String telefono;

    @Column(name = "ACTIVO")
    private Boolean activo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ROL")
    private RolEntity rol;

    @Column(name = "PASSWORD")
    private String password;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "PREFERENCIAS_ID")
    private com.unla.museo.usuarios.entity.PreferenciasUsuario preferencias;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<com.unla.museo.usuarios.entity.InteraccionUsuario> historialVisitas = new java.util.ArrayList<>();

    public com.unla.museo.usuarios.entity.PreferenciasUsuario getPreferencias() {
        if (preferencias == null) {
            preferencias = new com.unla.museo.usuarios.entity.PreferenciasUsuario();
        }
        return preferencias;
    }

    public void setPreferencias(com.unla.museo.usuarios.entity.PreferenciasUsuario preferencias) {
        this.preferencias = preferencias;
    }

    public java.util.List<com.unla.museo.usuarios.entity.InteraccionUsuario> getHistorialVisitas() {
        return historialVisitas;
    }

    public void setHistorialVisitas(java.util.List<com.unla.museo.usuarios.entity.InteraccionUsuario> historialVisitas) {
        this.historialVisitas = historialVisitas;
    }

    /**
     * Alias para creacion (usado por proto)
     * Heredado de AuditoriaEntity
     */
    public LocalDateTime getFechaRegistro() {
        return getCreacion();
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        setCreacion(fechaRegistro);
    }
}
