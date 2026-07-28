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

    // Spring Boot inyecta el servicio automáticamente
    public ArticuloController(ArticuloService articuloService) {
        this.articuloService = articuloService;
    }

    // 📋 GET: Obtener todos los artículos
    @GetMapping
    public List<Articulo> obtenerTodos() {
        return articuloService.obtenerTodos();
    }

    // 🔍 GET: Buscar por código de barras / SKU
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Articulo> buscarPorCodigo(@PathVariable String codigo) {
        Articulo articulo = articuloService.buscarPorCodigo(codigo);
        if (articulo != null) {
            return ResponseEntity.ok(articulo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 💾 POST: Guardar nuevo artículo
    @PostMapping
    public Articulo guardarArticulo(@RequestBody Articulo articulo) {
        return articuloService.guardarArticulo(articulo);
    }

    // 🔄 PUT: Actualizar/Editar artículo existente (¡EL QUE FALTABA!)
    @PutMapping("/{id}")
    public ResponseEntity<Articulo> actualizarArticulo(@PathVariable Long id, @RequestBody Articulo articuloDetalles) {
        try {
            Articulo actualizado = articuloService.actualizarArticulo(id, articuloDetalles);
            if (actualizado != null) {
                return ResponseEntity.ok(actualizado);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 🗑️ DELETE: Eliminar artículo por ID (¡EL QUE FALTABA!)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarArticulo(@PathVariable Long id) {
        try {
            boolean borrado = articuloService.eliminarArticulo(id);
            if (borrado) {
                return ResponseEntity.noContent().build(); // 204 No Content (Éxito)
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            // Si salta una restricción de clave foránea (artículo ya vendido), devuelve 409 Conflict o 500
            return ResponseEntity.status(409).build();
        }
    }
}