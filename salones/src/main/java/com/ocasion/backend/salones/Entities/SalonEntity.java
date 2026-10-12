package com.ocasion.backend.salones.Entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Getter 
@Setter 
@Entity
@Table(name="Salon")
public class SalonEntity {
    public SalonEntity(){};
    
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id_salon")
    private Integer id_salon;
    @Column(name="nombreSalon", nullable = false)
    private String nombreSalon;
    @Column(name="capacidadPersonas")
    private Integer capacidadPersonas;
    @Column(name="descripcion")
    private String descripcion;
    @Column(name= "precio_hora",nullable = false, precision = 10, scale = 2)
    private BigDecimal precio_hora;
    @Column(name = "Usuariosid_user", nullable = false)
    private Integer usuariosIdUser;
    @Column(name="imagen")
    private String imagen;
    @Column(name="estado",nullable = false)
    private  String estado;
                   
    @OneToOne(mappedBy = "salon", cascade = CascadeType.ALL)
    @JsonManagedReference 
    private UbicacionEntity ubicacion;
    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference 
    private List<DisponibilidadEntity> disponibilidades;

    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<SalonServiciosEntity> serviciosAsignados;
    @CreationTimestamp 
    @Column(name = "fecha_solicitud", updatable = false)
    private LocalDateTime fechaSolicitud;
    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<InventarioSalonEntity> inventario;
    @OneToOne(mappedBy = "salon",cascade = CascadeType.ALL,fetch = FetchType.LAZY,orphanRemoval = true )
    @JsonManagedReference 
    private DocumentosValidacionSalonEntity documentosValidacionSalonEntity;
}
