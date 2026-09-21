package com.unla.museo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "CREATION")
    private LocalDateTime creation =  LocalDateTime.now();

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CHANGED",nullable = true)
    private LocalDateTime changed;

    @Column(name = "CHANGED_BY",nullable = true)
    private String changedBy;


}
