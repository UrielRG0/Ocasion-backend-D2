package com.ocasion.backend.salones;

import com.ocasion.backend.salones.DTO.VeredictoAdminDTO;
import com.ocasion.backend.salones.Entities.*;
import com.ocasion.backend.salones.Repositories.*;
import com.ocasion.backend.salones.Services.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentacionSalonTest {
    final SalonRepository repo = mock(SalonRepository.class);
    final DocumentacionSalonService documentos = new DocumentacionSalonService(repo);
    final SalonServiciosRepository servicios = mock(SalonServiciosRepository.class);
    final InventarioSalonRepository inventario = mock(InventarioSalonRepository.class);
    final SalonService salones = new SalonService(repo, servicios, mock(ServiciosRepository.class),
        mock(CategoriaRecursoRepository.class), inventario);
    final AdminValidacionService admin = new AdminValidacionService(repo);
    SalonEntity salon;
    final MockMultipartFile foto = new MockMultipartFile("foto", "foto.jpg", "image/jpeg", new byte[]{-1,-40,-1,1});
    final MockMultipartFile pdf = new MockMultipartFile("pdf", "legal.pdf", "application/pdf", "%PDF-1.7\nlegal".getBytes());

    @BeforeEach void preparar() {
        salon = new SalonEntity();
        salon.setId_salon(7); salon.setUsuariosIdUser(9); salon.setEstado("BORRADOR");
        salon.setUbicacion(new UbicacionEntity());
        when(repo.findByIdForUpdate(7)).thenReturn(Optional.of(salon));
        when(repo.findById(7)).thenReturn(Optional.of(salon));
        when(repo.saveAndFlush(salon)).thenAnswer(i -> { salon.getDocumentosValidacionSalonEntity().setIdExpediente(1); return salon; });
        when(servicios.countBySalonId(7)).thenReturn(1L);
        when(inventario.countCategoriasBySalonId(7)).thenReturn(1L);
    }

    void cargar() { documentos.guardar(7,9,foto,pdf,pdf,pdf,List.of(foto),List.of(foto,foto,foto)); }
    void error(int status, org.junit.jupiter.api.function.Executable accion) {
        assertEquals(status, assertThrows(ResponseStatusException.class, accion).getStatusCode().value());
    }

    @Test void cargaRecuperaCadaFotoYGeneraUrlsProtegidas() throws Exception {
        cargar();
        assertEquals("BORRADOR", salon.getEstado());
        assertArrayEquals(pdf.getBytes(), documentos.archivo(7,"vobo",0));
        assertArrayEquals(foto.getBytes(), documentos.archivo(7,"foto-propietario",2));
        salon.setEstado("PENDIENTE_VALIDACION");
        var urls = admin.obtenerExpediente(7).getDocumentos();
        assertEquals(3, urls.getUrlsFotosPropietario().size());
        assertEquals("/api/v1/admin/salones/7/archivo/ine", urls.getUrlInePasaporte());
        error(404, () -> documentos.archivo(7,"foto-propietario",3));
    }

    @Test void impideCargaAjenaYDuranteRevision() {
        error(403, () -> documentos.guardar(7,8,foto,pdf,pdf,pdf,List.of(foto),List.of(foto,foto,foto)));
        salon.setEstado("PENDIENTE_VALIDACION");
        error(409, this::cargar);
        verify(repo, never()).saveAndFlush(any());
    }

    @Test void validaCantidadDeFotosYFormatoRealSinModificarExpediente() {
        error(400, () -> documentos.guardar(7,9,foto,pdf,pdf,pdf,List.of(foto),List.of(foto,foto)));
        error(400, () -> documentos.guardar(7,9,foto,pdf,pdf,pdf,Collections.nCopies(11,foto),List.of(foto,foto,foto)));
        var falso = new MockMultipartFile("ine", "ine.jpg", "image/jpeg", "contenido falso".getBytes());
        error(415, () -> documentos.guardar(7,9,falso,pdf,pdf,pdf,List.of(foto),List.of(foto,foto,foto)));
        var grande = mock(org.springframework.web.multipart.MultipartFile.class);
        when(grande.getSize()).thenReturn(DocumentacionSalonService.MAX_ARCHIVO + 1);
        error(413, () -> documentos.guardar(7,9,grande,pdf,pdf,pdf,List.of(foto),List.of(foto,foto,foto)));
        assertNull(salon.getDocumentosValidacionSalonEntity());
    }

    @Test void noEnviaRevisionSinDocumentacionOServicios() {
        error(422, () -> salones.solicitarValidacion(7,9));
        cargar();
        when(servicios.countBySalonId(7)).thenReturn(0L);
        error(422, () -> salones.solicitarValidacion(7,9));
        assertEquals("BORRADOR",salon.getEstado());
        error(403, () -> salones.solicitarValidacion(7,8));
    }

    @Test void rechazoCorreccionReenvioYAprobacion() {
        cargar(); salones.solicitarValidacion(7,9);
        var veredicto = new VeredictoAdminDTO();
        veredicto.setVeredicto(VeredictoAdminDTO.Veredicto.RECHAZADO);
        error(400, () -> admin.procesarVeredicto(7,veredicto));
        veredicto.setObservaciones("Comprobante ilegible");
        admin.procesarVeredicto(7,veredicto);
        assertEquals("RECHAZADO",salon.getEstado());
        assertEquals("Comprobante ilegible",salon.getDocumentosValidacionSalonEntity().getObservacionesAdmin());
        cargar(); salones.solicitarValidacion(7,9);
        veredicto.setVeredicto(VeredictoAdminDTO.Veredicto.APROBADO);
        admin.procesarVeredicto(7,veredicto);
        assertEquals("PUBLICADO",salon.getEstado());
        assertNull(salon.getDocumentosValidacionSalonEntity().getObservacionesAdmin());
        error(400, () -> admin.procesarVeredicto(7,veredicto));
        error(409, this::cargar);
    }

    @Test void noApruebaExpedienteIncompleto() {
        cargar(); salon.setEstado("PENDIENTE_VALIDACION");
        salon.getDocumentosValidacionSalonEntity().setComprobanteDomSalon(null);
        var dto = new VeredictoAdminDTO(); dto.setVeredicto(VeredictoAdminDTO.Veredicto.APROBADO);
        error(422, () -> admin.procesarVeredicto(7,dto));
        assertEquals("PENDIENTE_VALIDACION", salon.getEstado());
    }
}
