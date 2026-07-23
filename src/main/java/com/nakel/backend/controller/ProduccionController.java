package com.nakel.backend.controller;

import com.nakel.backend.dto.ProduccionRequestDTO;
import com.nakel.backend.service.ProduccionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/produccion")
@CrossOrigin(origins = "*") // Permite que el frontend de JavaFX se comunique
public class ProduccionController {

    @Autowired
    private ProduccionService produccionService;

    @PostMapping("/fabricar")
    public ResponseEntity<?> procesarFabricacion(@RequestBody ProduccionRequestDTO peticion) {
        try {
            produccionService.procesarFabricacionYAlta(peticion);
            return ResponseEntity.ok("¡Producto fabricado y stock descontado con éxito!");
        } catch (Exception e) {
            // Si el patovica (el validador de stock) frena algo, devuelve el error acá
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/siguiente-sku/{idCategoria}")
    public ResponseEntity<String> obtenerSiguienteSku(@PathVariable Long idCategoria) {
        try {
            // Llama al método que armamos antes en el ProduccionService
            String siguienteSku = produccionService.obtenerSiguienteSku(idCategoria);
            return ResponseEntity.ok(siguienteSku);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al generar SKU");
        }
    }
}