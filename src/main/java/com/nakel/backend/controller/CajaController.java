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

    // 🔥 Agregamos ?username=... para saber quién abre la caja
    @GetMapping("/actual")
    public ResponseEntity<CajaDiaria> obtenerCajaActual(@RequestParam String username) {
        return ResponseEntity.ok(cajaService.obtenerOCrearCajaActual(username));
    }

    // 🔥 Agregamos ?username=... para saber quién la cierra
    @PostMapping("/cerrar")
    public ResponseEntity<?> cerrarCaja(@RequestParam String username) {
        try {
            CajaDiaria cajaCerrada = cajaService.cerrarCajaActual(username);
            return ResponseEntity.ok(cajaCerrada);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Historial (Este queda igual porque solo lee datos)
    @GetMapping("/historial")
    public ResponseEntity<List<CajaDiaria>> obtenerHistorial() {
        return ResponseEntity.ok(cajaService.obtenerHistorialCajas());
    }
}