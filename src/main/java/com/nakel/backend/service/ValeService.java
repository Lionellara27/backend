package com.nakel.backend.service;

import com.nakel.backend.model.Vale;
import com.nakel.backend.repository.ValeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ValeService {

    private final ValeRepository valeRepository;

    @Autowired
    public ValeService(ValeRepository valeRepository) {
        this.valeRepository = valeRepository;
    }

    @Transactional
    // 🔥 Le agregamos el Long idCliente para que coincida con el Controller
    public Vale generarVale(BigDecimal monto, Long idCliente) {
        return generarVale(monto, idCliente, null);
    }

    // motivo: por qué se emite (cambio, vuelto, atención al cliente...). Queda guardado en el vale.
    @Transactional
    public Vale generarVale(BigDecimal monto, Long idCliente, String motivo) {
        if (monto == null || monto.signum() <= 0) {
            throw new RuntimeException("El monto del vale debe ser mayor a 0.");
        }

        Vale nuevoVale = new Vale();

        // Generamos un código único corto, ej: "VALE-4F9A2" (reintentamos si justo ya existe)
        String codigoUnico;
        do {
            codigoUnico = "VALE-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        } while (valeRepository.findByCodigo(codigoUnico).isPresent());

        nuevoVale.setCodigo(codigoUnico);
        nuevoVale.setMonto(monto.setScale(2, RoundingMode.HALF_UP));

        // 🔥 Guardamos el cliente asociado (si viene null, queda como null)
        nuevoVale.setIdCliente(idCliente);
        if (motivo != null && !motivo.isBlank()) {
            String limpio = motivo.trim();
            nuevoVale.setMotivo(limpio.length() > 300 ? limpio.substring(0, 300) : limpio);
        }

        return valeRepository.save(nuevoVale);
    }

    // Listado para la pantalla de Vales. soloVigentes = activos que todavía no vencieron.
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Vale> listar(boolean soloVigentes,
                                                             org.springframework.data.domain.Pageable pageable) {
        return soloVigentes
                ? valeRepository.findByEstadoAndFechaVencimientoAfterOrderByFechaEmisionDesc("ACTIVO", LocalDateTime.now(), pageable)
                : valeRepository.findAllByOrderByFechaEmisionDesc(pageable);
    }

    // Los vales vigentes de un cliente (el detalle de su saldo a favor)
    @Transactional(readOnly = true)
    public java.util.List<Vale> vigentesDe(Long idCliente) {
        return valeRepository.findByIdClienteAndEstadoAndFechaVencimientoAfterOrderByFechaEmisionAsc(
                idCliente, "ACTIVO", LocalDateTime.now());
    }

    // Saldo a favor de un cliente = suma de sus vales vigentes
    @Transactional(readOnly = true)
    public BigDecimal saldoAFavorDe(Long idCliente) {
        if (idCliente == null) return BigDecimal.ZERO;
        BigDecimal suma = valeRepository.sumarVigentesDeCliente(idCliente, LocalDateTime.now());
        return suma != null ? suma : BigDecimal.ZERO;
    }

    // noRollbackFor: si el vale está vencido queremos que quede guardado como VENCIDO
    // aunque después avisemos con una excepción.
    @Transactional(noRollbackFor = RuntimeException.class)
    public Vale validarVale(String codigo) {
        Vale vale = valeRepository.findByCodigo(codigo == null ? "" : codigo.trim())
                .orElseThrow(() -> new RuntimeException("Vale no encontrado."));

        if ("USADO".equals(vale.getEstado())) {
            throw new RuntimeException("Este vale ya fue utilizado.");
        }

        if ("VENCIDO".equals(vale.getEstado())) {
            throw new RuntimeException("El vale está vencido.");
        }

        // Si la fecha de hoy es "después" de la fecha de vencimiento
        if (LocalDateTime.now().isAfter(vale.getFechaVencimiento())) {
            vale.setEstado("VENCIDO");
            valeRepository.save(vale);
            throw new RuntimeException("El vale está vencido.");
        }

        return vale;
    }

    /**
     * Consume un vale. Si se usa solo una parte (montoUsado menor al valor del vale),
     * el vale sigue ACTIVO con el saldo que le queda, en vez de perderse entero.
     * Si montoUsado es null o cubre todo el vale, queda USADO.
     */
    @Transactional
    public Vale consumirVale(String codigo, BigDecimal montoUsado) {
        Vale vale = validarVale(codigo); // Reutilizamos la validación

        if (montoUsado != null && montoUsado.signum() <= 0) {
            return vale; // No se usó nada: el vale queda intacto
        }

        if (montoUsado != null && montoUsado.compareTo(vale.getMonto()) < 0) {
            vale.setMonto(vale.getMonto().subtract(montoUsado).setScale(2, RoundingMode.HALF_UP));
            return valeRepository.save(vale); // Sigue ACTIVO con el saldo restante
        }

        vale.setEstado("USADO"); // Lo "quemamos"
        return valeRepository.save(vale);
    }
}
