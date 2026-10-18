package com.ocasion.backend.salones;

import com.ocasion.backend.salones.DTO.FiltroBusquedaDTO;
import com.ocasion.backend.salones.Entities.*;
import com.ocasion.backend.salones.Repositories.SalonSpecification;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ClienteSalonSpecificationTest {
    private static SessionFactory factory;
    private Session session;
    private SimpleJpaRepository<SalonEntity, Integer> repository;
    private ServiciosEntity banquete;
    private ServiciosEntity sonido;

    @BeforeAll static void crearBaseTemporal() {
        var config = new Configuration()
            .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
            .setProperty("hibernate.connection.url", "jdbc:h2:mem:cliente_specs;MODE=MySQL;DB_CLOSE_DELAY=-1")
            .setProperty("hibernate.hbm2ddl.auto", "create-drop")
            .setProperty("hibernate.show_sql", "false");
        for (Class<?> entity : List.of(SalonEntity.class, UbicacionEntity.class, ServiciosEntity.class,
            SalonServiciosEntity.class, DisponibilidadEntity.class, DocumentosValidacionSalonEntity.class,
            InventarioSalonEntity.class, CategoriaRecursoEntity.class)) config.addAnnotatedClass(entity);
        factory = config.buildSessionFactory();
    }

    @BeforeEach void preparar() {
        session = factory.openSession();
        session.beginTransaction();
        repository = new SimpleJpaRepository<>(SalonEntity.class, session);
        banquete = servicio("Banquete");
        sonido = servicio("Sonido");
        salon("Coincide", 250, "2400.00", "PUBLICADO", banquete, sonido);
        salon("Falta sonido", 250, "2400.00", "PUBLICADO", banquete);
        salon("Muy caro", 250, "2600.00", "PUBLICADO", banquete, sonido);
        salon("Pequeño", 100, "1000.00", "PUBLICADO", banquete, sonido);
        salon("Borrador", 250, "1000.00", "BORRADOR", banquete, sonido);
        session.flush();
        session.clear();
    }

    @AfterEach void limpiar() { session.getTransaction().rollback(); session.close(); }
    @AfterAll static void cerrar() { if (factory != null) factory.close(); }

    @Test void combinaCapacidadPrecioCiudadYTodosLosServicios() {
        var filtros = new FiltroBusquedaDTO();
        filtros.setCapacidadMin(200);
        filtros.setCapacidadMax(1500);
        filtros.setPrecioHoraMax(2500.0);
        filtros.setCiudad("aguascalientes");
        filtros.setServiciosIds(List.of(banquete.getIdServicio(), sonido.getIdServicio()));
        var result = repository.findAll(SalonSpecification.aplicarFiltros(filtros), PageRequest.of(0, 1));
        assertEquals(1, result.getTotalElements());
        assertEquals("Coincide", result.getContent().getFirst().getNombreSalon());
    }

    @Test void buscaPorCodigoPostalYTextoSinExponerBorradores() {
        var filtros = new FiltroBusquedaDTO();
        filtros.setCiudad("20131");
        filtros.setTexto("coincide");
        var result = repository.findAll(SalonSpecification.aplicarFiltros(filtros));
        assertEquals(List.of("Coincide"), result.stream().map(SalonEntity::getNombreSalon).toList());
    }

    @Test void paginacionNoDuplicaSalonesPorSusServicios() {
        var filtros = new FiltroBusquedaDTO();
        filtros.setServiciosIds(List.of(banquete.getIdServicio(), sonido.getIdServicio()));
        var first = repository.findAll(SalonSpecification.aplicarFiltros(filtros), PageRequest.of(0, 2));
        var second = repository.findAll(SalonSpecification.aplicarFiltros(filtros), PageRequest.of(1, 2));
        assertEquals(3, first.getTotalElements());
        assertEquals(1, second.getNumberOfElements());
        assertFalse(first.getContent().stream().anyMatch(s -> s.getId_salon().equals(second.getContent().getFirst().getId_salon())));
    }

    @Test void filtrosRestrictivosDevuelvenListaVacia() {
        var filtros = new FiltroBusquedaDTO();
        filtros.setCapacidadMin(1000);
        filtros.setPrecioHoraMax(10.0);
        assertTrue(repository.findAll(SalonSpecification.aplicarFiltros(filtros)).isEmpty());
    }

    private ServiciosEntity servicio(String nombre) {
        var entity = new ServiciosEntity();
        entity.setNombreServicio(nombre);
        session.persist(entity);
        return entity;
    }

    private void salon(String nombre, int capacidad, String precio, String estado, ServiciosEntity... servicios) {
        var salon = new SalonEntity();
        salon.setNombreSalon(nombre);
        salon.setCapacidadPersonas(capacidad);
        salon.setPrecio_hora(new BigDecimal(precio));
        salon.setEstado(estado);
        salon.setUsuariosIdUser(1);
        var ubicacion = new UbicacionEntity();
        ubicacion.setCiudad("Aguascalientes");
        ubicacion.setCP("20131");
        ubicacion.setDireccion("Av. Universidad");
        ubicacion.setLatitud(new BigDecimal("21.91"));
        ubicacion.setLongitud(new BigDecimal("-102.31"));
        ubicacion.setSalon(salon);
        salon.setUbicacion(ubicacion);
        session.persist(salon);
        for (var servicio : servicios) {
            var relacion = new SalonServiciosEntity();
            relacion.setId(new SalonServiciosId(salon.getId_salon(), servicio.getIdServicio()));
            relacion.setSalon(salon);
            relacion.setServicio(servicio);
            session.persist(relacion);
        }
    }
}
