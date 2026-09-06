package com.ocasion.backend.salones.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter 
@Setter 
@Entity
@Table(name="Servicios")
public class ServiciosEntity {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id_servicio")
    private Integer idServicio;

    @Column(name="NombreServicio", nullable = false)
    private String nombreServicio;

    @Column(name="Descripcion")
    private String descripcion;

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL)
    private List<SalonServiciosEntity> salonesAsignados;
}