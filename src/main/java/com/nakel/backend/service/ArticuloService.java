package com.nakel.backend.service;

import com.nakel.backend.model.Articulo;
import com.nakel.backend.repository.ArticuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticuloService {

    @Autowired
    private ArticuloRepository articuloRepository;

    public List<Articulo> obtenerTodos() {
        return articuloRepository.findAll();
    }

    public Articulo guardarArticulo(Articulo articulo) {
        // En el futuro, si querés validar que el código de barras no se repita, lo hacés acá adentro.
        return articuloRepository.save(articulo);
    }
}