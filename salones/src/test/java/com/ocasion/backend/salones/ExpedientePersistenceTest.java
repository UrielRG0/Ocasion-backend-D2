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
            InventarioSalonEntity.class, CategoriaRecursoEntity.class, FotoExpedienteEntity.class))
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
        service = new DocumentacionSalonService(repo);
    }
    @AfterEach void limpiar() { session.getTransaction().rollback(); session.close(); }
    @AfterAll static void cerrar() { if (factory != null) factory.close(); }

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
}
