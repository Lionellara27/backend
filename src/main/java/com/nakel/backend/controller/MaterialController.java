package com.nakel.backend.controller;

import com.nakel.backend.model.Material;
import com.nakel.backend.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/materiales") // <--- ¡Esta es la ruta que tu Frontend busca!
@CrossOrigin(origins = "*")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public ResponseEntity<List<Material>> obtenerTodos() {
        return ResponseEntity.ok(materialService.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody Material material) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(materialService.guardar(material));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            materialService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}