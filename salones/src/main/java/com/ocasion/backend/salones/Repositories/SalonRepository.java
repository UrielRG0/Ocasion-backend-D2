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
    interface EstadoPropio {
        String getEstado();
        Integer getIdExpediente();
        String getRevision();
        String getObservaciones();
        java.time.Instant getFechaResolucion();
        java.time.Instant getArchivosEliminadosEn();
    }
    @org.springframework.data.jpa.repository.Query("""
        select s.estado as estado, d.idExpediente as idExpediente, d.estadoRevisionAdmin as revision,
               d.observacionesAdmin as observaciones, d.fechaResolucion as fechaResolucion,
               d.archivosEliminadosEn as archivosEliminadosEn
        from SalonEntity s left join s.documentosValidacionSalonEntity d
        where s.id_salon = :id and s.usuariosIdUser = :propietario
        """)
    java.util.Optional<EstadoPropio> findEstadoPropio(@org.springframework.data.repository.query.Param("id") Integer id,
        @org.springframework.data.repository.query.Param("propietario") Integer propietario);
    @org.springframework.data.jpa.repository.Query("select count(s) from SalonEntity s where s.id_salon = :id and s.usuariosIdUser = :propietario")
    long countPropio(@org.springframework.data.repository.query.Param("id") Integer id,
        @org.springframework.data.repository.query.Param("propietario") Integer propietario);
    interface PendienteResumen {
        Integer getId();
        String getNombre();
        Integer getPropietario();
        java.time.LocalDateTime getFecha();
    }
    @org.springframework.data.jpa.repository.Query(value = "select s.id_salon as id, s.nombreSalon as nombre, s.usuariosIdUser as propietario, s.fechaSolicitud as fecha from SalonEntity s join s.documentosValidacionSalonEntity d where s.estado = 'PUBLICADO' and d.estadoRevisionAdmin = 'PENDIENTE'",
        countQuery = "select count(s) from SalonEntity s join s.documentosValidacionSalonEntity d where s.estado = 'PUBLICADO' and d.estadoRevisionAdmin = 'PENDIENTE'")
    Page<PendienteResumen> findPendientesResumen(Pageable pageable);
    // Buscar salones de un propietario específico
    List<SalonEntity> findByUsuariosIdUser(Integer usuariosIdUser);
    Page<SalonEntity> findByEstado(String estado, Pageable pageable);
    Page<SalonEntity> findByEstadoAndDocumentosValidacionSalonEntity_EstadoRevisionAdmin(String estadoSalon, String estadoRevision, Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from SalonEntity s where s.id_salon = :id")
    java.util.Optional<SalonEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
}
