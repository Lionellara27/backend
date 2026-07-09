package com.nakel.backend.controller;

import com.nakel.backend.model.Insumo;
import com.nakel.backend.service.InsumoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Le dice a Spring "Soy una API REST, devuelvo JSON, no páginas web"
@RequestMapping("/api/insumos") // La ruta base a la que le pega tu HttpClient
@CrossOrigin(origins = "*") // Permite que cualquier front (JavaFX, web, celular) se conecte sin bloqueos de seguridad
public class InsumoController {

    private final InsumoService insumoService;

    public InsumoController(InsumoService insumoService) {
        this.insumoService = insumoService;
    }

    // 🟢 GET: OBTENER TODOS (Paginado)
    @GetMapping
    public ResponseEntity<Page<Insumo>> obtenerTodos(Pageable pageable) {
        // Spring Boot automáticamente lee cosas como ?page=0&size=20 de la URL
        Page<Insumo> insumos = insumoService.obtenerTodos(pageable);
        return ResponseEntity.ok(insumos);
    }

    // 🟢 GET: BUSCADOR PREDICTIVO
    // Ejemplo: /api/insumos/buscar?nombre=cuero
    @GetMapping("/buscar")
    public ResponseEntity<Page<Insumo>> buscarPorNombre(
            @RequestParam String nombre,
            Pageable pageable) {
        Page<Insumo> resultados = insumoService.buscarPorNombre(nombre, pageable);
        return ResponseEntity.ok(resultados);
    }

    // 🟢 POST: GUARDAR NUEVO
    @PostMapping
    public ResponseEntity<?> guardarInsumo(@RequestBody Insumo insumo) {
        try {
            Insumo insumoGuardado = insumoService.guardarInsumo(insumo);
            return ResponseEntity.status(HttpStatus.CREATED).body(insumoGuardado); // Devuelve 201
        } catch (Exception e) {
            // Si el service tira una excepción (ej: Categoría no existe), mandamos un 400 (Bad Request)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 🟢 PUT: ACTUALIZAR EXISTENTE
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarInsumo(@PathVariable Long id, @RequestBody Insumo insumo) {
        try {
            Insumo insumoActualizado = insumoService.actualizarInsumo(id, insumo);
            return ResponseEntity.ok(insumoActualizado); // Devuelve 200
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 🟢 DELETE: BORRAR
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarInsumo(@PathVariable Long id) {
        try {
            insumoService.eliminarInsumo(id);
            return ResponseEntity.noContent().build(); // Devuelve 204 (Todo ok, no hay nada que devolver)
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}