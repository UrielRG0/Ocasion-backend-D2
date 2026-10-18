package com.ocasion.backend.salones.DTO;

import lombok.Data;
import java.util.List;

@Data
public class FiltroBusquedaDTO {
    private Integer capacidadMin;
    private Integer capacidadMax;
    private Double precioHoraMax;
    private String ciudad;
    private List<Integer> serviciosIds;
}
