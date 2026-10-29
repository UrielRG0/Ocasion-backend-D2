package com.ocasion.backend.salones.DTO;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpedienteAdminDTO {

    private Integer id_Salon;
    private String nombreSalon;
    private String descripcion;
    private BigDecimal precioPorHora;
    private Integer capacidadPersonas;
    private String direccion;
    private String estado;
    private String estadoRevisionAdmin;

    // NUEVO
    private List<String> servicios;

    private DocumentosUrlsDTO documentos;
}