package com.ocasion.backend.salones.Entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Getter 
@Setter 
@Entity 
@Table(name = "Disponibilidad")
public class DisponibilidadEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_disponibilidad")
    private Integer id_disponibilidad;

    @Column(name="hora_inicio",nullable = false)
    private LocalDateTime hora_inicio;

    @Column(name="hora_fin",nullable = false)
    private LocalDateTime hora_fin;

    @Column(name="fecha",nullable = false)
    private LocalDate fecha;
    @Column(name="observaciones")
    private String observaciones;
    @ManyToOne
    @JoinColumn(name = "Salonid_salon", nullable = false)
    private SalonEntity salon;
}  
