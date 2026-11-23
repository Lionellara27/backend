package com.nakel.backend.controller;

import com.nakel.backend.model.ConfiguracionDivisa;
import com.nakel.backend.repository.ConfiguracionDivisaRepository;
import com.nakel.backend.service.AumentoPreciosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Pestaña "Precios y Dólar" de Configuración.
@RestController
@RequestMapping("/api/configuracion")
@CrossOrigin(origins = "*")
public class ConfiguracionController {

    private static final Long ID_UNICO = 1L;
    private static final Set<String> TIPOS_DOLAR = Set.of("oficial", "blue", "tarjeta", "bolsa");
    // Servicio público y gratuito de cotizaciones de Argentina. No se le manda ningún dato del local.
    private static final String FUENTE_COTIZACION = "https://dolarapi.com/v1/dolares/";

    private final ConfiguracionDivisaRepository repository;
    private final AumentoPreciosService aumentoPreciosService;

    public ConfiguracionController(ConfiguracionDivisaRepository repository,
                                   AumentoPreciosService aumentoPreciosService) {
        this.repository = repository;
        this.aumentoPreciosService = aumentoPreciosService;
    }

    // 📥 Devuelve los ajustes guardados (o los valores por defecto si nunca se guardó nada)
    @GetMapping("/divisa")
    public ResponseEntity<ConfiguracionDivisa> obtenerDivisa() {
        ConfiguracionDivisa config = repository.findById(ID_UNICO).orElseGet(ConfiguracionDivisa::new);
        completar(config);
        return ResponseEntity.ok(config);
    }

    // 📤 Guarda la cotización de hoy y el tipo de dólar.
    //    El "dólar con el que están calculados los precios" NO se cambia acá: solo cambia cuando
    //    se actualizan los precios según el dólar (o al deshacer esa actualización).
    @PutMapping("/divisa")
    public ResponseEntity<?> guardarDivisa(@RequestBody ConfiguracionDivisa ajustes) {
        if (ajustes.getCotizacionUsd() == null || ajustes.getCotizacionUsd() <= 0) {
            return ResponseEntity.badRequest().body("La cotización debe ser un número mayor a 0.");
        }

        boolean primeraVez = false;
        ConfiguracionDivisa guardada = repository.findById(ID_UNICO).orElse(null);
        if (guardada == null) {
            // Primera vez que se guarda: los precios se dan por calculados con esta cotización
            guardada = new ConfiguracionDivisa();
            guardada.setCotizacionBasePrecios(ajustes.getCotizacionUsd());
            primeraVez = true;
        } else if (guardada.getCotizacionBasePrecios() == null) {
            // Base vieja sin el dato: los precios estaban calculados con la cotización que había
            guardada.setCotizacionBasePrecios(guardada.getCotizacionUsd());
        }

        guardada.setId(ID_UNICO);
        guardada.setCotizacionUsd(ajustes.getCotizacionUsd());
        guardada.setModoAutomatico(false);
        if (ajustes.getTipoDolar() != null && TIPOS_DOLAR.contains(ajustes.getTipoDolar().trim().toLowerCase())) {
            guardada.setTipoDolar(ajustes.getTipoDolar().trim().toLowerCase());
        }
        if (primeraVez) {
            // El punto de partida es este dólar, de hoy
            guardada.setTipoDolarBase(guardada.getTipoDolar());
            guardada.setFechaBasePrecios(AumentoPreciosService.hoy());
        }
        completar(guardada);
        return ResponseEntity.ok(repository.save(guardada));
    }

    // 🌐 Consulta en internet a cuánto está el dólar (lo pide la pantalla solo si la usuaria acepta).
    //    Devuelve el valor de VENTA. Si no hay internet responde 503 con un mensaje claro.
    @GetMapping("/divisa/cotizacion-online")
    public ResponseEntity<?> cotizacionOnline(@RequestParam(defaultValue = "blue") String tipo) {
        String elegido = tipo.trim().toLowerCase();
        if (!TIPOS_DOLAR.contains(elegido)) {
            return ResponseEntity.badRequest().body("Tipo de dólar desconocido: " + tipo);
        }
        try {
            HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(FUENTE_COTIZACION + elegido))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("La página de cotizaciones no respondió. Probá más tarde o cargá el valor a mano.");
            }

            // La respuesta es un JSON chico: {"compra": 1655, "venta": 1675, "fechaActualizacion": "..."}
            double venta = numeroDe(respuesta.body(), "venta");
            if (venta <= 0) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("La página de cotizaciones no informó un valor. Cargá el valor a mano.");
            }

            Map<String, Object> salida = new LinkedHashMap<>();
            salida.put("tipo", elegido);
            salida.put("nombre", AumentoPreciosService.nombreTipo(elegido));
            salida.put("compra", numeroDe(respuesta.body(), "compra"));
            salida.put("venta", venta);
            salida.put("fuente", "dolarapi.com");
            return ResponseEntity.ok(salida);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Se interrumpió la consulta.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No se pudo consultar la cotización. Revisá la conexión a internet o cargá el valor a mano.");
        }
    }

    // 💵 Lleva todos los precios al dólar de hoy (queda en el historial y se puede deshacer 7 días)
    @PostMapping("/divisa/actualizar-precios")
    public ResponseEntity<?> actualizarPreciosSegunDolar() {
        try {
            return ResponseEntity.ok(aumentoPreciosService.actualizarSegunDolar());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Lee un número de un JSON plano: "campo": 1675.5 */
    private double numeroDe(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)").matcher(json == null ? "" : json);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }

    private void completar(ConfiguracionDivisa config) {
        if (config.getTipoDolar() == null || config.getTipoDolar().isBlank()) config.setTipoDolar("blue");
        if (config.getCotizacionBasePrecios() == null) config.setCotizacionBasePrecios(config.getCotizacionUsd());
        // Bases anteriores a este dato: se asume el tipo que se sigue hoy (la fecha queda sin informar)
        if (config.getTipoDolarBase() == null || config.getTipoDolarBase().isBlank()) config.setTipoDolarBase(config.getTipoDolar());
    }
}
