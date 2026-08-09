package com.nakel.backend.controller;

import com.nakel.backend.model.Venta;
import com.nakel.backend.service.VentaService;
import com.nakel.backend.service.CorreoService; // 🔥 Importamos el servicio de correos
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class VentaController {

    private final VentaService service;
    private final CorreoService correoService; // 🔥 Declaramos que vamos a usar el servicio de correos

    // 🔥 Actualizamos el constructor para que Spring Boot nos inyecte AMBOS servicios
    public VentaController(VentaService service, CorreoService correoService) {
        this.service = service;
        this.correoService = correoService;
    }

    @PostMapping
    public ResponseEntity<Venta> cobrarVenta(@RequestBody Venta venta, @RequestParam String username) {
        // Le pasamos la venta Y el username al servicio
        return ResponseEntity.status(HttpStatus.CREATED).body(service.procesarYGuardarVenta(venta, username));
    }

    // 🔥 CAMBIO EXACTO: Le agregamos "/historial" a la ruta para que el Frontend lo encuentre
    @GetMapping("/historial")
    public ResponseEntity<org.springframework.data.domain.Page<Venta>> obtenerHistorialVentas(org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(service.obtenerTodasLasVentas(pageable));
    }

    @PostMapping("/{id}/enviar-correo")
    public ResponseEntity<String> enviarCorreo(@PathVariable Long id, @RequestParam String email) {
        try {
            // 🔥 Usamos "service" en lugar de "ventaService" porque así se llama la variable arriba
            Venta venta = service.buscarPorId(id);

            if(venta == null) return ResponseEntity.notFound().build();

            // 🔥 Ahora correoService ya existe y Spring Boot sabe qué es
            correoService.enviarTicketPorEmail(email, venta);
            return ResponseEntity.ok("Correo enviado");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al enviar el correo");
        }
    }
}