package com.nakel.backend.controller;

import com.nakel.backend.model.ConfiguracionDivisa;
import com.nakel.backend.repository.ConfiguracionDivisaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Endpoint que la pestaña "Divisas" del frontend ya llamaba y que no existía (daba 404).
@RestController
@RequestMapping("/api/configuracion")
@CrossOrigin(origins = "*")
public class ConfiguracionController {

    private static final Long ID_UNICO = 1L;

    private final ConfiguracionDivisaRepository repository;

    public ConfiguracionController(ConfiguracionDivisaRepository repository) {
        this.repository = repository;
    }

    // 📥 Devuelve los ajustes guardados (o los valores por defecto si nunca se guardó nada)
    @GetMapping("/divisa")
    public ResponseEntity<ConfiguracionDivisa> obtenerDivisa() {
        return ResponseEntity.ok(repository.findById(ID_UNICO).orElseGet(ConfiguracionDivisa::new));
    }

    // 📤 Guarda los ajustes
    @PutMapping("/divisa")
    public ResponseEntity<?> guardarDivisa(@RequestBody ConfiguracionDivisa ajustes) {
        if (ajustes.getCotizacionUsd() == null || ajustes.getCotizacionUsd() <= 0) {
            return ResponseEntity.badRequest().body("La cotización debe ser un número mayor a 0.");
        }
        ajustes.setId(ID_UNICO);
        return ResponseEntity.ok(repository.save(ajustes));
    }
}
