package com.nakel.backend.controller;

import com.nakel.backend.model.CajaDiaria;
import com.nakel.backend.service.CajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
@CrossOrigin(origins = "*")
public class CajaController {

    @Autowired
    private CajaService cajaService;

    // Obtener caja actual (la abre si no existe)
    @GetMapping("/actual")
    public ResponseEntity<CajaDiaria> obtenerCajaActual() {
        return ResponseEntity.ok(cajaService.obtenerOCrearCajaActual());
    }

    // Cerrar caja
    @PostMapping("/cerrar")
    public ResponseEntity<?> cerrarCaja() {
        try {
            CajaDiaria cajaCerrada = cajaService.cerrarCajaActual();
            return ResponseEntity.ok(cajaCerrada);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Historial
    @GetMapping("/historial")
    public ResponseEntity<List<CajaDiaria>> obtenerHistorial() {
        return ResponseEntity.ok(cajaService.obtenerHistorialCajas());
    }
}