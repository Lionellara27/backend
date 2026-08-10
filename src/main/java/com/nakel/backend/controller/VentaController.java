package com.nakel.backend.controller;

import com.nakel.backend.model.Venta;
import com.nakel.backend.service.VentaService;
import com.nakel.backend.service.CorreoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class VentaController {

    private final VentaService service;
    private final CorreoService correoService;

    public VentaController(VentaService service, CorreoService correoService) {
        this.service = service;
        this.correoService = correoService;
    }

    @PostMapping
    public ResponseEntity<Venta> cobrarVenta(@RequestBody Venta venta, @RequestParam String username) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.procesarYGuardarVenta(venta, username));
    }

    // 🔥 ACTUALIZADO: Ataja el 'criterio'
    @GetMapping("/historial")
    public ResponseEntity<Page<Venta>> obtenerHistorialVentas(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false, defaultValue = "Nro. Comprobante") String criterio,
            @RequestParam(required = false, defaultValue = "0") int mes,
            Pageable pageable) {

        return ResponseEntity.ok(service.obtenerHistorialConFiltros(buscar, criterio, mes, pageable));
    }

    // 🔥 ACTUALIZADO: Ataja el 'criterio' para el total
    @GetMapping("/historial/total")
    public ResponseEntity<Double> obtenerTotalGlobal(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false, defaultValue = "Nro. Comprobante") String criterio,
            @RequestParam(required = false, defaultValue = "0") int mes) {

        return ResponseEntity.ok(service.obtenerTotalGlobal(buscar, criterio, mes));
    }

    @PostMapping("/{id}/enviar-correo")
    public ResponseEntity<String> enviarCorreo(@PathVariable Long id, @RequestParam String email) {
        try {
            Venta venta = service.buscarPorId(id);
            if(venta == null) return ResponseEntity.notFound().build();

            correoService.enviarTicketPorEmail(email, venta);
            return ResponseEntity.ok("Correo enviado");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al enviar el correo");
        }
    }
}