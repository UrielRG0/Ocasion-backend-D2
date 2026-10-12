package com.ocasion.backend.salones.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DisponibilidadMesDTO {
    private LocalDate fecha;
    private LocalTime hora_inicio;
    private LocalTime hora_fin;
    private String tipo_bloqueo; 
}