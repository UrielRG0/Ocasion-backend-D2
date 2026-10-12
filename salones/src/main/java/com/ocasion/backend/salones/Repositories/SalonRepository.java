package com.ocasion.backend.salones.Repositories;

import java.util.List;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ocasion.backend.salones.Entities.SalonEntity;

@Repository
public interface SalonRepository extends JpaRepository<SalonEntity, Integer>, JpaSpecificationExecutor<SalonEntity> {
    // Buscar salones de un propietario específico
    List<SalonEntity> findByUsuariosIdUser(Integer usuariosIdUser);
    Page<SalonEntity> findByEstado(String estado, Pageable pageable);
}
