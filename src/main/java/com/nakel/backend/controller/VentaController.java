package com.nakel.backend.controller;

import com.nakel.backend.model.Venta;
import com.nakel.backend.service.VentaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class VentaController {

    private final VentaService service;

    public VentaController(VentaService service) {
        this.service = service;
    }

    // 🔥 Agregamos el @RequestParam String username
    @PostMapping
    public ResponseEntity<Venta> cobrarVenta(@RequestBody Venta venta, @RequestParam String username) {
        // 🔥 Le pasamos la venta Y el username al servicio
        return ResponseEntity.status(HttpStatus.CREATED).body(service.procesarYGuardarVenta(venta, username));
    }

    // Historial de Ventas (Este queda intacto)
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<Venta>> obtenerHistorialVentas(org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(service.obtenerTodasLasVentas(pageable));
    }
}