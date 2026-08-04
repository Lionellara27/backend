package com.nakel.backend.controller;

import com.nakel.backend.model.Vale;
import com.nakel.backend.service.ValeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/vales")
public class ValeController {

    private final ValeService valeService;

    @Autowired
    public ValeController(ValeService valeService) {
        this.valeService = valeService;
    }

    // 🔥 1. Ahora recibe el monto y, si existe, el ID del cliente
    @PostMapping("/generar")
    public ResponseEntity<Vale> generarVale(
            @RequestParam BigDecimal monto,
            @RequestParam(required = false) Long idCliente) {

        // Le pasamos los dos datos al servicio
        Vale nuevoVale = valeService.generarVale(monto, idCliente);
        return ResponseEntity.ok(nuevoVale);
    }

    // 🔥 2. Este queda IGUAL (la validación de la fecha se hace en el Service)
    @GetMapping("/validar/{codigo}")
    public ResponseEntity<?> validarVale(@PathVariable String codigo) {
        try {
            Vale vale = valeService.validarVale(codigo);
            return ResponseEntity.ok(vale);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔥 3. Este queda IGUAL
    @PutMapping("/consumir/{codigo}")
    public ResponseEntity<?> consumirVale(@PathVariable String codigo) {
        try {
            Vale vale = valeService.consumirVale(codigo);
            return ResponseEntity.ok(vale);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}