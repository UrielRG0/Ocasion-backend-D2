package com.ocasion.backend.salones.Services;

import com.ocasion.backend.salones.Entities.CategoriaRecursoEntity;
import com.ocasion.backend.salones.Entities.InventarioSalonEntity;
import com.ocasion.backend.salones.Entities.SalonEntity;
import com.ocasion.backend.salones.Repositories.CategoriaRecursoRepository;
import com.ocasion.backend.salones.Repositories.InventarioSalonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventarioService {

    private final CategoriaRecursoRepository categoriaRepository;
    private final InventarioSalonRepository inventarioRepository;
    private final SalonService salonService;

    public InventarioService(CategoriaRecursoRepository categoriaRepository, InventarioSalonRepository inventarioRepository, SalonService salonService) {
        this.categoriaRepository = categoriaRepository;
        this.inventarioRepository = inventarioRepository;
        this.salonService = salonService;
    }

    // --- CATEGORÍAS ---

    @Transactional
    public void actualizarCategoria(Integer idCategoria, String nuevoNombre, String nuevaDescripcion) {
        CategoriaRecursoEntity categoria = categoriaRepository.findById(idCategoria)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada."));
        categoria.setNombreCategoria(nuevoNombre);
        categoria.setDescripcion(nuevaDescripcion);
        categoriaRepository.save(categoria);
    }

    @Transactional
    public void eliminarCategoria(Integer idCategoria) {
        if (categoriaRepository.existsByCategoriaIdAndInsumosNotEmpty(idCategoria)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar la categoría porque tiene recursos registrados.");
        }
        categoriaRepository.deleteById(idCategoria);
    }

    // --- INVENTARIO (RECURSOS) ---

    @Transactional
    public InventarioSalonEntity agregarRecurso(Integer idSalon, Integer idCategoria, String nombre, Integer cantidad, Integer idPropietario) {
        SalonEntity salon = salonService.obtenerSalonesPorPropietario(idPropietario).stream()
            .filter(s -> s.getId_salon().equals(idSalon)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permiso."));

        if (cantidad < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser >= 1.");

        long recursosActuales = inventarioRepository.countBySalonIdAndCategoriaId(idSalon, idCategoria);
        if (recursosActuales >= 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Límite de 20 recursos por categoría alcanzado.");
        }

        CategoriaRecursoEntity categoria = categoriaRepository.findById(idCategoria)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada."));

        InventarioSalonEntity inventario = new InventarioSalonEntity();
        inventario.setNombreRecurso(nombre);
        inventario.setCantidadTotal(cantidad);
        inventario.setSalon(salon);
        inventario.setCategoria(categoria);
        return inventarioRepository.save(inventario);
    }

    @Transactional
    public void eliminarRecurso(Integer idRecurso) {
        if (!inventarioRepository.existsById(idRecurso)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso no encontrado.");
        }
        inventarioRepository.deleteById(idRecurso);
    }

    @Transactional
    public CategoriaRecursoEntity crearCategoriaIndividual(Integer idSalon, String nombre, String descripcion, Integer idPropietario) {
        SalonEntity salon = salonService.obtenerSalonesPorPropietario(idPropietario).stream()
            .filter(s -> s.getId_salon().equals(idSalon)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permiso."));

        long totalCategorias = inventarioRepository.countCategoriasBySalonId(idSalon);
        if (totalCategorias >= 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Límite de 8 categorías alcanzado.");
        }

        CategoriaRecursoEntity categoria = new CategoriaRecursoEntity();
        categoria.setNombreCategoria(nombre);
        categoria.setDescripcion(descripcion);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void actualizarRecurso(Integer idRecurso, String nuevoNombre, Integer nuevaCantidad) {
        InventarioSalonEntity inventario = inventarioRepository.findById(idRecurso)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso no encontrado."));
        
        if (nuevaCantidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser >= 1.");
        }
        
        inventario.setNombreRecurso(nuevoNombre);
        inventario.setCantidadTotal(nuevaCantidad);
        inventarioRepository.save(inventario);
    }
}