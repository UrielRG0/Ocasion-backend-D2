package com.ocasion.backend.salones.Services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ocasion.backend.salones.Repositories.SalonRepository;
import com.ocasion.backend.salones.DTO.DocumentosUrlsDTO;
import com.ocasion.backend.salones.DTO.ExpedienteAdminDTO;
import com.ocasion.backend.salones.DTO.SalonPendienteAdminDTO;
import com.ocasion.backend.salones.DTO.VeredictoAdminDTO;
import com.ocasion.backend.salones.Entities.DocumentosValidacionSalonEntity;
import com.ocasion.backend.salones.Entities.SalonEntity;


import jakarta.transaction.Transactional;

@Service 
public class AdminValidacionService {
    
    private final SalonRepository salonRepository;
    
    public AdminValidacionService(SalonRepository salonRepository){
        this.salonRepository = salonRepository;
    }
    public Page<SalonPendienteAdminDTO> obtenerSalonesPendientes(Pageable pageable) {
        Page<SalonEntity> paginaSalones = salonRepository.findByEstadoAndDocumentosValidacionSalonEntity_EstadoRevisionAdmin("PUBLICADO", "PENDIENTE", pageable);

        return paginaSalones.map(salon -> {
            SalonPendienteAdminDTO dto = new SalonPendienteAdminDTO();
            dto.setId_salon(salon.getId_salon());
            dto.setNombreSalon(salon.getNombreSalon());
                
            dto.setFechaSolicitud(salon.getFechaSolicitud()); 
            // (Simulada por ahora si aún no conectas los microservicios):
            dto.setPropietarioNombre("Propietario ID: " + salon.getUsuariosIdUser());
            // (Ideal con FeignClient llamando al microservicio de Usuarios):
            // String nombreUsuario = usuarioFeignClient.obtenerNombrePorId(salon.getUsuariosIdUser());
            // dto.setPropietarioNombre(nombreUsuario)
            return dto;
        });
    }
    @Transactional
    public ExpedienteAdminDTO obtenerExpediente(Integer idSalon){
        SalonEntity salon = salonRepository.findById(idSalon)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el salón!"));
            
        if(!"PUBLICADO".equals(salon.getEstado()) || salon.getDocumentosValidacionSalonEntity() == null || !"PENDIENTE".equals(salon.getDocumentosValidacionSalonEntity().getEstadoRevisionAdmin())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se requiere validación");
        }
        
        ExpedienteAdminDTO expediente = new ExpedienteAdminDTO();
        expediente.setId_Salon(salon.getId_salon());
        expediente.setNombreSalon(salon.getNombreSalon());
        expediente.setDescripcion(salon.getDescripcion());
        expediente.setPrecioPorHora(salon.getPrecio_hora());
        expediente.setCapacidadPersonas(salon.getCapacidadPersonas());
        expediente.setEstado(salon.getEstado());
        expediente.setEstadoRevisionAdmin(salon.getDocumentosValidacionSalonEntity().getEstadoRevisionAdmin());
        if (salon.getServiciosAsignados() != null) {

            List<String> servicios = salon.getServiciosAsignados()
                    .stream()
                    .map(relacion -> relacion.getServicio().getNombreServicio())
                    .toList();

            expediente.setServicios(servicios);
        }
        if (salon.getUbicacion() != null) expediente.setDireccion(salon.getUbicacion().getDireccion());
        
        DocumentosValidacionSalonEntity docsEntity = salon.getDocumentosValidacionSalonEntity();
        
        if(docsEntity != null){
            DocumentosUrlsDTO documentosDTO = new DocumentosUrlsDTO();

            String baseUrl = "/api/v1/admin/salones/" + idSalon + "/archivo/";

            if (docsEntity.getInePropietarioPasaporte() != null && docsEntity.getInePropietarioPasaporte().length > 0) {
                documentosDTO.setUrlInePasaporte(baseUrl + "ine");
            }
            if (docsEntity.getDocumentoPropiedad() != null && docsEntity.getDocumentoPropiedad().length > 0) {
                documentosDTO.setUrlDocumentoPropiedad(baseUrl + "propiedad");
            }
            if (docsEntity.getComprobanteDomSalon() != null && docsEntity.getComprobanteDomSalon().length > 0) {
                documentosDTO.setUrlComprobanteDom(baseUrl + "comprobante");
            }
            if (docsEntity.getVistoBuenoProteccionCivil() != null && docsEntity.getVistoBuenoProteccionCivil().length > 0) {
                documentosDTO.setUrlVistoBueno(baseUrl + "vobo");
            }
            if (docsEntity.getFotoEstablecimiento() != null && docsEntity.getFotoEstablecimiento().length > 0) {
                documentosDTO.setUrlFotosEstablecimiento(baseUrl + "foto-establecimiento");
            }
            if (docsEntity.getFotoPropietario() != null && docsEntity.getFotoPropietario().length > 0) {
                documentosDTO.setUrlFotosPropietario(baseUrl + "foto-propietario");
            }
            
            documentosDTO.setUrlsFotosEstablecimiento(urlsFotos(docsEntity, baseUrl, "foto-establecimiento",
                documentosDTO.getUrlFotosEstablecimiento()));
            documentosDTO.setUrlsFotosPropietario(urlsFotos(docsEntity, baseUrl, "foto-propietario",
                documentosDTO.getUrlFotosPropietario()));
            expediente.setDocumentos(documentosDTO);
        }
        
        return expediente;
    }
    @Transactional
    public void procesarVeredicto(Integer idSalon, VeredictoAdminDTO dto) {
        SalonEntity salon = salonRepository.findByIdForUpdate(idSalon)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el salón!"));

        if (!"PUBLICADO".equals(salon.getEstado()) || salon.getDocumentosValidacionSalonEntity() == null || !"PENDIENTE".equals(salon.getDocumentosValidacionSalonEntity().getEstadoRevisionAdmin())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El expediente no requiere revisión");
        }
        if (dto.getVeredicto() == null || (dto.getObservaciones() != null && dto.getObservaciones().length() > 255)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Veredicto obligatorio y observaciones de máximo 255 caracteres");
        }

        DocumentosValidacionSalonEntity documentos = salon.getDocumentosValidacionSalonEntity();
        if (documentos == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El salón no tiene expediente de documentos");
        }

        if (dto.getVeredicto() == VeredictoAdminDTO.Veredicto.APROBADO) {
            DocumentacionSalonService.comprobarExpediente(documentos);
            documentos.setEstadoRevisionAdmin("APROBADO");
            documentos.setObservacionesAdmin(null); 
 
            
        } else if (dto.getVeredicto() == VeredictoAdminDTO.Veredicto.RECHAZADO) {

            if (dto.getObservaciones() == null || dto.getObservaciones().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe proporcionar observaciones en caso de rechazo.");
            }
            documentos.setEstadoRevisionAdmin("RECHAZADO");
            documentos.setObservacionesAdmin(dto.getObservaciones().trim()); // Persistir texto

        }

        salonRepository.save(salon);
    }

    private java.util.List<String> urlsFotos(DocumentosValidacionSalonEntity docs, String baseUrl,
            String tipo, String primera) {
        java.util.List<String> urls = new java.util.ArrayList<>();
        if (primera != null) urls.add(primera);
        docs.getFotosAdicionales().stream().filter(f -> tipo.equals(f.getTipo()))
            .sorted(java.util.Comparator.comparingInt(com.ocasion.backend.salones.Entities.FotoExpedienteEntity::getPosicion))
            .forEach(f -> urls.add(baseUrl + tipo + "/" + f.getPosicion()));
        return urls;
    }
}
