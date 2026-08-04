package com.nakel.backend.service;

import com.nakel.backend.model.Vale;
import com.nakel.backend.repository.ValeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        Vale nuevoVale = new Vale();

        // Generamos un código único corto, ej: "VALE-4F9A2"
        String codigoUnico = "VALE-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        nuevoVale.setCodigo(codigoUnico);
        nuevoVale.setMonto(monto);

        // 🔥 Guardamos el cliente asociado (si viene null, queda como null)
        nuevoVale.setIdCliente(idCliente);

        return valeRepository.save(nuevoVale);
    }

    public Vale validarVale(String codigo) {
        Vale vale = valeRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Vale no encontrado."));

        if ("USADO".equals(vale.getEstado())) {
            throw new RuntimeException("Este vale ya fue utilizado.");
        }

        // Si la fecha de hoy es "después" de la fecha de vencimiento
        if (LocalDateTime.now().isAfter(vale.getFechaVencimiento()) && !"VENCIDO".equals(vale.getEstado())) {
            vale.setEstado("VENCIDO");
            valeRepository.save(vale);
            throw new RuntimeException("El vale está vencido.");
        }

        return vale;
    }

    @Transactional
    public Vale consumirVale(String codigo) {
        Vale vale = validarVale(codigo); // Reutilizamos la validación
        vale.setEstado("USADO"); // Lo "quemamos"
        return valeRepository.save(vale);
    }
}