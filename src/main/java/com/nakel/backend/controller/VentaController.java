package com.nakel.backend.controller;

import com.nakel.backend.model.Venta;
import com.nakel.backend.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class VentaController {

    // Ahora inyectamos el Service, no el Repository directamente
    @Autowired
    private VentaService ventaService;

    @PostMapping
    public Venta cobrarVenta(@RequestBody Venta venta) {
        // El mozo le pasa el pedido a la cocina y devuelve el plato listo
        return ventaService.procesarYGuardarVenta(venta);
    }
}