package com.nakel.backend.controller;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.model.TipoMovimientoCaja;
import com.nakel.backend.model.Vale;
import com.nakel.backend.repository.ClienteRepository;
import com.nakel.backend.service.CajaService;
import com.nakel.backend.service.ValeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vales")
public class ValeController {

    private final ValeService valeService;
    private final CajaService cajaService;
    private final ClienteRepository clienteRepository;

    @Autowired
    public ValeController(ValeService valeService, CajaService cajaService, ClienteRepository clienteRepository) {
        this.valeService = valeService;
        this.cajaService = cajaService;
        this.clienteRepository = clienteRepository;
    }

    private static String plata(BigDecimal monto) {
        return "$" + monto.stripTrailingZeros().toPlainString();
    }

    // 0. Listado para la pantalla de Vales: cada vale con el nombre de su cliente y su estado real
    @GetMapping
    public Page<Map<String, Object>> listar(
            @RequestParam(required = false, defaultValue = "true") boolean soloVigentes,
            @PageableDefault(size = 20) Pageable pageable) {

        LocalDateTime ahora = LocalDateTime.now();
        return valeService.listar(soloVigentes, pageable).map(vale -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", vale.getId());
            fila.put("codigo", vale.getCodigo());
            fila.put("monto", vale.getMonto());
            fila.put("fechaEmision", vale.getFechaEmision());
            fila.put("fechaVencimiento", vale.getFechaVencimiento());
            // Un vale ACTIVO cuya fecha ya pasó se muestra como VENCIDO aunque nadie lo haya consultado
            boolean vencido = "ACTIVO".equals(vale.getEstado()) && vale.getFechaVencimiento() != null
                    && ahora.isAfter(vale.getFechaVencimiento());
            fila.put("estado", vencido ? "VENCIDO" : vale.getEstado());
            fila.put("idCliente", vale.getIdCliente());
            fila.put("clienteNombre", vale.getIdCliente() == null
                    ? null
                    : clienteRepository.findById(vale.getIdCliente()).map(Cliente::getNombre).orElse(null));
            fila.put("motivo", vale.getMotivo());
            return fila;
        });
    }

    // 0.1. Los vales vigentes de UN cliente: el detalle de dónde sale su saldo a favor
    @GetMapping("/cliente/{idCliente}")
    public java.util.List<Vale> vigentesDeCliente(@PathVariable Long idCliente) {
        return valeService.vigentesDe(idCliente);
    }

    // 1. Genera un vale suelto: por vuelto, o desde la pantalla de Vales (atención al cliente, etc.).
    //    Los vales por CAMBIO los emite CambioService dentro de la misma operación del cambio.
    @PostMapping("/generar")
    public ResponseEntity<?> generarVale(
            @RequestParam BigDecimal monto,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String motivo) {
        try {
            if (idCliente != null && !clienteRepository.existsById(idCliente)) {
                return ResponseEntity.badRequest().body("El cliente elegido ya no existe.");
            }

            String motivoFinal = (motivo == null || motivo.isBlank()) ? "Vuelto o saldo a favor" : motivo.trim();
            Vale nuevoVale = valeService.generarVale(monto, idCliente, motivoFinal);

            // Queda asentado en la caja del día para poder rastrearlo
            cajaService.registrarMovimientoInformativo(TipoMovimientoCaja.AJUSTE,
                    "Vale emitido " + nuevoVale.getCodigo(),
                    motivoFinal, "Vale de Cambio", nuevoVale.getMonto(), username);

            return ResponseEntity.ok(nuevoVale);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Valida que el vale exista y no esté usado ni vencido
    @GetMapping("/validar/{codigo}")
    public ResponseEntity<?> validarVale(@PathVariable String codigo) {
        try {
            Vale vale = valeService.validarVale(codigo);
            return ResponseEntity.ok(vale);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 3. Descuenta lo usado del vale (todo o una parte) y lo deja asentado en la caja del día
    @PutMapping("/consumir/{codigo}")
    public ResponseEntity<?> consumirVale(@PathVariable String codigo,
                                          @RequestParam(required = false) BigDecimal montoUsado,
                                          @RequestParam(required = false) String username) {
        try {
            BigDecimal valorAntes = valeService.validarVale(codigo).getMonto();
            Vale vale = valeService.consumirVale(codigo, montoUsado);

            BigDecimal usado = (montoUsado == null || montoUsado.compareTo(valorAntes) > 0) ? valorAntes : montoUsado;
            if (usado.signum() > 0) {
                boolean quedaSaldo = "ACTIVO".equals(vale.getEstado());
                cajaService.registrarMovimientoInformativo(TipoMovimientoCaja.AJUSTE,
                        "Vale usado " + vale.getCodigo(),
                        "Se usaron " + plata(usado) + (quedaSaldo
                                ? " · le quedan " + plata(vale.getMonto())
                                : " · vale agotado"),
                        "Vale de Cambio", usado, username);
            }

            return ResponseEntity.ok(vale);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
