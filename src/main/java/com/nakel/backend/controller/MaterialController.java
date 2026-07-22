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
    public ResponseEntity<Material> guardar(@RequestBody Material material) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.guardar(material));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        materialService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}