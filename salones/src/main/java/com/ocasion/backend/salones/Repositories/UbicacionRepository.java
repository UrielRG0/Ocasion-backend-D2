package com.ocasion.backend.salones.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ocasion.backend.salones.Entities.UbicacionEntity;

@Repository
public interface UbicacionRepository extends JpaRepository<UbicacionEntity, Integer> {}