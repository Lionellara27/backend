package com.nakel.backend.controller;

import com.nakel.backend.model.CategoriaInsumo;
import com.nakel.backend.service.CategoriaInsumoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-insumo")
@CrossOrigin(origins = "*")
public class CategoriaInsumoController {

    private final CategoriaInsumoService categoriaService;

    public CategoriaInsumoController(CategoriaInsumoService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // 🟢 GET: Devuelve la lista completa para el ComboBox del Frontend
    @GetMapping
    public ResponseEntity<List<CategoriaInsumo>> obtenerTodas() {
        return ResponseEntity.ok(categoriaService.obtenerTodas());
    }

    // 🟢 POST: Para cuando la clienta toca "Crear nueva categoría..."
    @PostMapping
    public ResponseEntity<?> guardarCategoria(@RequestBody CategoriaInsumo categoria) {
        try {
            CategoriaInsumo nuevaCategoria = categoriaService.guardarCategoria(categoria);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaCategoria);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 🟢 DELETE: Por si necesita borrar alguna
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCategoria(@PathVariable Long id) {
        try {
            categoriaService.eliminarCategoria(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}