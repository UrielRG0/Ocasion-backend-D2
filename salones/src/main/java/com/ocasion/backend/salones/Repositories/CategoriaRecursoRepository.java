package com.ocasion.backend.salones.Repositories;

import com.ocasion.backend.salones.Entities.CategoriaRecursoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRecursoRepository extends JpaRepository<CategoriaRecursoEntity, Integer> {
    
    // Validación para evitar borrado de categorías con stock activo (Error 409 Conflict)
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InventarioSalonEntity i WHERE i.categoria.idCategoria = :categoriaId")
    boolean existsByCategoriaIdAndInsumosNotEmpty(@Param("categoriaId") Integer categoriaId);
}