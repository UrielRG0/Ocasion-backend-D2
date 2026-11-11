package com.ocasion.backend.salones.Repositories;

import com.ocasion.backend.salones.DTO.SalonPropietarioDTO;
import com.ocasion.backend.salones.Services.ImagenesSalon;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@Transactional(readOnly=true)
public class PropietarioConsulta {
    @PersistenceContext private EntityManager em;

    public List<SalonPropietarioDTO> buscar(Integer propietario) {
        var filas=em.createQuery("""
            select s.id_salon, s.nombreSalon, s.capacidadPersonas, s.descripcion, s.precio_hora,
                   s.usuariosIdUser, s.estado, s.fechaSolicitud, s.imagen,
                   u.id_ubicacion, u.direccion, u.latitud, u.longitud, u.ciudad, u.CP,
                   d.idExpediente, d.estadoRevisionAdmin, d.observacionesAdmin
            from SalonEntity s left join s.ubicacion u left join s.documentosValidacionSalonEntity d
            where s.usuariosIdUser = :propietario order by s.id_salon
            """,Object[].class).setParameter("propietario",propietario).getResultList();
        Map<Integer,SalonPropietarioDTO> salones=new LinkedHashMap<>();
        for(var f:filas) {
            var s=new SalonPropietarioDTO();
            s.setId_salon((Integer)f[0]); s.setNombreSalon((String)f[1]); s.setCapacidadPersonas((Integer)f[2]);
            s.setDescripcion((String)f[3]); s.setPrecio_hora((BigDecimal)f[4]); s.setUsuariosIdUser((Integer)f[5]);
            s.setEstado((String)f[6]); s.setFechaSolicitud((LocalDateTime)f[7]);
            s.setImagenes(new ArrayList<>(ImagenesSalon.antiguas((String)f[8])));
            if(f[9]!=null) s.setUbicacion(new SalonPropietarioDTO.Ubicacion((Integer)f[9],(String)f[10],
                (BigDecimal)f[11],(BigDecimal)f[12],(String)f[13],(String)f[14]));
            if(f[15]!=null) s.setDocumentosValidacionSalonEntity(new SalonPropietarioDTO.Documentos(
                (Integer)f[15],(String)f[16],(String)f[17]));
            s.setVerificado("APROBADO".equals(f[16])); salones.put(s.getId_salon(),s);
        }
        if(salones.isEmpty()) return List.of();
        var ids=salones.keySet();
        var fotos=em.createQuery("select f.salon.id_salon, f.id from ImagenSalonEntity f where f.salon.id_salon in :ids order by f.posicion, f.id",Object[].class)
            .setParameter("ids",ids).getResultList();
        Set<Integer> conFotos=new HashSet<>();
        for(var f:fotos) {
            var id=(Integer)f[0]; var urls=salones.get(id).getImagenes();
            if(conFotos.add(id)) urls.clear();
            urls.add("/api/v1/salones/"+id+"/imagenes/"+f[1]);
        }
        var servicios=em.createQuery("""
            select r.salon.id_salon, r.servicio.idServicio, r.servicio.nombreServicio,
                   r.servicio.descripcion, r.numeroServiciosSolicitados
            from SalonServiciosEntity r where r.salon.id_salon in :ids order by r.servicio.idServicio
            """,Object[].class).setParameter("ids",ids).getResultList();
        for(var f:servicios) {
            var id=(Integer)f[0]; var servicioId=(Integer)f[1];
            salones.get(id).getServiciosAsignados().add(new SalonPropietarioDTO.ServicioAsignado(
                new SalonPropietarioDTO.IdServicio(id,servicioId),
                new SalonPropietarioDTO.Servicio(servicioId,(String)f[2],(String)f[3]),(Integer)f[4]));
        }
        var recursos=em.createQuery("""
            select i.salon.id_salon, i.idRecurso, i.nombreRecurso, i.cantidadTotal,
                   i.categoria.idCategoria, i.categoria.nombreCategoria, i.categoria.descripcion
            from InventarioSalonEntity i where i.salon.id_salon in :ids order by i.idRecurso
            """,Object[].class).setParameter("ids",ids).getResultList();
        for(var f:recursos) salones.get((Integer)f[0]).getInventario().add(new SalonPropietarioDTO.Inventario(
            (Integer)f[1],(String)f[2],(Integer)f[3],new SalonPropietarioDTO.Categoria((Integer)f[4],(String)f[5],(String)f[6])));
        var horarios=em.createQuery("""
            select d.salon.id_salon, d.id_disponibilidad, d.hora_inicio, d.hora_fin, d.fecha, d.observaciones
            from DisponibilidadEntity d where d.salon.id_salon in :ids order by d.fecha, d.id_disponibilidad
            """,Object[].class).setParameter("ids",ids).getResultList();
        for(var f:horarios) salones.get((Integer)f[0]).getDisponibilidades().add(new SalonPropietarioDTO.Disponibilidad(
            (Integer)f[1],(LocalDateTime)f[2],(LocalDateTime)f[3],(LocalDate)f[4],(String)f[5]));
        return new ArrayList<>(salones.values());
    }
}
