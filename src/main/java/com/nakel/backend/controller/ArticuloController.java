package com.nakel.backend.controller;

import com.nakel.backend.model.Articulo;
import com.nakel.backend.service.ArticuloService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articulos")
@CrossOrigin(origins = "*")
public class ArticuloController {

    private final ArticuloService articuloService;

    // Spring Boot inyecta el servicio automáticamente acá
    public ArticuloController(ArticuloService articuloService) {
        this.articuloService = articuloService;
    }

    // 🚀 ESTE ES EL QUE TE HABÍAS OLVIDADO (Fundamental para la tabla de inventario)
    @GetMapping
    public List<Articulo> obtenerTodos() {
        return articuloService.obtenerTodos();
    }

    // 🔍 Endpoint con "chaleco antibalas" para el lector láser
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Articulo> buscarPorCodigo(@PathVariable String codigo) {
        Articulo articulo = articuloService.buscarPorCodigo(codigo);

        if (articulo != null) {
            // Si lo encuentra, devuelve código 200 y el artículo
            return ResponseEntity.ok(articulo);
        } else {
            // Si NO lo encuentra, devuelve un código 404 (Not Found)
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Articulo guardarArticulo(@RequestBody Articulo articulo) {
        return articuloService.guardarArticulo(articulo);
    }
}