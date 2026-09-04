package com.ocasion.backend.salones.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ocasion.backend.salones.Entities.DisponibilidadEntity;

@Repository
public interface DisponibilidadRepository extends JpaRepository<DisponibilidadEntity, Integer> {}
