package com.nakel.backend.controller;

import com.nakel.backend.model.CajaDiaria;
import com.nakel.backend.service.CajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 🔥 CIRUGÍA APLICADA: Ahora recibe Pageable y devuelve una Page
    @GetMapping("/historial")
    public ResponseEntity<Page<CajaDiaria>> obtenerHistorial(Pageable pageable) {
        System.out.println("🔍 [BACKEND-CAJAS-DEBUG] Petición recibida en /api/caja/historial");
        System.out.println("   -> Página solicitada: " + pageable.getPageNumber() + " | Tamaño: " + pageable.getPageSize());

        Page<CajaDiaria> paginaCajas = cajaService.obtenerHistorialPaginado(pageable);

        System.out.println("   -> Total de cajas encontradas en la BD: " + paginaCajas.getTotalElements());
        System.out.println("   -> Total de páginas calculadas: " + paginaCajas.getTotalPages());
        System.out.println("   -> Cajas en esta página actual: " + paginaCajas.getNumberOfElements());

        return ResponseEntity.ok(paginaCajas);
    }
}