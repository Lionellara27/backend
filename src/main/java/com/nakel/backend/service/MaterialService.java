package com.nakel.backend.service;

import com.nakel.backend.model.Material;
import com.nakel.backend.repository.ArticuloRepository;
import com.nakel.backend.repository.InsumoRepository;
import com.nakel.backend.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MaterialService {
    private final MaterialRepository repository;
    private final ArticuloRepository articuloRepository;
    private final InsumoRepository insumoRepository;

    public MaterialService(MaterialRepository repository, ArticuloRepository articuloRepository,
                           InsumoRepository insumoRepository) {
        this.repository = repository;
        this.articuloRepository = articuloRepository;
        this.insumoRepository = insumoRepository;
    }

    public List<Material> obtenerTodos() { return repository.findAll(); }

    public Material guardar(Material material) {
        if (material.getNombre() == null || material.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del material es obligatorio.");
        }
        String nombre = material.getNombre().trim();
        material.setNombre(nombre);
        boolean repetido = repository.findAll().stream()
                .anyMatch(m -> m.getNombre().equalsIgnoreCase(nombre) && !m.getId().equals(material.getId()));
        if (repetido) {
            throw new RuntimeException("Ya existe un material llamado " + nombre + ".");
        }
        return repository.save(material);
    }

    // SQLite no hace cumplir las claves foráneas: lo controlamos a mano.
    public void eliminar(Long id) {
        long articulos = articuloRepository.contarActivosPorMaterial(id);
        long insumos = insumoRepository.countByMaterialId(id);
        if (articulos > 0 || insumos > 0) {
            throw new RuntimeException("No se puede eliminar: el material está en uso en "
                    + articulos + " artículo(s) y " + insumos + " insumo(s).");
        }
        repository.deleteById(id);
    }
}
