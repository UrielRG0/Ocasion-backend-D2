package com.ocasion.backend.salones.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
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

    @JsonIgnore
    @Lob
    @Column(name = "INEpropietarioPasaporte", columnDefinition = "LONGBLOB")
    private byte[] inePropietarioPasaporte;

    @Lob
    @Column(name = "documentoPropiedad", columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] documentoPropiedad;

    @Lob
    @Column(name = "comprobanteDomSalon",columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] comprobanteDomSalon;

    @Lob
    @Column(name = "vistoBuenoProteccionCivil",columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] vistoBuenoProteccionCivil;
    @Lob
    @Column(name = "fotoEstablecimiento", length = 1000,columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] fotoEstablecimiento;
    @Lob
    @Column(name = "fotoPropietario", length = 1000,columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] fotoPropietario;

    // La primera foto conserva su columna existente; las siguientes se guardan por separado.
    @JsonIgnore
    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    private List<FotoExpedienteEntity> fotosAdicionales = new ArrayList<>();
    @Column(name = "estadoRevisionAdmin")
    private String estadoRevisionAdmin;
    @Column(name = "observacionesAdmin")
    private String observacionesAdmin;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Salonid_salon", referencedColumnName = "id_salon", nullable = false)
    @JsonBackReference
    private SalonEntity salon;
}
