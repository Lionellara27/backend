package com.nakel.backend.service;

import com.nakel.backend.model.Material;
import com.nakel.backend.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MaterialService {
    private final MaterialRepository repository;

    public MaterialService(MaterialRepository repository) {
        this.repository = repository;
    }

    public List<Material> obtenerTodos() { return repository.findAll(); }
    public Material guardar(Material material) { return repository.save(material); }
    public void eliminar(Long id) { repository.deleteById(id); }
}