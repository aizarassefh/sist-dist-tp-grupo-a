package com.unla.museo.mensajeria;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

/**
 * Marca "este consumidor ya procesó este mensaje". Implementa Persistable
 * para que save() haga siempre INSERT: con una clave asignada, Spring Data
 * haría merge, y un duplicado se convertiría en un UPDATE silencioso en lugar
 * de la violación de unicidad que detecta la carrera entre dos consumidores.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "mensajes_procesados")
public class MensajeProcesadoEntity implements Persistable<MensajeProcesadoId> {

    @EmbeddedId
    private MensajeProcesadoId id;

    @Column(name = "fecha_procesamiento", nullable = false)
    private Instant fechaProcesamiento;

    public MensajeProcesadoEntity(MensajeProcesadoId id, Instant fechaProcesamiento) {
        this.id = id;
        this.fechaProcesamiento = fechaProcesamiento;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
