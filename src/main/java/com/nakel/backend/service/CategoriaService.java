package com.nakel.backend.service;

import com.nakel.backend.model.Categoria;
import com.nakel.backend.repository.ArticuloRepository;
import com.nakel.backend.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoriaService {
    private final CategoriaRepository repository;
    private final ArticuloRepository articuloRepository;

    public CategoriaService(CategoriaRepository repository, ArticuloRepository articuloRepository) {
        this.repository = repository;
        this.articuloRepository = articuloRepository;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Categoria> obtenerTodas() { return repository.findAll(); }

    public Categoria guardarCategoria(Categoria cat) {
        if (cat.getNombre() == null || cat.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la categoría es obligatorio.");
        }
        if (cat.getPrefijoSku() == null || cat.getPrefijoSku().isBlank()) {
            throw new RuntimeException("El prefijo de código (SKU) es obligatorio.");
        }
        String nombre = cat.getNombre().trim();
        cat.setNombre(nombre);
        boolean repetida = repository.findAll().stream()
                .anyMatch(c -> c.getNombre().equalsIgnoreCase(nombre) && !c.getId().equals(cat.getId()));
        if (repetida) {
            throw new RuntimeException("Ya existe una categoría llamada " + nombre + ".");
        }
        return repository.save(cat);
    }

    // SQLite no hace cumplir las claves foráneas: lo controlamos a mano para que
    // ningún artículo quede apuntando a una categoría que ya no existe.
    public void eliminarCategoria(Long id) {
        long enUso = articuloRepository.contarActivosPorCategoria(id);
        if (enUso > 0) {
            throw new RuntimeException("No se puede eliminar: hay " + enUso
                    + (enUso == 1 ? " artículo que usa" : " artículos que usan") + " esta categoría.");
        }
        repository.deleteById(id);
    }
}
