package com.ocasion.backend.salones.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@Entity
@Table(name="Inventario_Salon")
public class InventarioSalonEntity {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id_recurso")
    private Integer idRecurso;

    @Column(name="nombre_recurso", nullable = false)
    private String nombreRecurso;

    @Column(name="cantidad_total", nullable = false)
    private Integer cantidadTotal = 0;

    @ManyToOne
    @JoinColumn(name = "Salonid_salon", nullable = false)
    private SalonEntity salon;

    @ManyToOne
    @JoinColumn(name = "Categoria_recursoid_categoria", nullable = false)
    private CategoriaRecursoEntity categoria;
}