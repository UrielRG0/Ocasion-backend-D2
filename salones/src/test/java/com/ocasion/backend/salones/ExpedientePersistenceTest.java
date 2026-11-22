package com.ocasion.backend.salones;

import com.ocasion.backend.salones.Entities.*;
import com.ocasion.backend.salones.Repositories.SalonRepository;
import com.ocasion.backend.salones.Services.DocumentacionSalonService;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpedientePersistenceTest {
    static SessionFactory factory;
    Session session;
    DocumentacionSalonService service;
    com.ocasion.backend.salones.Services.AdminValidacionService admin;
    Integer id;
    final MockMultipartFile foto = new MockMultipartFile("foto", "foto.png", "image/png",
        new byte[]{(byte)137,80,78,71,13,10,26,10,1});
    final MockMultipartFile pdf = new MockMultipartFile("pdf", "legal.pdf", "application/pdf", "%PDF-1.7\ntest".getBytes());

    @BeforeAll static void baseTemporal() {
        var config = new Configuration()
            .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
            .setProperty("hibernate.connection.url", "jdbc:h2:mem:expedientes;MODE=MySQL;DB_CLOSE_DELAY=-1")
            .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        for (Class<?> entity : List.of(SalonEntity.class, UbicacionEntity.class, ServiciosEntity.class,
            SalonServiciosEntity.class, DisponibilidadEntity.class, DocumentosValidacionSalonEntity.class,
            InventarioSalonEntity.class, CategoriaRecursoEntity.class, FotoExpedienteEntity.class, ImagenSalonEntity.class, ContenidoImagenSalonEntity.class))
            config.addAnnotatedClass(entity);
        factory = config.buildSessionFactory();
    }

    @BeforeEach void preparar() {
        session = factory.openSession(); session.beginTransaction();
        var salon = new SalonEntity(); salon.setUsuariosIdUser(9); salon.setNombreSalon("Expediente de prueba");
        salon.setEstado("PUBLICADO"); salon.setPrecio_hora(BigDecimal.TEN); session.persist(salon);
        id = salon.getId_salon();
        var repo = mock(SalonRepository.class);
        when(repo.findByIdForUpdate(id)).thenAnswer(i -> Optional.ofNullable(session.find(SalonEntity.class,id)));
        when(repo.findById(id)).thenAnswer(i -> Optional.ofNullable(session.find(SalonEntity.class,id)));
        when(repo.saveAndFlush(any())).thenAnswer(i -> {
            session.persist(i.getArgument(0)); session.flush(); return i.getArgument(0);
        });
        doAnswer(i -> { session.flush(); return null; }).when(repo).flush();
        service = new DocumentacionSalonService(repo);
        admin = new com.ocasion.backend.salones.Services.AdminValidacionService(repo);
    }
    @AfterEach void limpiar() { session.getTransaction().rollback(); session.close(); }
    @AfterAll static void cerrar() { if (factory != null) factory.close(); }

    @Test void calendarioNoConfundeHorarioInicialConBloqueos() throws Exception {
        var salon = session.find(SalonEntity.class, id);
        var apertura = new DisponibilidadEntity();
        apertura.setSalon(salon); apertura.setFecha(java.time.LocalDate.of(2027, 1, 10));
        apertura.setHora_inicio(apertura.getFecha().atTime(10, 0));
        apertura.setHora_fin(apertura.getFecha().atTime(22, 0));
        session.persist(apertura);
        var bloqueo = new DisponibilidadEntity();
        bloqueo.setSalon(salon); bloqueo.setFecha(apertura.getFecha());
        bloqueo.setHora_inicio(bloqueo.getFecha().atTime(15, 0));
        bloqueo.setHora_fin(bloqueo.getFecha().atTime(18, 0));
        session.persist(bloqueo); session.flush();
        var metodo = com.ocasion.backend.salones.Repositories.DisponibilidadRepository.class
            .getMethod("findDisponibilidadPorMes", Integer.class, java.time.LocalDate.class, java.time.LocalDate.class);
        var hql = metodo.getAnnotation(org.springframework.data.jpa.repository.Query.class).value();
        var resultado = session.createQuery(hql, DisponibilidadEntity.class)
            .setParameter("idSalon", id).setParameter("inicioMes", java.time.LocalDate.of(2027, 1, 1))
            .setParameter("finMes", java.time.LocalDate.of(2027, 1, 31)).getResultList();
        assertEquals(1, resultado.size());
        assertEquals(bloqueo.getId_disponibilidad(), resultado.getFirst().getId_disponibilidad());
    }

    @Test void persisteBinariosReemplazaFotosYEliminaHuerfanas() throws Exception {
        service.guardar(id,9,foto,pdf,pdf,pdf,List.of(foto,foto),List.of(foto,foto,foto,foto));
        session.clear();
        var original = session.find(SalonEntity.class,id).getDocumentosValidacionSalonEntity();
        Integer expedienteId = original.getIdExpediente();
        assertEquals(4, original.getFotosAdicionales().size());
        assertArrayEquals(pdf.getBytes(), service.archivo(id,"propiedad",0));
        assertArrayEquals(foto.getBytes(), service.archivo(id,"foto-propietario",3));
        service.guardar(id,9,foto,pdf,pdf,pdf,List.of(foto),List.of(foto,foto,foto));
        session.clear();
        var actual = session.find(SalonEntity.class,id).getDocumentosValidacionSalonEntity();
        assertEquals(expedienteId, actual.getIdExpediente());
        assertEquals(2, actual.getFotosAdicionales().size());
        assertEquals(2L, session.createQuery("select count(f) from FotoExpedienteEntity f", Long.class).getSingleResult());
        DocumentacionSalonService.comprobarExpediente(actual);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(com.ocasion.backend.salones.DTO.VeredictoAdminDTO.Veredicto.class)
    void resolverEliminaBinariosYFotosAdicionalesEnLaBasePeroConservaResultado(
            com.ocasion.backend.salones.DTO.VeredictoAdminDTO.Veredicto veredicto) {
        service.guardar(id,9,foto,pdf,pdf,pdf,List.of(foto,foto),List.of(foto,foto,foto,foto));
        var salon = session.find(SalonEntity.class,id);
        salon.setImagen("portada-comercial.jpg");
        var docs = salon.getDocumentosValidacionSalonEntity();
        Integer folio = docs.getIdExpediente();
        docs.setEstadoRevisionAdmin("PENDIENTE");
        session.flush(); session.clear();
        var dto = new com.ocasion.backend.salones.DTO.VeredictoAdminDTO();
        dto.setVeredicto(veredicto); dto.setObservaciones("El documento no es legible");
        admin.procesarVeredicto(id,dto);
        session.clear();
        salon = session.find(SalonEntity.class,id); docs = salon.getDocumentosValidacionSalonEntity();
        assertEquals(folio,docs.getIdExpediente());
        assertEquals(veredicto.name(),docs.getEstadoRevisionAdmin());
        assertNotNull(docs.getFechaResolucion()); assertNotNull(docs.getArchivosEliminadosEn());
        assertNull(docs.getInePropietarioPasaporte()); assertNull(docs.getDocumentoPropiedad());
        assertNull(docs.getComprobanteDomSalon()); assertNull(docs.getVistoBuenoProteccionCivil());
        assertNull(docs.getFotoEstablecimiento()); assertNull(docs.getFotoPropietario());
        assertTrue(docs.getFotosAdicionales().isEmpty());
        assertEquals(0L,session.createQuery("select count(f) from FotoExpedienteEntity f where f.expediente.idExpediente = :id",Long.class)
            .setParameter("id",folio).getSingleResult());
        assertEquals("PUBLICADO",salon.getEstado());
        assertEquals("portada-comercial.jpg",salon.getImagen());
        assertEquals(veredicto == com.ocasion.backend.salones.DTO.VeredictoAdminDTO.Veredicto.APROBADO,salon.isVerificado());
        assertEquals(404,assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> service.archivo(id,"ine",0)).getStatusCode().value());
        assertEquals(404,assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> service.archivo(id,"foto-propietario",3)).getStatusCode().value());
        if (veredicto == com.ocasion.backend.salones.DTO.VeredictoAdminDTO.Veredicto.RECHAZADO) {
            assertEquals("El documento no es legible",docs.getObservacionesAdmin());
            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> DocumentacionSalonService.comprobarExpediente(salonDocumentos()));
            service.guardar(id,9,foto,pdf,pdf,pdf,List.of(foto),List.of(foto,foto,foto));
            session.clear();
            var nuevos = salonDocumentos();
            assertEquals("SIN_ENVIAR",nuevos.getEstadoRevisionAdmin());
            assertNull(nuevos.getArchivosEliminadosEn()); assertNull(nuevos.getFechaResolucion());
            assertNotNull(nuevos.getInePropietarioPasaporte());
        }
    }
    private DocumentosValidacionSalonEntity salonDocumentos() {
        return session.find(SalonEntity.class,id).getDocumentosValidacionSalonEntity();
    }
}
