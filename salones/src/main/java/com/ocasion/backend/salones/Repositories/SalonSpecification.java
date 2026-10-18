package com.ocasion.backend.salones.Repositories;

import com.ocasion.backend.salones.DTO.FiltroBusquedaDTO;
import com.ocasion.backend.salones.Entities.SalonEntity;
import com.ocasion.backend.salones.Entities.SalonServiciosEntity;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalonSpecification {
    public static Specification<SalonEntity> aplicarFiltros(FiltroBusquedaDTO filtros) {
        return (root, query, cb) -> {
            // Criteria usa el atributo literal; Sort interpretaría el guion bajo como una ruta.
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(cb.asc(root.get("id_salon")));
            }
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("estado"), "PUBLICADO"));
            if (filtros.getCapacidadMin() != null)
                condiciones.add(cb.ge(root.get("capacidadPersonas"), filtros.getCapacidadMin()));
            if (filtros.getCapacidadMax() != null)
                condiciones.add(cb.le(root.get("capacidadPersonas"), filtros.getCapacidadMax()));
            if (filtros.getPrecioHoraMax() != null)
                condiciones.add(cb.le(root.get("precio_hora"), BigDecimal.valueOf(filtros.getPrecioHoraMax())));

            if (filtros.getCiudad() != null && !filtros.getCiudad().isBlank()) {
                var ubicacion = root.join("ubicacion", JoinType.LEFT);
                String lugar = filtros.getCiudad().trim();
                condiciones.add(cb.or(
                    cb.like(cb.lower(ubicacion.get("ciudad")), patron(lugar), '\\'),
                    cb.equal(ubicacion.get("CP"), lugar)));
            }
            if (filtros.getTexto() != null && !filtros.getTexto().isBlank()) {
                String texto = patron(filtros.getTexto().trim());
                var ubicacion = root.join("ubicacion", JoinType.LEFT);
                condiciones.add(cb.or(
                    cb.like(cb.lower(root.get("nombreSalon")), texto, '\\'),
                    cb.like(cb.lower(root.get("descripcion")), texto, '\\'),
                    cb.like(cb.lower(ubicacion.get("ciudad")), texto, '\\'),
                    cb.equal(ubicacion.get("CP"), filtros.getTexto().trim())));
            }
            // Exigir todos los servicios sin multiplicar filas ni alterar la paginación.
            if (filtros.getServiciosIds() != null) {
                for (Integer id : filtros.getServiciosIds().stream().distinct().toList()) {
                    var servicio = query.subquery(Integer.class);
                    var relacion = servicio.from(SalonServiciosEntity.class);
                    servicio.select(cb.literal(1)).where(
                        cb.equal(relacion.get("salon"), root),
                        cb.equal(relacion.get("servicio").get("idServicio"), id));
                    condiciones.add(cb.exists(servicio));
                }
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    private static String patron(String texto) {
        return "%" + texto.toLowerCase(Locale.ROOT).replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
