package com.ocasion.backend.salones;

import com.ocasion.backend.salones.Controllers.ClienteSalonController;
import com.ocasion.backend.salones.DTO.FiltroBusquedaDTO;
import com.ocasion.backend.salones.Entities.SalonEntity;
import com.ocasion.backend.salones.Repositories.SalonRepository;
import com.ocasion.backend.salones.Repositories.DisponibilidadRepository;
import com.ocasion.backend.salones.Services.ClienteSalonService;
import com.ocasion.backend.salones.Services.ClienteSalonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClienteSalonContractTest {
    private final SalonRepository salones = mock(SalonRepository.class);
    private final DisponibilidadRepository disponibilidad = mock(DisponibilidadRepository.class);
    private final ClienteSalonServiceImpl service = new ClienteSalonServiceImpl();
    private SalonEntity salon;

    @BeforeEach void preparar() {
        ReflectionTestUtils.setField(service, "salonRepository", salones);
        ReflectionTestUtils.setField(service, "disponibilidadRepository", disponibilidad);
        salon = new SalonEntity();
        salon.setId_salon(42);
        salon.setEstado("PUBLICADO");
        when(salones.findById(42)).thenReturn(Optional.of(salon));
    }

    @Test void detalleDivideGaleriaQuitaVaciosYLimitaACinco() {
        salon.setImagen(" a.jpg,b.jpg,a.jpg,,c.jpg,d.jpg,e.jpg,f.jpg ");
        assertEquals(List.of("a.jpg", "b.jpg", "c.jpg", "d.jpg", "e.jpg"),
            service.obtenerDetalle(42).getGaleriaFotos());
    }

    @Test void noExponeBorradoresComoDetallePublico() {
        salon.setEstado("BORRADOR");
        assertEquals(404, assertThrows(ResponseStatusException.class,
            () -> service.obtenerDetalle(42)).getStatusCode().value());
    }

    @Test void calendarioConsultaLosLimitesDelMesBisiestoSinCrearReservas() {
        when(disponibilidad.findDisponibilidadPorMes(42, LocalDate.of(2028,2,1),
            LocalDate.of(2028,2,29))).thenReturn(List.of());
        assertTrue(service.obtenerDisponibilidadMensual(42, 2, 2028).isEmpty());
        verify(disponibilidad).findDisponibilidadPorMes(42, LocalDate.of(2028,2,1), LocalDate.of(2028,2,29));
        verifyNoMoreInteractions(disponibilidad);
    }

    @Test void mesInvalidoProduce400AntesDeConsultar() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> service.obtenerDisponibilidadMensual(42, 13, 2026)).getStatusCode().value());
        verifyNoInteractions(disponibilidad);
    }

    @Test void filtrosInvalidosNoLleganAlRepositorio() {
        var controller = new ClienteSalonController();
        var consulta = mock(ClienteSalonService.class);
        ReflectionTestUtils.setField(controller, "clienteSalonService", consulta);
        var filtros = new FiltroBusquedaDTO();
        filtros.setCapacidadMin(500);
        filtros.setCapacidadMax(200);
        assertEquals(400, controller.explorarSalones(filtros, 0, 20).getStatusCode().value());
        filtros.setCapacidadMin(50);
        filtros.setPrecioHoraMax(-1.0);
        assertEquals(400, controller.explorarSalones(filtros, 0, 20).getStatusCode().value());
        filtros.setPrecioHoraMax(null);
        assertEquals(400, controller.explorarSalones(filtros, -1, 20).getStatusCode().value());
        verifyNoInteractions(consulta);
    }
    @Test void verificacionEsIndependienteDeVisibilidadPublica() {
        assertFalse(service.obtenerDetalle(42).isVerificado());
        var docs = new com.ocasion.backend.salones.Entities.DocumentosValidacionSalonEntity();
        salon.setDocumentosValidacionSalonEntity(docs);
        for (String estado : List.of("SIN_ENVIAR", "PENDIENTE", "RECHAZADO")) {
            docs.setEstadoRevisionAdmin(estado);
            assertFalse(service.obtenerDetalle(42).isVerificado());
            assertEquals("PUBLICADO", salon.getEstado());
        }
        docs.setEstadoRevisionAdmin("APROBADO");
        assertTrue(service.obtenerDetalle(42).isVerificado());
    }
}
