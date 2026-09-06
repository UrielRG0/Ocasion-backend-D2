package com.ocasion.backend.salones.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ocasion.backend.salones.DTO.InventarioBatchDTO;
import com.ocasion.backend.salones.DTO.SalonCreateFase1DTO;
import com.ocasion.backend.salones.DTO.ServicioAsignadoDTO;
import com.ocasion.backend.salones.Entities.DisponibilidadEntity;
import com.ocasion.backend.salones.Entities.SalonEntity;
import com.ocasion.backend.salones.Entities.UbicacionEntity;
import com.ocasion.backend.salones.Repositories.SalonRepository;

// Asegúrate de importar los DTOs, Entidades y Repositorios necesarios
import com.ocasion.backend.salones.Entities.CategoriaRecursoEntity;
import com.ocasion.backend.salones.Entities.InventarioSalonEntity;
import com.ocasion.backend.salones.Entities.SalonServiciosEntity;
import com.ocasion.backend.salones.Entities.SalonServiciosId;
import com.ocasion.backend.salones.Entities.ServiciosEntity;
import com.ocasion.backend.salones.Repositories.CategoriaRecursoRepository;
import com.ocasion.backend.salones.Repositories.InventarioSalonRepository;
import com.ocasion.backend.salones.Repositories.SalonServiciosRepository;
import com.ocasion.backend.salones.Repositories.ServiciosRepository;
import com.ocasion.backend.salones.DTO.CategoriaBatchDTO;
import com.ocasion.backend.salones.DTO.RecursoDTO;

import jakarta.transaction.Transactional;

@Service
public class SalonService {

    private final SalonRepository salonRepository;
    private final SalonServiciosRepository salonServiciosRepository;
    private final ServiciosRepository serviciosRepository;
    private final CategoriaRecursoRepository categoriaRecursoRepository;
    private final InventarioSalonRepository inventarioSalonRepository;

   public SalonService(SalonRepository salonRepository, 
                        SalonServiciosRepository salonServiciosRepository,
                        ServiciosRepository serviciosRepository,
                        CategoriaRecursoRepository categoriaRecursoRepository,
                        InventarioSalonRepository inventarioSalonRepository) {
        this.salonRepository = salonRepository;
        this.salonServiciosRepository = salonServiciosRepository;
        this.serviciosRepository = serviciosRepository;
        this.categoriaRecursoRepository = categoriaRecursoRepository;
        this.inventarioSalonRepository = inventarioSalonRepository;
    }

    @Transactional
    public Map<String, Object> crearFase1(SalonCreateFase1DTO dto, Integer idPropietario) {
        SalonEntity salon = new SalonEntity();
        salon.setNombreSalon(dto.getNombreSalon());
        salon.setCapacidadPersonas(dto.getCapacidadPersonas());
        salon.setPrecio_hora(dto.getPrecio_hora());
        salon.setDescripcion(dto.getDescripcion());
        salon.setUsuariosIdUser(idPropietario);
        salon.setEstado("BORRADOR");

        if (dto.getImagenes() != null && !dto.getImagenes().isEmpty()) {
            salon.setImagen(String.join(",", dto.getImagenes()));
        }

        // 1. Mapear Ubicación
        UbicacionEntity ubicacion = new UbicacionEntity();
        ubicacion.setDireccion(dto.getUbicacion().getDireccion());
        ubicacion.setLatitud(dto.getUbicacion().getLatitud());
        ubicacion.setLongitud(dto.getUbicacion().getLongitud());
        ubicacion.setCiudad(dto.getUbicacion().getCiudad());
        ubicacion.setCP(dto.getUbicacion().getCp());
        ubicacion.setSalon(salon);
        salon.setUbicacion(ubicacion);

        // 2. Mapear Disponibilidad Inicial
        DisponibilidadEntity disp = new DisponibilidadEntity();
        disp.setHora_inicio(dto.getDisponibilidadInicial().getHora_inicio());
        disp.setHora_fin(dto.getDisponibilidadInicial().getHora_fin());
        disp.setFecha(dto.getDisponibilidadInicial().getFecha());
        disp.setObservaciones(dto.getDisponibilidadInicial().getObservaciones());
        disp.setSalon(salon);
        salon.setDisponibilidades(List.of(disp)); // Asumiendo relación mapeada

        salonRepository.save(salon);

        // Armar respuesta JSON solicitada
        Map<String, Object> response = new HashMap<>();
        response.put("id_salon", salon.getId_salon());
        response.put("estado", salon.getEstado());
        response.put("mensaje", "Fase 1 registrada exitosamente. Proceda con la asignación de servicios e inventario.");
        return response;
    }

    // --- Validación Reutilizable de Propiedad ---
    private SalonEntity obtenerSalonValidado(Integer idSalon, Integer idPropietario) {
        SalonEntity salon = salonRepository.findById(idSalon)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salón no encontrado"));
        if (!salon.getUsuariosIdUser().equals(idPropietario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este salón");
        }
        return salon;
    }

    // --- FASE 2.A ---
    @Transactional
    public void asignarServicios(Integer idSalon, List<ServicioAsignadoDTO> servicios, Integer idPropietario) {
        SalonEntity salon = obtenerSalonValidado(idSalon, idPropietario);
        
        if (servicios.size() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes asignar más de 10 servicios.");
        }
        
        long countActual = salonServiciosRepository.countBySalonId(idSalon);
        if (countActual + servicios.size() > 10) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El salón excedería el límite de 10 servicios.");
        }

        for (ServicioAsignadoDTO dto : servicios) {
            ServiciosEntity servicio = serviciosRepository.findById(dto.getId_servicio())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
            
            SalonServiciosEntity relacion = new SalonServiciosEntity();
            relacion.setId(new SalonServiciosId(idSalon, servicio.getIdServicio()));
            relacion.setSalon(salon);
            relacion.setServicio(servicio);
            relacion.setNumeroServiciosSolicitados(dto.getNumeroServiciosSolicitados());
            
            salonServiciosRepository.save(relacion);
        }
    }

    // --- FASE 2.B ---
    @Transactional
    public void cargarInventarioLote(Integer idSalon, InventarioBatchDTO lote, Integer idPropietario) {
        SalonEntity salon = obtenerSalonValidado(idSalon, idPropietario);
        
        // Suponiendo que InventarioBatchDTO tiene un método getCategorias() que devuelve List<CategoriaBatchDTO>
        if (lote.getCategorias() == null || lote.getCategorias().isEmpty() || lote.getCategorias().size() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar entre 1 y 8 categorías.");
        }

        for (CategoriaBatchDTO catDTO : lote.getCategorias()) {
            if (catDTO.getRecursos().size() > 20) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Máximo 20 recursos por categoría.");
            }

            // Persiste o recupera la categoría (simplificado: asume creación directa por el lote)
            CategoriaRecursoEntity categoria = new CategoriaRecursoEntity();
            categoria.setNombreCategoria(catDTO.getNombreCategoria());
            // Si el nombre es UNIQUE, idealmente buscaríamos primero si existe, si no, lo guardamos.
            categoria = categoriaRecursoRepository.save(categoria);

            for (RecursoDTO recDTO : catDTO.getRecursos()) {
                InventarioSalonEntity inventario = new InventarioSalonEntity();
                inventario.setNombreRecurso(recDTO.getNombre_recurso());
                inventario.setCantidadTotal(recDTO.getCantidad_total());
                inventario.setSalon(salon);
                inventario.setCategoria(categoria);
                inventarioSalonRepository.save(inventario);
            }
        }
    }

    // --- FASE 2.C ---
    @Transactional
    public void publicarSalon(Integer idSalon, Integer idPropietario) {
        SalonEntity salon = obtenerSalonValidado(idSalon, idPropietario);
        
        if (salon.getUbicacion() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El salón no puede publicarse: Debe contar con una ubicación.");
        }
        
        long numServicios = salonServiciosRepository.countBySalonId(idSalon);
        long numCategorias = inventarioSalonRepository.countCategoriasBySalonId(idSalon);
        
        if (numServicios == 0 || numCategorias == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El salón no puede publicarse: Debe registrar al menos un servicio y un insumo en su inventario.");
        }
        
        salon.setEstado("PUBLICADO");
        salonRepository.save(salon);
    }

    public List<SalonEntity> obtenerSalonesPorPropietario(Integer idPropietario) {
        return salonRepository.findByUsuariosIdUser(idPropietario);
    }
    @Transactional
    public SalonEntity actualizarSalon(Integer idSalon, SalonCreateFase1DTO dto, Integer idPropietario) {
        SalonEntity salon = salonRepository.findById(idSalon)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salón no encontrado"));

        if (!salon.getUsuariosIdUser().equals(idPropietario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este salón");
        }
        salon.setNombreSalon(dto.getNombreSalon());
        salon.setCapacidadPersonas(dto.getCapacidadPersonas());
        salon.setPrecio_hora(dto.getPrecio_hora());
        salon.setDescripcion(dto.getDescripcion());
        if (dto.getImagenes() != null && !dto.getImagenes().isEmpty()) {
            salon.setImagen(String.join(",", dto.getImagenes()));
        } 
        UbicacionEntity ubicacion = salon.getUbicacion();
        if (ubicacion == null) {
            ubicacion = new UbicacionEntity();
            ubicacion.setSalon(salon);
            salon.setUbicacion(ubicacion);
        }
        
        ubicacion.setDireccion(dto.getUbicacion().getDireccion());
        ubicacion.setLatitud(dto.getUbicacion().getLatitud());
        ubicacion.setLongitud(dto.getUbicacion().getLongitud());
        ubicacion.setCiudad(dto.getUbicacion().getCiudad());
        ubicacion.setCP(dto.getUbicacion().getCp());
        return salonRepository.save(salon);
    }

    @Transactional
    public void eliminarSalon(Integer idSalon, Integer idPropietario) {
        SalonEntity salon = salonRepository.findById(idSalon)
            .orElseThrow(() -> new RuntimeException("Salón no encontrado"));
        
        if (!salon.getUsuariosIdUser().equals(idPropietario)) {
            throw new RuntimeException("No tienes permiso para eliminar este salón");
        }
        // Borrado en cascada (Elimina Salón y su Ubicación asociada)
        salonRepository.delete(salon);
    }
}