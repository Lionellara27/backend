package com.nakel.backend.controller;

import com.nakel.backend.dto.ArticuloInfoDTO; // 🔥 Importante: importar el DTO
import com.nakel.backend.model.Cambio;
import com.nakel.backend.service.CambioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cambios")
public class CambioController {

    private final CambioService cambioService;

    @Autowired
    public CambioController(CambioService cambioService) {
        this.cambioService = cambioService;
    }

    // 🔥 Guarda un nuevo registro de cambio asociado a una venta específica
    @PostMapping("/venta/{idVenta}")
    public ResponseEntity<?> registrarCambio(@PathVariable Long idVenta, @RequestBody Cambio cambio) {
        try {
            Cambio nuevoCambio = cambioService.registrarCambio(idVenta, cambio);
            return ResponseEntity.ok(nuevoCambio);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔥 Trae todo el historial de cambios de un ticket para mostrarlo en pantalla
    @GetMapping("/venta/{idVenta}")
    public ResponseEntity<List<Cambio>> obtenerHistorialCambios(@PathVariable Long idVenta) {
        List<Cambio> historial = cambioService.obtenerHistorialPorVenta(idVenta);
        return ResponseEntity.ok(historial);
    }

    // 🔥 NUEVO ENDPOINT: Trae el "Ticket Vivo" (lo que el cliente tiene realmente hoy en su poder)
    @GetMapping("/disponibles-actuales/{idVenta}")
    public ResponseEntity<List<ArticuloInfoDTO>> obtenerArticulosActuales(@PathVariable Long idVenta) {
        List<ArticuloInfoDTO> actuales = cambioService.obtenerArticulosActualesDeVenta(idVenta);
        return ResponseEntity.ok(actuales);
    }
}
//