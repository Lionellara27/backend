package com.nakel.backend.service;

import com.nakel.backend.model.CategoriaInsumo;
import com.nakel.backend.repository.CategoriaInsumoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaInsumoService {

    private final CategoriaInsumoRepository categoriaRepository;

    public CategoriaInsumoService(CategoriaInsumoRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
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
        categoriaRepository.deleteById(id);
    }
}