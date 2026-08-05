package com.nakel.backend.service;

import com.nakel.backend.model.Proveedor;
import com.nakel.backend.repository.ProveedorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    private final ProveedorRepository repository;

    // Usamos el constructor para inyectar el repositorio (Buena práctica de Spring Boot)
    public ProveedorService(ProveedorRepository repository) {
        this.repository = repository;
    }

    // 📋 Trae la lista completa para llenar la tabla del front
    public List<Proveedor> obtenerTodos() {
        return repository.findAll();
    }

    // 🔍 Busca un proveedor específico (Lo usa el PUT para actualizar)
    public Optional<Proveedor> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    // ➕ Guarda uno nuevo o guarda los cambios de uno existente
    public Proveedor guardar(Proveedor proveedor) {
        return repository.save(proveedor);
    }

    // 🗑️ Borra un proveedor por su ID
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    public List<Proveedor> buscarPorNombre(String nombre) {
        return repository.findByRazonSocialContainingIgnoreCase(nombre);
    }

    // 🔍 Endpoint para verificar CUIT (Llamado por el Controller)
    public Optional<Proveedor> buscarPorCuit(String cuit) {
        return repository.findByCuit(cuit);
    }
}