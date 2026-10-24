package com.unla.museo.usuarios.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa las preferencias de un usuario.
 * Almacena los estilos, técnicas, artistas y temas de interés del usuario.
 */
@Entity
@Table(name = "preferencias_usuario")
@Data
@NoArgsConstructor
public class PreferenciasUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    @CollectionTable(name = "preferencias_estilos", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "estilo")
    private List<String> estilosFavoritos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencias_tecnicas", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "tecnica")
    private List<String> tecnicasFavoritas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencias_artistas", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "artista_id")
    private List<Long> artistasFavoritos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencias_temas", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "tema")
    private List<String> temasInteres = new ArrayList<>();

    @Column(name = "umbral_calificacion")
    private Double umbralCalificacion = 3.0;
}
