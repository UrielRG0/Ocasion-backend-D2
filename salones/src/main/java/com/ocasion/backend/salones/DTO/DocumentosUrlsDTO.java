package com.ocasion.backend.salones.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class DocumentosUrlsDTO {
    private String urlInePasaporte;
    private String urlDocumentoPropiedad;
    private String urlComprobanteDom;
    private String urlVistoBueno;
    private String urlFotosEstablecimiento;
    private String urlFotosPropietario;
    private java.util.List<String> urlsFotosEstablecimiento;
    private java.util.List<String> urlsFotosPropietario;
}
