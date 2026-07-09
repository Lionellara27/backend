package com.nakel.backend.service;

import com.nakel.backend.model.CategoriaInsumo;
import com.nakel.backend.model.Insumo;
import com.nakel.backend.repository.CategoriaInsumoRepository;
import com.nakel.backend.repository.InsumoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service // Le avisa a Spring que esta clase es la que piensa
public class InsumoService {

    // Traemos las dos herramientas que necesitamos para guardar
    private final InsumoRepository insumoRepository;
    private final CategoriaInsumoRepository categoriaRepository;

    // Inyección de dependencias por constructor (Buena práctica)
    public InsumoService(InsumoRepository insumoRepository, CategoriaInsumoRepository categoriaRepository) {
        this.insumoRepository = insumoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // 🟢 OBTENER TODOS (BLINDADO CON PAGINACIÓN)
    @Transactional(readOnly = true)
    public Page<Insumo> obtenerTodos(Pageable pageable) {
        return insumoRepository.findAll(pageable);
    }

    // 🟢 BUSCADOR PREDICTIVO
    @Transactional(readOnly = true)
    public Page<Insumo> buscarPorNombre(String nombre, Pageable pageable) {
        return insumoRepository.findByNombreContainingIgnoreCase(nombre, pageable);
    }

    // 🟢 GUARDAR NUEVO
    @Transactional
    public Insumo guardarInsumo(Insumo insumo) {
        // VALIDACIÓN CLAVE: Buscamos que la categoría exista de verdad antes de guardar
        if (insumo.getCategoria() != null && insumo.getCategoria().getId() != null) {
            CategoriaInsumo categoriaReal = categoriaRepository.findById(insumo.getCategoria().getId())
                    .orElseThrow(() -> new RuntimeException("Error: La categoría seleccionada no existe en el sistema."));

            // Le pegamos la categoría real de la BD al insumo
            insumo.setCategoria(categoriaReal);
        } else {
            throw new RuntimeException("Error: El insumo debe tener una categoría válida asignada.");
        }

        return insumoRepository.save(insumo);
    }

    // 🟢 ACTUALIZAR EXISTENTE
    @Transactional
    public Insumo actualizarInsumo(Long id, Insumo insumoActualizado) {
        // 1. Buscamos el viejo
        Insumo insumoExistente = insumoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Insumo no encontrado con el ID " + id));

        // 2. Le pisamos los datos con lo que vino del Front
        insumoExistente.setNombre(insumoActualizado.getNombre());
        insumoExistente.setCostoTotal(insumoActualizado.getCostoTotal());
        insumoExistente.setAnchoCm(insumoActualizado.getAnchoCm());
        insumoExistente.setLargoCm(insumoActualizado.getLargoCm());
        insumoExistente.setCantidad(insumoActualizado.getCantidad());

        // 3. Verificamos la categoría por si la cambió
        if (insumoActualizado.getCategoria() != null && insumoActualizado.getCategoria().getId() != null) {
            CategoriaInsumo categoriaReal = categoriaRepository.findById(insumoActualizado.getCategoria().getId())
                    .orElseThrow(() -> new RuntimeException("Error: La nueva categoría no existe."));
            insumoExistente.setCategoria(categoriaReal);
        }

        // 4. Guardamos
        return insumoRepository.save(insumoExistente);
    }

    // 🟢 ELIMINAR
    @Transactional
    public void eliminarInsumo(Long id) {
        if (!insumoRepository.existsById(id)) {
            throw new RuntimeException("Error: No se puede eliminar. El insumo no existe.");
        }
        insumoRepository.deleteById(id);
    }
}