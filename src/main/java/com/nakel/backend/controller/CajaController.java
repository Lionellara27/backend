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
    public ResponseEntity<Page<CajaDiaria>> obtenerHistorial(
            // Filtros opcionales: mes (1-12, 0 = todos) y año (0 = todos)
            @RequestParam(required = false, defaultValue = "0") int mes,
            @RequestParam(required = false, defaultValue = "0") int anio,
            // Las cajas más nuevas primero salvo que la pantalla pida otro orden (?sort=totalVentas,desc)
            @org.springframework.data.web.PageableDefault(size = 20, sort = "fechaApertura",
                    direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        System.out.println("🔍 [BACKEND-CAJAS-DEBUG] Petición recibida en /api/caja/historial");
        System.out.println("   -> Página solicitada: " + pageable.getPageNumber() + " | Tamaño: " + pageable.getPageSize());

        java.time.LocalDate[] rango = rangoDeFechas(mes, anio);
        Page<CajaDiaria> paginaCajas = cajaService.obtenerHistorialPaginado(rango[0], rango[1], pageable);

        System.out.println("   -> Total de cajas encontradas en la BD: " + paginaCajas.getTotalElements());
        System.out.println("   -> Total de páginas calculadas: " + paginaCajas.getTotalPages());
        System.out.println("   -> Cajas en esta página actual: " + paginaCajas.getNumberOfElements());

        return ResponseEntity.ok(paginaCajas);
    }

    // Suma de ingresos de TODAS las cajas del período elegido (no solo de la página que se ve)
    @GetMapping("/historial/total")
    public ResponseEntity<java.math.BigDecimal> obtenerTotalHistorial(
            @RequestParam(required = false, defaultValue = "0") int mes,
            @RequestParam(required = false, defaultValue = "0") int anio) {
        java.time.LocalDate[] rango = rangoDeFechas(mes, anio);
        return ResponseEntity.ok(cajaService.sumarIngresos(rango[0], rango[1]));
    }

    // Convierte mes/año en un rango [desde, hasta]. Los dos null = sin filtro de fecha.
    private java.time.LocalDate[] rangoDeFechas(int mes, int anio) {
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate desde = null;
        java.time.LocalDate hasta = null;
        if (mes >= 1 && mes <= 12) {
            // Solo mes: se entiende el más reciente (en enero, "Diciembre" es el del año pasado)
            int anioReal = anio > 0 ? anio : (mes > hoy.getMonthValue() ? hoy.getYear() - 1 : hoy.getYear());
            desde = java.time.LocalDate.of(anioReal, mes, 1);
            hasta = desde.plusMonths(1).minusDays(1);
        } else if (anio > 0) {
            desde = java.time.LocalDate.of(anio, 1, 1);
            hasta = java.time.LocalDate.of(anio, 12, 31);
        }
        return new java.time.LocalDate[]{desde, hasta};
    }
}