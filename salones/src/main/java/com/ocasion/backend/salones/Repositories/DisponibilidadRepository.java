package com.ocasion.backend.salones.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ocasion.backend.salones.Entities.DisponibilidadEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisponibilidadRepository extends JpaRepository<DisponibilidadEntity, Integer> {
    
    @Query("SELECT d FROM DisponibilidadEntity d WHERE d.salon.id_salon = :idSalon AND d.fecha BETWEEN :inicioMes AND :finMes")
    List<DisponibilidadEntity> findDisponibilidadPorMes(
        @Param("idSalon") Integer idSalon, 
        @Param("inicioMes") LocalDate inicioMes, 
        @Param("finMes") LocalDate finMes
    );
}