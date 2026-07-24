package com.nakel.backend.controller;

import com.nakel.backend.model.MovimientoCaja;
import com.nakel.backend.service.MovimientoCajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos-caja")
@CrossOrigin(origins = "*") // 🔥 Permite que JavaFX/Frontend se conecte sin bloqueos CORS
public class MovimientoCajaController {

    @Autowired
    private MovimientoCajaService movimientoCajaService;

    // 🟢 Obtener todos los movimientos de una caja específica (Para llenar el modal del Ojito 👁️)
    // Ejemplo: GET http://localhost:8080/api/movimientos-caja/caja/1
    @GetMapping("/caja/{cajaId}")
    public ResponseEntity<List<MovimientoCaja>> obtenerPorCaja(@PathVariable Long cajaId) {
        List<MovimientoCaja> movimientos = movimientoCajaService.obtenerMovimientosPorCaja(cajaId);
        return ResponseEntity.ok(movimientos);
    }
}