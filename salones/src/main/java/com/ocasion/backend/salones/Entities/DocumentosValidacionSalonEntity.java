package com.ocasion.backend.salones.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "DocumentosValidacionSalon")
public class DocumentosValidacionSalonEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_expediente")
    private Integer idExpediente;

    @Lob
    @Column(name = "INEpropietarioPasaporte", columnDefinition = "LONGBLOB")
    private byte[] inePropietarioPasaporte;

    @Lob
    @Column(name = "documentoPropiedad", columnDefinition = "LONGBLOB")
    private byte[] documentoPropiedad;

    @Lob
    @Column(name = "comprobanteDomSalon",columnDefinition = "LONGBLOB")
    private byte[] comprobanteDomSalon;

    @Lob
    @Column(name = "vistoBuenoProteccionCivil",columnDefinition = "LONGBLOB")
    private byte[] vistoBuenoProteccionCivil;
    @Lob
    @Column(name = "fotoEstablecimiento", length = 1000,columnDefinition = "LONGBLOB")
    private byte[] fotoEstablecimiento;
    @Lob
    @Column(name = "fotoPropietario", length = 1000,columnDefinition = "LONGBLOB")
    private byte[] fotoPropietario;
    @Column(name = "estadoRevisionAdmin")
    private String estadoRevisionAdmin;
    @Column(name = "observacionesAdmin")
    private String observacionesAdmin;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Salonid_salon", referencedColumnName = "id_salon", nullable = false)
    @JsonBackReference
    private SalonEntity salon;
}