package com.nakel.backend.service;

import com.nakel.backend.model.Categoria;
import com.nakel.backend.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoriaService {
    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public List<Categoria> obtenerTodas() { return repository.findAll(); }
    public Categoria guardarCategoria(Categoria cat) { return repository.save(cat); }
    public void eliminarCategoria(Long id) { repository.deleteById(id); }
}