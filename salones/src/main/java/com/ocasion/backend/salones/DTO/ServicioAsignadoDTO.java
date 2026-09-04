package com.ocasion.backend.salones.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class ServicioAsignadoDTO {
    @NotNull
    private Integer id_servicio;
    private Integer numeroServiciosSolicitados;

}
