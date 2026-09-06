package com.ocasion.backend.salones.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@Entity
@Table(name="Salon_Servicios")
public class SalonServiciosEntity {
    
    @EmbeddedId
    private SalonServiciosId id = new SalonServiciosId();

    @ManyToOne
    @MapsId("salonId")
    @JoinColumn(name = "Salonid_salon")
    private SalonEntity salon;

    @ManyToOne
    @MapsId("servicioId")
    @JoinColumn(name = "Serviciosid_servicio")
    private ServiciosEntity servicio;

    @Column(name="numeroServiciosSolicitados")
    private Integer numeroServiciosSolicitados = 0;
}