package com.ocasion.backend.salones.DTO;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class SalonPendienteAdminDTO {
    private Integer id_salon;
    private String nombreSalon;
    private String propietarioNombre; 
    private LocalDateTime fechaSolicitud; 
}