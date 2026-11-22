package com.ocasion.backend.salones.Services;

import com.ocasion.backend.salones.DTO.*;
import com.ocasion.backend.salones.Entities.*;
import com.ocasion.backend.salones.Repositories.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisponibilidadServiceTest {
    SalonRepository salones = mock(SalonRepository.class);
    DisponibilidadRepository repo = mock(DisponibilidadRepository.class);
    DisponibilidadService service = new DisponibilidadService(salones, repo);
    SalonEntity salon;
    DisponibilidadEntity inicial;
    @BeforeEach void preparar() {
        salon = new SalonEntity(); salon.setId_salon(7); salon.setUsuariosIdUser(9);
        inicial = fila(1, LocalDate.of(2027,1,1),10,22);
        salon.setDisponibilidades(new ArrayList<>(List.of(inicial)));
        when(salones.findById(7)).thenReturn(Optional.of(salon));
        when(salones.findByIdForUpdate(7)).thenReturn(Optional.of(salon));
        when(repo.findInicial(eq(7),any())).thenReturn(List.of(inicial));
        when(repo.save(any())).thenAnswer(i->i.getArgument(0));
    }
    DisponibilidadEntity fila(int id, LocalDate fecha,int inicio,int fin) {
        var d = new DisponibilidadEntity(); d.setId_disponibilidad(id); d.setSalon(salon);
        d.setFecha(fecha); d.setHora_inicio(fecha.atTime(inicio,0)); d.setHora_fin(fecha.atTime(fin,0)); return d;
    }
    @Test void otroPropietarioNoLeeNiCambiaDisponibilidad() {
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.consultarBloqueos(7,10)).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->service.actualizarHorario(7,
            new ActualizarHorarioDTO(LocalDate.now(),LocalTime.of(9,0),LocalTime.of(20,0)),10)).getStatusCode().value());
        verifyNoInteractions(repo);
    }
    @Test void actualizaHorarioEnLaMismaFilaSinConvertirBloqueosEnHorario() {
        var bloqueo = fila(2,LocalDate.now().plusDays(5),12,14); salon.getDisponibilidades().add(bloqueo);
        service.actualizarHorario(7,new ActualizarHorarioDTO(LocalDate.now(),LocalTime.of(8,0),LocalTime.of(19,0)),9);
        verify(repo).saveAndFlush(inicial); assertEquals(LocalTime.of(8,0),inicial.getHora_inicio().toLocalTime());
        assertEquals(List.of(bloqueo),service.consultarBloqueos(7,9));
    }
    @Test void rechazaHorasInvertidasYFechasCruzadas() {
        assertThrows(ResponseStatusException.class,()->service.actualizarHorario(7,
            new ActualizarHorarioDTO(LocalDate.now(),LocalTime.of(20,0),LocalTime.of(10,0)),9));
        var dto = new DisponibilidadDTO(); dto.setFecha(LocalDate.now().plusDays(1));
        dto.setHora_inicio(dto.getFecha().atTime(22,0)); dto.setHora_fin(dto.getFecha().plusDays(1).atTime(2,0));
        assertThrows(ResponseStatusException.class,()->service.registrarBloqueo(7,dto,9));
        verify(repo,never()).save(any());verify(repo,never()).saveAndFlush(any());
    }
    @Test void registraBloqueoDelDiaCompletoSinModificarHorario() {
        var dto = new DisponibilidadDTO(); dto.setFecha(LocalDate.now().plusDays(1));
        dto.setHora_inicio(dto.getFecha().atStartOfDay());dto.setHora_fin(dto.getFecha().atTime(23,59,59));
        var guardado = service.registrarBloqueo(7,dto,9);
        assertEquals(LocalTime.of(23,59,59),guardado.getHora_fin().toLocalTime());
        assertEquals(LocalTime.of(10,0),inicial.getHora_inicio().toLocalTime());
    }
    @Test void primerBloqueoDeSalonAntiguoNoSeConfundeConHorarioInicial() {
        when(repo.findInicial(eq(7),any())).thenReturn(List.of());
        var dto = new DisponibilidadDTO();dto.setFecha(LocalDate.now().plusDays(1));
        dto.setHora_inicio(dto.getFecha().atTime(12,0));dto.setHora_fin(dto.getFecha().atTime(14,0));
        var bloqueo = service.registrarBloqueo(7,dto,9);
        var captura = org.mockito.ArgumentCaptor.forClass(DisponibilidadEntity.class);
        verify(repo).saveAndFlush(captura.capture());
        assertEquals(LocalDate.now(),captura.getValue().getFecha());
        assertEquals(LocalTime.MIDNIGHT,captura.getValue().getHora_inicio().toLocalTime());
        assertEquals(LocalTime.of(23,59),captura.getValue().getHora_fin().toLocalTime());
        assertNotSame(captura.getValue(),bloqueo);
    }
    @Test void eliminaSoloBloqueosDelSalonPropioYProtegeHorario() {
        when(repo.findById(1)).thenReturn(Optional.of(inicial));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.eliminarBloqueo(7,1,9)).getStatusCode().value());
        var bloqueo = fila(2,LocalDate.now().plusDays(1),12,14);when(repo.findById(2)).thenReturn(Optional.of(bloqueo));
        service.eliminarBloqueo(7,2,9);verify(repo).delete(bloqueo);
        var ajeno = new SalonEntity();ajeno.setId_salon(8);bloqueo.setSalon(ajeno);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.eliminarBloqueo(7,2,9)).getStatusCode().value());
    }
}
