package com.ocasion.backend.salones.DTO;

import java.time.LocalDate;
import java.time.LocalTime;

/** Horario diario de atención desde la fecha de apertura. */
public record HorarioSalonDTO(LocalDate fechaDesde, LocalTime horaInicio, LocalTime horaFin) {}
