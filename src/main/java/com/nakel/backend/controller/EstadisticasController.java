package com.nakel.backend.controller;

import com.nakel.backend.dto.DashboardDTO;
import com.nakel.backend.service.EstadisticasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    @Autowired
    private EstadisticasService estadisticasService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        DashboardDTO dashboard = estadisticasService.obtenerDatosDashboard();
        return ResponseEntity.ok(dashboard);
    }

    @Autowired
    private com.nakel.backend.service.EstadisticasResumenService resumenService;

    // Todo el módulo de Estadísticas en una sola respuesta, para el período elegido:
    // HOY, SEMANA (últimos 7 días), MES, MES_PASADO o ANIO.
    @GetMapping("/resumen")
    public ResponseEntity<java.util.Map<String, Object>> getResumen(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "MES") String periodo,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer anio) {
        // anio: solo se usa con periodo=ANIO (ej: 2026). Sin él, toma el año en curso.
        return ResponseEntity.ok(resumenService.resumen(periodo, anio));
    }
}
