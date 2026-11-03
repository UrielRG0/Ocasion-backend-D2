package com.ocasion.backend.salones.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Se carga únicamente al solicitar el archivo, no al listar las tarjetas. */
@Entity @Table(name="salon_imagen_contenido") @Getter @Setter
public class ContenidoImagenSalonEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Lob @Column(nullable=false, columnDefinition="LONGBLOB")
    private byte[] bytes;
}
