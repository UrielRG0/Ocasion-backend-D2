package com.ocasion.backend.salones.Repositories;

import com.ocasion.backend.salones.Entities.InventarioSalonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventarioSalonRepository extends JpaRepository<InventarioSalonEntity, Integer> {
    @Query("SELECT COUNT(s) FROM SalonEntity s WHERE s.id_salon = :salonId AND s.estado = 'PUBLICADO'")
    long countSalonPublicado(@Param("salonId") Integer salonId);

    @Query("SELECT new com.ocasion.backend.salones.DTO.RecursoResponseDTO(i.idRecurso, i.nombreRecurso, i.cantidadTotal) FROM InventarioSalonEntity i WHERE i.salon.id_salon = :salonId AND i.salon.estado = 'PUBLICADO' ORDER BY i.idRecurso")
    java.util.List<com.ocasion.backend.salones.DTO.RecursoResponseDTO> findBySalonId(@Param("salonId") Integer salonId);
    
    // Validación de límite de 20 recursos por categoría para un salón específico
    @Query("SELECT COUNT(i) FROM InventarioSalonEntity i WHERE i.salon.id_salon = :salonId AND i.categoria.idCategoria = :categoriaId")
    long countBySalonIdAndCategoriaId(@Param("salonId") Integer salonId, @Param("categoriaId") Integer categoriaId);
    
    // Validación de límite de 8 categorías distintas en el inventario de un salón
    @Query("SELECT COUNT(DISTINCT i.categoria.idCategoria) FROM InventarioSalonEntity i WHERE i.salon.id_salon = :salonId")
    long countCategoriasBySalonId(@Param("salonId") Integer salonId);
}
