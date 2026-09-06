package com.ocasion.backend.salones.Repositories;

import com.ocasion.backend.salones.Entities.SalonServiciosEntity;
import com.ocasion.backend.salones.Entities.SalonServiciosId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SalonServiciosRepository extends JpaRepository<SalonServiciosEntity, SalonServiciosId> {
    
    // Consulta para verificar el límite de servicios asignados (list.size() <= 10)
    @Query("SELECT COUNT(ss) FROM SalonServiciosEntity ss WHERE ss.salon.id_salon = :salonId")
    long countBySalonId(@Param("salonId") Integer salonId);
}