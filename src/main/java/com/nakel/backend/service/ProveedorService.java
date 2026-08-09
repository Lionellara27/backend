package com.nakel.backend.service;

import com.nakel.backend.model.Proveedor;
import com.nakel.backend.repository.ProveedorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    private final ProveedorRepository repository;

    // Usamos el constructor para inyectar el repositorio (Buena práctica de Spring Boot)
    public ProveedorService(ProveedorRepository repository) {
        this.repository = repository;
    }

    // 📋 Trae la lista PAGINADA para llenar la tabla del front sin saturar la memoria
    @Transactional(readOnly = true)
    public Page<Proveedor> obtenerTodos(Pageable pageable) {
        return repository.findAll(pageable);
    }

    // 🔍 Busca un proveedor específico (Lo usa el PUT para actualizar)
    public Optional<Proveedor> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    // ➕ Guarda uno nuevo o guarda los cambios de uno existente
    @Transactional
    public Proveedor guardar(Proveedor proveedor) {
        return repository.save(proveedor);
    }

    // 🗑️ Borra un proveedor por su ID
    @Transactional
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Proveedor> buscarPorNombre(String nombre, Pageable pageable) {
        return repository.findByRazonSocialContainingIgnoreCase(nombre, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Proveedor> buscarPorContacto(String nombre, Pageable pageable) {
        return repository.findByNombreContactoContainingIgnoreCase(nombre, pageable);
    }

    // 🔍 Endpoint para verificar CUIT (Llamado por el Controller)
    public Optional<Proveedor> buscarPorCuit(String cuit) {
        return repository.findByCuit(cuit);
    }
}