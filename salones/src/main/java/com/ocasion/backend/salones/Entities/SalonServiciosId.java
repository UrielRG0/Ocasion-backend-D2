package com.ocasion.backend.salones.Entities;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class SalonServiciosId implements Serializable {
    private Integer salonId;
    private Integer servicioId;
    
    public SalonServiciosId() {}
    
    public SalonServiciosId(Integer salonId, Integer servicioId) {
        this.salonId = salonId;
        this.servicioId = servicioId;
    }
}