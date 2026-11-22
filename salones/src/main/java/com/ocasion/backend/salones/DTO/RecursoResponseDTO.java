package com.ocasion.backend.salones.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecursoResponseDTO {
    @JsonProperty("id_recurso")
    private Integer id_recurso;
    @JsonProperty("nombre_recurso")
    private String nombre_recurso;
    @JsonProperty("cantidad_total")
    private Integer cantidad_total;

    public RecursoResponseDTO() {}
    public RecursoResponseDTO(Integer id_recurso, String nombre_recurso, Integer cantidad_total) {
        this.id_recurso = id_recurso;
        this.nombre_recurso = nombre_recurso;
        this.cantidad_total = cantidad_total;
    }
    public Integer getId_recurso() { return id_recurso; }
    public String getNombre_recurso() { return nombre_recurso; }
    public Integer getCantidad_total() { return cantidad_total; }
    public void setId_recurso(Integer id_recurso) { this.id_recurso = id_recurso; }
    public void setNombre_recurso(String nombre_recurso) { this.nombre_recurso = nombre_recurso; }
    public void setCantidad_total(Integer cantidad_total) { this.cantidad_total = cantidad_total; }
}
