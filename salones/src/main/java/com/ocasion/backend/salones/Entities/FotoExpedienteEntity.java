package com.ocasion.backend.salones.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "FotoExpediente")
@Getter
@Setter
public class FotoExpedienteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_expediente", nullable = false)
    @JsonIgnore
    private DocumentosValidacionSalonEntity expediente;
    @Column(nullable = false, length = 30)
    private String tipo;
    @Column(nullable = false)
    private int posicion;
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] contenido;
}
