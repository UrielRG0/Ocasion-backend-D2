package com.ocasion.backend.salones.Repositories;

import com.ocasion.backend.salones.Entities.ImagenSalonEntity;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ImagenLecturaRepository extends Repository<ImagenSalonEntity,Long> {
    interface Metadatos {
        String getEstado();
        Integer getPropietario();
        String getTipo();
        Integer getTamano();
    }
    @Query("select f.salon.estado as estado, f.salon.usuariosIdUser as propietario, f.contentType as tipo, f.tamano as tamano from ImagenSalonEntity f where f.id = :foto and f.salon.id_salon = :salon")
    Optional<Metadatos> metadatos(@Param("salon") Integer salon,@Param("foto") Long foto);
    @Query("select f.archivo.bytes from ImagenSalonEntity f where f.id = :foto and f.salon.id_salon = :salon")
    byte[] contenido(@Param("salon") Integer salon,@Param("foto") Long foto);
}
