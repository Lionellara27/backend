package com.nakel.backend.controller;

import com.nakel.backend.model.Articulo;
import com.nakel.backend.service.ArticuloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articulos")
@CrossOrigin(origins = "*")
public class ArticuloController {

    @Autowired
    private ArticuloService articuloService;

    @GetMapping
    public List<Articulo> obtenerTodos() {
        return articuloService.obtenerTodos();
    }

    // 🔍 ESTO ES LO QUE LE FALTA AL BACKEND
    @GetMapping("/codigo/{codigo}")
    public Articulo buscarPorCodigo(@PathVariable String codigo) {
        // Necesitas que tu service tenga este método, o podés usar el repository directo:
        return articuloService.buscarPorCodigo(codigo);
    }

    @PostMapping
    public Articulo guardarArticulo(@RequestBody Articulo articulo) {
        return articuloService.guardarArticulo(articulo);
    }
}