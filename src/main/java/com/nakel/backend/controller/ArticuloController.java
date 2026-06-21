package com.nakel.backend.controller;

import com.nakel.backend.model.Articulo;
import com.nakel.backend.repository.ArticuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articulos")
@CrossOrigin(origins = "*")
public class ArticuloController {

    @Autowired
    private ArticuloRepository articuloRepository;

    // Método para obtener el stock
    @GetMapping
    public List<Articulo> obtenerTodos() {
        return articuloRepository.findAll();
    }

    // Método para crear un artículo (Lo va a usar tu Calculadora Dinámica al final)
    @PostMapping
    public Articulo guardarArticulo(@RequestBody Articulo articulo) {
        return articuloRepository.save(articulo);
    }
}