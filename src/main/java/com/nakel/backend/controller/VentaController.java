package com.nakel.backend.controller;

import com.nakel.backend.model.Venta;
import com.nakel.backend.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class VentaController {

    // Ahora inyectamos el Service, no el Repository directamente
    @Autowired
    private VentaService ventaService;

    @PostMapping
    public ResponseEntity<Venta> cobrarVenta(@RequestBody Venta venta) {
        // 1. Procesamos la venta en el service
        Venta ventaGuardada = ventaService.procesarYGuardarVenta(venta);

        // 2. Retornamos 201 Created, que es lo que tu Frontend está esperando
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaGuardada);
    }
}