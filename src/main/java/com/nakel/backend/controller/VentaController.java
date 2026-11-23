package com.nakel.backend.controller;

import com.nakel.backend.model.Venta;
import com.nakel.backend.service.VentaService;
import com.nakel.backend.service.CorreoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<?> cobrarVenta(@RequestBody Venta venta, @RequestParam(required = false) String username) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.procesarYGuardarVenta(venta, username));
        } catch (IllegalArgumentException e) {
            // Errores de negocio (sin stock, artículo dado de baja, etc.): mensaje claro para la pantalla
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("No se pudo registrar la venta. No se descontó stock ni se cobró nada.");
        }
    }

    // 🔥 Historial con filtros (más nuevas primero si el front no pide otro orden)
    @GetMapping("/historial")
    public ResponseEntity<Page<Venta>> obtenerHistorialVentas(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false, defaultValue = "Nro. Comprobante") String criterio,
            @RequestParam(required = false, defaultValue = "0") int mes,
            @RequestParam(required = false) Integer anio,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(service.obtenerHistorialConFiltros(buscar, criterio, mes, anio, pageable));
    }

    @GetMapping("/historial/total")
    public ResponseEntity<Double> obtenerTotalGlobal(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false, defaultValue = "Nro. Comprobante") String criterio,
            @RequestParam(required = false, defaultValue = "0") int mes,
            @RequestParam(required = false) Integer anio) {

        return ResponseEntity.ok(service.obtenerTotalGlobal(buscar, criterio, mes, anio));
    }

    @PostMapping("/{id}/enviar-correo")
    public ResponseEntity<String> enviarCorreo(@PathVariable Long id, @RequestParam String email) {
        try {
            Venta venta = service.buscarPorId(id);
            if(venta == null) return ResponseEntity.notFound().build();

            correoService.enviarTicketPorEmail(email, venta);
            return ResponseEntity.ok("Correo enviado");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al enviar el correo: revisá la conexión a internet y la configuración del mail.");
        }
    }
}
