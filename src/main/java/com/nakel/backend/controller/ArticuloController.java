package com.nakel.backend.controller;

import com.nakel.backend.model.Articulo;
import com.nakel.backend.service.ArticuloService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    // 📋 GET: Obtener todos los artículos (Con soporte para Mostrador y Catálogo)
    @GetMapping
    public Page<Articulo> obtenerTodos(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) Boolean stockDisponible, // 🔥 ATajamos lo que manda el Mostrador
            @PageableDefault(size = 50) Pageable pageable) {

        // 🛡️ REGLA DE ORO: Si es el Mostrador, filtramos por stock > 0
        if (Boolean.TRUE.equals(stockDisponible)) {
            return articuloService.buscarParaVenta(buscar, pageable);
        }

        // 📋 Si es el Catálogo, buscamos con todos los filtros normales
        return articuloService.buscarConFiltros(
                buscar,
                categoriaId,
                materialId,
                origen,
                pageable
        );
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

    // 🔄 PUT: Actualizar/Editar artículo existente
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

    // 🗑️ DELETE: Eliminar artículo por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarArticulo(@PathVariable Long id) {
        try {
            boolean borrado = articuloService.eliminarArticulo(id);
            if (borrado) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(409).build();
        }
    }

    // 🔥 NUEVO: Sumar stock (cuando devuelven un producto)
    @PutMapping("/{id}/restaurar-stock")
    public ResponseEntity<?> restaurarStock(@PathVariable Long id, @RequestParam int cantidad) {
        try {
            Articulo actualizado = articuloService.restaurarStock(id, cantidad);
            if (actualizado != null) {
                return ResponseEntity.ok(actualizado);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al restaurar stock: " + e.getMessage());
        }
    }

    // 🔥 NUEVO: Restar stock (cuando se llevan un producto en el cambio)
    @PutMapping("/{id}/descontar-stock")
    public ResponseEntity<?> descontarStock(@PathVariable Long id, @RequestParam int cantidad) {
        try {
            Articulo actualizado = articuloService.descontarStock(id, cantidad);
            if (actualizado != null) {
                return ResponseEntity.ok(actualizado);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al descontar stock: " + e.getMessage());
        }
    }
    @PutMapping("/aumentar-precios")
    public ResponseEntity<?> aumentarPrecios(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam BigDecimal porcentaje) {

        System.out.println("========== BACKEND AUMENTO MASIVO ==========");
        System.out.println("📦 Categoría ID: " + categoriaId);
        System.out.println("📈 Porcentaje: " + porcentaje);

        try {
            int cantidad = articuloService.aumentarPrecios(
                    categoriaId,
                    porcentaje
            );

            System.out.println("✅ Artículos actualizados: " + cantidad);

            return ResponseEntity.ok(cantidad);

        } catch (Exception e) {
            System.err.println("❌ ERROR REAL EN AUMENTO MASIVO:");
            e.printStackTrace();

            return ResponseEntity.badRequest()
                    .body("Error al aumentar precios: " + e.getMessage());
        }
    }
}
///