package com.ocasion.backend.salones.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ocasion.backend.salones.DTO.*;
import com.ocasion.backend.salones.Entities.*;
import com.ocasion.backend.salones.Repositories.DisponibilidadRepository;
import com.ocasion.backend.salones.Repositories.SalonRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ClienteSalonServiceImpl implements ClienteSalonService {

    @Autowired
    private SalonRepository salonRepository;

    @Autowired
    private com.ocasion.backend.salones.Repositories.CatalogoConsulta catalogoConsulta;

    @Autowired
    private DisponibilidadRepository disponibilidadRepository;

    @Override
    public Page<SalonCardResponseDTO> buscarSalones(FiltroBusquedaDTO filtros, Pageable pageable) {
        return catalogoConsulta.buscar(filtros, pageable, usuarioActual());
    }

    @Override
    public SalonDetalleClienteDTO obtenerDetalle(Integer id) {
        SalonEntity salon = salonPublicado(id);

        SalonDetalleClienteDTO dto = new SalonDetalleClienteDTO();
        dto.setId_salon(salon.getId_salon());
        dto.setVerificado(salon.isVerificado());
        dto.setNombreSalon(salon.getNombreSalon());
        dto.setDescripcion(salon.getDescripcion());
        disponibilidadRepository.findInicial(id, org.springframework.data.domain.PageRequest.of(0, 1))
            .stream().findFirst().ifPresent(d -> {
                if (d.getFecha() != null && d.getHora_inicio() != null && d.getHora_fin() != null
                    && d.getHora_fin().toLocalTime().isAfter(d.getHora_inicio().toLocalTime())) {
                    dto.setHorarioAtencion(new HorarioSalonDTO(d.getFecha(),
                        d.getHora_inicio().toLocalTime(), d.getHora_fin().toLocalTime()));
                }
            });
        dto.setGaleriaFotos(fotos(salon));
        dto.setCapacidadPersonas(salon.getCapacidadPersonas());
        
        // CORRECCIÓN: Usando getPrecio_hora()
        if (salon.getPrecio_hora() != null) {
            dto.setPrecio_hora(salon.getPrecio_hora().doubleValue());
        }
        
        if (salon.getUbicacion() != null) {
            if (salon.getUbicacion().getLatitud() != null) {
                dto.setLatitud(salon.getUbicacion().getLatitud().doubleValue());
            }
            if (salon.getUbicacion().getLongitud() != null) {
                dto.setLongitud(salon.getUbicacion().getLongitud().doubleValue());
            }
            dto.setDireccion(salon.getUbicacion().getDireccion());
            dto.setCiudad(salon.getUbicacion().getCiudad());
        }

        // CORRECCIÓN: Usando getServiciosAsignados()
        if (salon.getServiciosAsignados() != null) {
            List<String> todosLosServicios = salon.getServiciosAsignados().stream()
                    .map(relacion -> relacion.getServicio().getNombreServicio())
                    .collect(Collectors.toList());
            dto.setServiciosCompletos(todosLosServicios);
        }

        return dto;
    }

    @Override
    public List<DisponibilidadMesDTO> obtenerDisponibilidadMensual(Integer id, int mes, int anio) {
        if (mes < 1 || mes > 12 || anio < 1900 || anio > 9999) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Mes o año inválido");
        }
        salonPublicado(id);
        YearMonth anioMes = YearMonth.of(anio, mes);
        LocalDate inicioMes = anioMes.atDay(1);
        LocalDate finMes = anioMes.atEndOfMonth();

        List<DisponibilidadEntity> entidades = disponibilidadRepository.findDisponibilidadPorMes(id, inicioMes, finMes);

        return entidades.stream().map(d -> {
            DisponibilidadMesDTO dto = new DisponibilidadMesDTO();
            dto.setFecha(d.getFecha());
            
            if (d.getHora_inicio() != null) dto.setHora_inicio(d.getHora_inicio().toLocalTime());
            if (d.getHora_fin() != null) dto.setHora_fin(d.getHora_fin().toLocalTime());
            
            // Las notas del propietario no se exponen en el calendario público.
            dto.setTipo_bloqueo("BLOQUEO_PROPIETARIO");
            return dto;
        }).collect(Collectors.toList());
    }

    private SalonEntity salonPublicado(Integer id) {
        SalonEntity salon = salonRepository.findById(id).filter(s -> "PUBLICADO".equals(s.getEstado()))
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Salón no disponible"));
        Integer usuario = usuarioActual();
        if (usuario != null && usuario.equals(salon.getUsuariosIdUser()))
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "No puede reservar su propio salón. Utilice el panel de administración.");
        return salon;
    }

    private Integer usuarioActual() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
            return null;
        try { return Integer.valueOf(auth.getName()); }
        catch (NumberFormatException ex) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Identidad de usuario inválida");
        }
    }

    private List<String> fotos(SalonEntity salon) {
        return ImagenesSalon.urls(salon);
    }
}
