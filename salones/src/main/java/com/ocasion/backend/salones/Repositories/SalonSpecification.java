package com.ocasion.backend.salones.Repositories;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

// Importamos el DTO y la Entidad para que Java los reconozca
import com.ocasion.backend.salones.DTO.FiltroBusquedaDTO;
import com.ocasion.backend.salones.Entities.SalonEntity;

public class SalonSpecification {

    public static Specification<SalonEntity> aplicarFiltros(FiltroBusquedaDTO filtros) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicados = new ArrayList<>();

            // Solo mostrar salones publicados
            predicados.add(criteriaBuilder.equal(root.get("estado"), "PUBLICADO"));

            if (filtros.getCapacidadMin() != null) {
                predicados.add(criteriaBuilder.greaterThanOrEqualTo(root.get("capacidadPersonas"), filtros.getCapacidadMin()));
            }
            
            if (filtros.getCapacidadMax() != null) {
                predicados.add(criteriaBuilder.lessThanOrEqualTo(root.get("capacidadPersonas"), filtros.getCapacidadMax()));
            }

            if (filtros.getPrecioHoraMax() != null) {
                predicados.add(criteriaBuilder.lessThanOrEqualTo(root.get("precio_hora"), filtros.getPrecioHoraMax()));
            }

            if (filtros.getCiudad() != null && !filtros.getCiudad().isEmpty()) {
                // Asumiendo que hay un JOIN con UbicacionEntity
                predicados.add(criteriaBuilder.like(root.join("ubicacion").get("ciudad"), "%" + filtros.getCiudad() + "%"));
            }

            if (filtros.getServiciosIds() != null && !filtros.getServiciosIds().isEmpty()) {
                // El JOIN debe seguir la ruta exacta de las variables en tus Entidades de Java:
                // SalonEntity (serviciosAsignados) -> SalonServiciosEntity (servicio) -> ServiciosEntity (idServicio)
                predicados.add(root.join("serviciosAsignados").get("servicio").get("idServicio").in(filtros.getServiciosIds()));
            }

            return criteriaBuilder.and(predicados.toArray(new Predicate[0]));
        };
    }
}