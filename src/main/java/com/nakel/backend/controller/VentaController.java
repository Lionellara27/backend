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

    @PostMapping
    public ResponseEntity<Venta> cobrarVenta(@RequestBody Venta venta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.procesarYGuardarVenta(venta));
    }

    // Adentro de tu VentaController (Backend)
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<Venta>> obtenerHistorialVentas(org.springframework.data.domain.Pageable pageable) {
        // Asumiendo que tenés el repository inyectado en el controller o llamás a un service
        return ResponseEntity.ok(service.obtenerTodasLasVentas(pageable));
    }
}