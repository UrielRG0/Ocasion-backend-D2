package com.ocasion.backend.salones.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter 
@Setter 
@Entity
@Table(name="Categoria_recurso")
public class CategoriaRecursoEntity {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="Id_categoria")
    private Integer idCategoria;

    @Column(name="NombreCategoria", nullable = false, unique = true)
    private String nombreCategoria;

    @Column(name="Descripcion")
    private String descripcion;

    @OneToMany(mappedBy = "categoria")
    private List<InventarioSalonEntity> insumos;
}