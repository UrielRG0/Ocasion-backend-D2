package com.ocasion.backend.salones.Repositories;

import com.ocasion.backend.salones.DTO.*;
import com.ocasion.backend.salones.Entities.SalonEntity;
import com.ocasion.backend.salones.Services.ImagenesSalon;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;

/** Proyección escalar: el listado nunca carga expedientes ni binarios de fotografías. */
@Repository
public class CatalogoConsulta {
    @PersistenceContext private EntityManager em;

    public Page<SalonCardResponseDTO> buscar(FiltroBusquedaDTO filtros, Pageable pageable) {
        var cb=em.getCriteriaBuilder();
        var query=cb.createTupleQuery();
        var salon=query.from(SalonEntity.class);
        var ubicacion=salon.join("ubicacion",JoinType.LEFT);
        var expediente=salon.join("documentosValidacionSalonEntity",JoinType.LEFT);
        query.multiselect(salon.get("id_salon"),salon.get("nombreSalon"),salon.get("capacidadPersonas"),
            salon.get("precio_hora"),salon.get("imagen"),ubicacion.get("ciudad"),ubicacion.get("direccion"),
            expediente.get("estadoRevisionAdmin"));
        query.where(SalonSpecification.aplicarFiltros(filtros).toPredicate(salon,query,cb));
        var filas=em.createQuery(query).setFirstResult(Math.toIntExact(pageable.getOffset()))
            .setMaxResults(pageable.getPageSize()).getResultList();
        var count=cb.createQuery(Long.class);
        var root=count.from(SalonEntity.class);
        count.select(cb.count(root)).where(SalonSpecification.aplicarFiltros(filtros).toPredicate(root,count,cb));
        long total=em.createQuery(count).getSingleResult();
        if(filas.isEmpty()) return new PageImpl<>(List.of(),pageable,total);

        Map<Integer,SalonCardResponseDTO> tarjetas=new LinkedHashMap<>();
        for(var fila:filas) {
            var dto=new SalonCardResponseDTO();
            dto.setId_salon(fila.get(0,Integer.class)); dto.setNombreSalon(fila.get(1,String.class));
            dto.setCapacidadPersonas(fila.get(2,Integer.class));
            var precio=fila.get(3,BigDecimal.class); dto.setPrecio_hora(precio==null?null:precio.doubleValue());
            dto.setImagen_portada(ImagenesSalon.antiguas(fila.get(4,String.class)).stream().findFirst().orElse(null));
            dto.setCiudad(fila.get(5,String.class)); dto.setDireccion(fila.get(6,String.class));
            dto.setVerificado("APROBADO".equals(fila.get(7,String.class)));
            dto.setServiciosDestacados(new ArrayList<>()); tarjetas.put(dto.getId_salon(),dto);
        }
        // Una consulta por colección para toda la página; sin fetch join de colecciones paginadas.
        var fotos=em.createQuery("select f.salon.id_salon, f.id from ImagenSalonEntity f where f.salon.id_salon in :ids order by f.posicion, f.id",Object[].class)
            .setParameter("ids",tarjetas.keySet()).getResultList();
        var conPortada=new HashSet<Integer>();
        for(var foto:fotos) {
            Integer id=(Integer)foto[0];
            if(conPortada.add(id)) tarjetas.get(id).setImagen_portada("/api/v1/salones/"+id+"/imagenes/"+foto[1]);
        }
        var servicios=em.createQuery("select r.salon.id_salon, r.servicio.nombreServicio from SalonServiciosEntity r where r.salon.id_salon in :ids order by r.servicio.idServicio",Object[].class)
            .setParameter("ids",tarjetas.keySet()).getResultList();
        for(var servicio:servicios) {
            var nombres=tarjetas.get((Integer)servicio[0]).getServiciosDestacados();
            if(nombres.size()<4) nombres.add((String)servicio[1]);
        }
        return new PageImpl<>(new ArrayList<>(tarjetas.values()),pageable,total);
    }
}
