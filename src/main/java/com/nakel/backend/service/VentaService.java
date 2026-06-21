package com.nakel.backend.service;

import com.nakel.backend.model.Venta;
import com.nakel.backend.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    public Venta procesarYGuardarVenta(Venta venta) {

        // 1. Enlazamos los renglones y los pagos a la venta padre (Lo que antes estaba en el Controller)
        if (venta.getDetalles() != null) {
            venta.getDetalles().forEach(detalle -> detalle.setVenta(venta));
        }
        if (venta.getPagos() != null) {
            venta.getPagos().forEach(pago -> pago.setVenta(venta));
        }

        // 2. 🔥 LA LÓGICA DE NEGOCIO (AFIP vs BARRANÍ) 🔥
        // Por defecto asumimos que es en negro (Efectivo)
        boolean debeSerFiscal = false;

        // Recorremos los pagos. Si encuentra AL MENOS UN PAGO que NO sea Efectivo, salta la ficha.
        if (venta.getPagos() != null) {
            for (var pago : venta.getPagos()) {
                if (!pago.getMetodoPago().equalsIgnoreCase("Efectivo")) {
                    debeSerFiscal = true;
                    break; // Cortamos el ciclo, ya sabemos que hay que facturar
                }
            }
        }

        // Le clavamos la decisión a la venta antes de guardarla
        venta.setEsFiscal(debeSerFiscal);

        // 3. Guardamos en la Base de Datos
        return ventaRepository.save(venta);
    }
}