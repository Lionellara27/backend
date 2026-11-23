package com.nakel.backend.service;

import com.nakel.backend.model.CategoriaInsumo;
import com.nakel.backend.repository.CategoriaInsumoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaInsumoService {

    private final CategoriaInsumoRepository categoriaRepository;
    private final com.nakel.backend.repository.InsumoRepository insumoRepository;

    public CategoriaInsumoService(CategoriaInsumoRepository categoriaRepository,
                                  com.nakel.backend.repository.InsumoRepository insumoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.insumoRepository = insumoRepository;
    }

    // 🟢 OBTENER TODAS (Ideal para llenar tu ComboBox)
    @Transactional(readOnly = true)
    public List<CategoriaInsumo> obtenerTodas() {
        return categoriaRepository.findAll();
    }

    // 🟢 GUARDAR NUEVA (Para cuando elijan "➕ Crear nueva categoría...")
    @Transactional
    public CategoriaInsumo guardarCategoria(CategoriaInsumo categoria) {
        // Validación súper básica para evitar nulos
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la categoría no puede estar vacío.");
        }
        if (categoria.getTipoMedicion() == null || categoria.getTipoMedicion().isBlank()) {
            throw new RuntimeException("Debe especificar cómo se mide esta categoría (SUPERFICIE, UNIDAD, etc.).");
        }

        return categoriaRepository.save(categoria);
    }

    // 🟢 ELIMINAR (Opcional, por si se equivocan al crearla)
    @Transactional
    public void eliminarCategoria(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new RuntimeException("La categoría no existe.");
        }
        long enUso = insumoRepository.countByCategoriaId(id);
        if (enUso > 0) {
            throw new RuntimeException("No se puede eliminar: hay " + enUso + " insumo(s) que usan esta categoría.");
        }
        categoriaRepository.deleteById(id);
    }
}