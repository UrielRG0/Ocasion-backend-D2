package com.ocasion.backend.salones.Repositories;

import com.ocasion.backend.salones.Entities.InventarioSalonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventarioSalonRepository extends JpaRepository<InventarioSalonEntity, Integer> {
    
    // Validación de límite de 20 recursos por categoría para un salón específico
    @Query("SELECT COUNT(i) FROM InventarioSalonEntity i WHERE i.salon.id_salon = :salonId AND i.categoria.idCategoria = :categoriaId")
    long countBySalonIdAndCategoriaId(@Param("salonId") Integer salonId, @Param("categoriaId") Integer categoriaId);
    
    // Validación de límite de 8 categorías distintas en el inventario de un salón
    @Query("SELECT COUNT(DISTINCT i.categoria.idCategoria) FROM InventarioSalonEntity i WHERE i.salon.id_salon = :salonId")
    long countCategoriasBySalonId(@Param("salonId") Integer salonId);
}