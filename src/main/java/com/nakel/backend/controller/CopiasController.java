package com.nakel.backend.controller;

import com.nakel.backend.copias.CopiasDeSeguridad;
import com.nakel.backend.copias.CopiasDeSeguridad.Copia;
import com.nakel.backend.copias.CopiasDeSeguridad.CopiaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copias de seguridad, para la pestaña de Configuración:
 * ver cuáles hay, hacer una ahora y volver la base a como estaba un día.
 */
@RestController
@RequestMapping("/api/copias")
public class CopiasController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CopiasDeSeguridad copias;

    public CopiasController(CopiasDeSeguridad copias) {
        this.copias = copias;
    }

    @GetMapping
    public Map<String, Object> listar() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("hechaHoy", copias.copiaDeHoyHecha());
        respuesta.put("aviso", copias.avisoBaseSospechosa());
        respuesta.put("carpeta", copias.getCarpeta().toString());
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Copia copia : copias.listar()) filas.add(fila(copia));
        respuesta.put("copias", filas);
        return respuesta;
    }

    @PostMapping("/ahora")
    public ResponseEntity<?> hacerAhora() {
        try {
            return ResponseEntity.ok(fila(copias.hacerCopiaAhora()));
        } catch (CopiaException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{dia}/restaurar")
    public ResponseEntity<?> restaurar(@PathVariable String dia) {
        try {
            copias.restaurar(dia);
            return ResponseEntity.ok(Map.of("restaurada", dia.toUpperCase()));
        } catch (CopiaException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private static Map<String, Object> fila(Copia copia) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("dia", copia.dia());
        fila.put("nombre", copia.nombre());
        fila.put("fecha", copia.fecha().format(FECHA));
        fila.put("hora", copia.fecha().format(HORA));
        fila.put("deHoy", copia.deHoy());
        fila.put("esRespaldo", CopiasDeSeguridad.RESPALDO.equals(copia.dia()));
        return fila;
    }
}
