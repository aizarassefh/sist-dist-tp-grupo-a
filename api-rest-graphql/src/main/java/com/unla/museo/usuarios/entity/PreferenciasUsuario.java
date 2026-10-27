package com.unla.museo.usuarios.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Gustos declarados por el visitante. Los tres ejes coinciden con las
 * categorías del catálogo (movimiento, técnica, temática) para que el motor de
 * recomendaciones pueda cruzarlos sin traducir.
 */
@Entity
// Nombre distinto al de la primera versión de la rama gRPC: aquellas tablas
// tenían otras columnas (un ENUM con otros valores, columnas NOT NULL) y
// ddl-auto=update no las corrige; con un nombre nuevo Hibernate las crea
// bien también en las bases de desarrollo que ya corrieron esa versión.
@Table(name = "preferencias_visitante")
@Getter
@Setter
@NoArgsConstructor
public class PreferenciasUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    @CollectionTable(name = "preferencias_visitante_movimientos", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "movimiento")
    private List<String> movimientos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencias_visitante_tecnicas", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "tecnica")
    private List<String> tecnicas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "preferencias_visitante_tematicas", joinColumns = @JoinColumn(name = "preferencia_id"))
    @Column(name = "tematica")
    private List<String> tematicas = new ArrayList<>();
}
