package com.ocasion.backend.salones;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:contexto;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
    "jwt.secret=dGVzdC1zZWNyZXQtb25seS1mb3ItYXV0b21hdGVkLXRlc3RzLTEyMzQ1Njc4OTA="
})
class SalonesApplicationTests {

	@Test
	void contextLoads() {
	}


    @org.springframework.beans.factory.annotation.Autowired
    com.ocasion.backend.salones.Repositories.SalonRepository salones;
    @org.springframework.beans.factory.annotation.Autowired
    com.ocasion.backend.salones.Services.AdminValidacionService admin;
    @org.springframework.beans.factory.annotation.Autowired
    jakarta.persistence.EntityManager em;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void bandejaUsaEstadoDocumentalYVeredictoConservaPublicacionEnBaseDeDatos() {
        var pendiente = crearSalon("PENDIENTE");
        crearSalon("SIN_ENVIAR");
        crearSalon("RECHAZADO");
        em.flush(); em.clear();
        var pagina = admin.obtenerSalonesPendientes(org.springframework.data.domain.PageRequest.of(0,10));
        org.junit.jupiter.api.Assertions.assertEquals(1, pagina.getTotalElements());
        org.junit.jupiter.api.Assertions.assertEquals(pendiente.getId_salon(), pagina.getContent().getFirst().getId_salon());
        var dto = new com.ocasion.backend.salones.DTO.VeredictoAdminDTO();
        dto.setVeredicto(com.ocasion.backend.salones.DTO.VeredictoAdminDTO.Veredicto.APROBADO);
        admin.procesarVeredicto(pendiente.getId_salon(), dto);
        em.flush(); em.clear();
        var aprobado = salones.findById(pendiente.getId_salon()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("PUBLICADO", aprobado.getEstado());
        org.junit.jupiter.api.Assertions.assertTrue(aprobado.isVerificado());
        org.junit.jupiter.api.Assertions.assertEquals(0, admin.obtenerSalonesPendientes(org.springframework.data.domain.PageRequest.of(0,10)).getTotalElements());
        var otro = crearSalon("PENDIENTE");
        dto.setVeredicto(com.ocasion.backend.salones.DTO.VeredictoAdminDTO.Veredicto.RECHAZADO);
        dto.setObservaciones("Comprobante ilegible");
        admin.procesarVeredicto(otro.getId_salon(), dto);
        em.flush(); em.clear();
        var rechazado = salones.findById(otro.getId_salon()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("PUBLICADO", rechazado.getEstado());
        org.junit.jupiter.api.Assertions.assertFalse(rechazado.isVerificado());
    }

    private com.ocasion.backend.salones.Entities.SalonEntity crearSalon(String revision) {
        var salon = new com.ocasion.backend.salones.Entities.SalonEntity();
        salon.setUsuariosIdUser(9); salon.setNombreSalon("Prueba " + revision);
        salon.setEstado("PUBLICADO"); salon.setPrecio_hora(java.math.BigDecimal.TEN);
        var docs = new com.ocasion.backend.salones.Entities.DocumentosValidacionSalonEntity();
        byte[] contenido = {1,2,3};
        docs.setSalon(salon); docs.setEstadoRevisionAdmin(revision);
        docs.setInePropietarioPasaporte(contenido); docs.setDocumentoPropiedad(contenido);
        docs.setComprobanteDomSalon(contenido); docs.setVistoBuenoProteccionCivil(contenido);
        docs.setFotoEstablecimiento(contenido); docs.setFotoPropietario(contenido);
        for (int i=1;i<=2;i++) {
            var foto = new com.ocasion.backend.salones.Entities.FotoExpedienteEntity();
            foto.setExpediente(docs); foto.setTipo("foto-propietario"); foto.setPosicion(i); foto.setContenido(contenido);
            docs.getFotosAdicionales().add(foto);
        }
        salon.setDocumentosValidacionSalonEntity(docs);
        return salones.saveAndFlush(salon);
    }

    @org.springframework.beans.factory.annotation.Autowired
    com.ocasion.backend.salones.Services.DocumentacionSalonService documentosService;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void primeraCargaDevuelveIdPersistidoYPermiteReemplazarSinDuplicarExpediente() throws Exception {
        var salon = new com.ocasion.backend.salones.Entities.SalonEntity();
        salon.setUsuariosIdUser(9); salon.setNombreSalon("Primera carga documental");
        salon.setEstado("PUBLICADO"); salon.setPrecio_hora(java.math.BigDecimal.TEN);
        Integer id = salones.saveAndFlush(salon).getId_salon();
        em.clear();
        var foto = new org.springframework.mock.web.MockMultipartFile("foto", "foto.png", "image/png",
            new byte[]{(byte)137,80,78,71,13,10,26,10,1});
        var pdf = new org.springframework.mock.web.MockMultipartFile("pdf", "legal.pdf", "application/pdf",
            "%PDF-1.7 test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var respuesta = documentosService.guardar(id,9,foto,pdf,pdf,pdf,
            java.util.List.of(foto),java.util.List.of(foto,foto,foto));
        org.junit.jupiter.api.Assertions.assertNotNull(respuesta.get("id_expediente"));
        org.junit.jupiter.api.Assertions.assertEquals("SIN_ENVIAR",respuesta.get("estadoRevisionAdmin"));
        em.flush(); em.clear();
        var expediente = salones.findById(id).orElseThrow().getDocumentosValidacionSalonEntity();
        org.junit.jupiter.api.Assertions.assertEquals(expediente.getIdExpediente(),respuesta.get("id_expediente"));
        org.junit.jupiter.api.Assertions.assertEquals(2,expediente.getFotosAdicionales().size());
        org.junit.jupiter.api.Assertions.assertArrayEquals(pdf.getBytes(),documentosService.archivo(id,"propiedad",0));
        var reemplazo = documentosService.guardar(id,9,foto,pdf,pdf,pdf,
            java.util.List.of(foto,foto),java.util.List.of(foto,foto,foto));
        org.junit.jupiter.api.Assertions.assertEquals(respuesta.get("id_expediente"),reemplazo.get("id_expediente"));
        em.flush(); em.clear();
        org.junit.jupiter.api.Assertions.assertEquals(3,salones.findById(id).orElseThrow()
            .getDocumentosValidacionSalonEntity().getFotosAdicionales().size());
    }
}
