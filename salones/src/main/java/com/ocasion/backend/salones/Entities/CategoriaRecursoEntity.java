package com.ocasion.backend.salones.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    // Ignorado en el JSON: si un día se serializa junto con InventarioSalonEntity.categoria,
    // provocaría el mismo ciclo infinito que ya tuvimos con Salon <-> Inventario.
    @OneToMany(mappedBy = "categoria")
    @JsonIgnore
    private List<InventarioSalonEntity> insumos;
}