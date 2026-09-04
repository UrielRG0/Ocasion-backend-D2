package com.ocasion.backend.salones.Entities;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Getter 
@Setter 
@Entity 
@Table(name = "Ubicacion")
public class UbicacionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_ubicacion")
    private Integer id_ubicacion;
    @Column(name="direccion",nullable = false)
    private String direccion;

    @Column(name="latitud",nullable = false, precision = 10, scale = 8)
    private BigDecimal latitud;

    @Column(name="longitud",nullable = false, precision = 11, scale = 8)
    private BigDecimal longitud;

    @Column(name="ciudad",nullable = false)
    private String ciudad;

    @Column(name="CP",nullable = false)
    private String CP;

    @OneToOne
    @JoinColumn(name = "Salonid_salon", nullable = false, unique = true)
    private SalonEntity salon;

}
