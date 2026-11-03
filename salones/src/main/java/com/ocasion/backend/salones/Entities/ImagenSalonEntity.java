package com.ocasion.backend.salones.Entities;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="salon_imagen") @Getter @Setter
public class ImagenSalonEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="salon_id", nullable=false) @JsonIgnore
    private SalonEntity salon;
    @Column(nullable=false) private int posicion;
    @Column(nullable=false, length=32) private String contentType;
    @Column(nullable=false) private int tamano;
    @OneToOne(fetch=FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval=true, optional=false)
    @JoinColumn(name="contenido_id", nullable=false, unique=true) @JsonIgnore
    private ContenidoImagenSalonEntity archivo;

    @Transient @JsonIgnore
    public byte[] getContenido() { return archivo.getBytes(); }
    public void setContenido(byte[] bytes) {
        archivo = new ContenidoImagenSalonEntity(); archivo.setBytes(bytes);
    }
}
