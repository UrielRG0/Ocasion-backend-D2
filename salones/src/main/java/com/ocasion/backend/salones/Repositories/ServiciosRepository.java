package com.ocasion.backend.salones.Repositories;

import com.ocasion.backend.salones.Entities.ServiciosEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiciosRepository extends JpaRepository<ServiciosEntity, Integer> {
    // Método útil para cuando crees el servicio propio en el catálogo
    boolean existsByNombreServicio(String nombreServicio);
}