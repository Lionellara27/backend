package com.nakel.backend.service;

import com.nakel.backend.model.CajaDiaria;
import com.nakel.backend.repository.CajaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    @Autowired
    private CajaRepository cajaRepository;

    // 🟢 Obtener la caja activa o crear una nueva si no hay ninguna abierta hoy
    public CajaDiaria obtenerOCrearCajaActual() {
        Optional<CajaDiaria> cajaAbierta = cajaRepository.findFirstByEstadoOrderByFechaAperturaDesc("ABIERTA");

        if (cajaAbierta.isPresent()) {
            CajaDiaria caja = cajaAbierta.get();
            // Verificar si es del mismo día
            if (caja.getFechaApertura().toLocalDate().equals(LocalDateTime.now().toLocalDate())) {
                return caja;
            }
        }

        // Si no hay caja abierta hoy, creamos una nueva
        CajaDiaria nuevaCaja = new CajaDiaria();
        nuevaCaja.setFechaApertura(LocalDateTime.now());
        nuevaCaja.setEstado("ABIERTA");
        nuevaCaja.setSaldoInicial(BigDecimal.ZERO); // 🔥 Adaptado al nuevo modelo
        nuevaCaja.setSaldoFinal(BigDecimal.ZERO);
        return cajaRepository.save(nuevaCaja);
    }

    // 🔴 Cerrar la caja activa
    public CajaDiaria cerrarCajaActual() throws Exception {
        CajaDiaria caja = cajaRepository.findFirstByEstadoOrderByFechaAperturaDesc("ABIERTA")
                .orElseThrow(() -> new Exception("No hay ninguna caja abierta para cerrar."));

        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado("CERRADA");

        // 🔥 Aseguramos que el Saldo Final sea exacto al momento de cerrar (Saldo Inicial + Total Ventas)
        BigDecimal totalCalculado = caja.getSaldoInicial().add(caja.getTotalVentas());
        caja.setSaldoFinal(totalCalculado);

        return cajaRepository.save(caja);
    }

    // 📊 Traer todas las cajas para el Historial (Abiertas y Cerradas)
    public List<CajaDiaria> obtenerHistorialCajas() {
        return cajaRepository.findAllByOrderByFechaAperturaDesc();
    }

    // 💵 Método para sumar una venta a la caja activa
    public void acumularVentaEnCajaActual(BigDecimal totalVenta, String medioPago) {
        try {
            CajaDiaria caja = obtenerOCrearCajaActual();

            // 1. Sumar a la cantidad de ventas y al total general
            caja.setCantidadVentas(caja.getCantidadVentas() + 1);
            caja.setTotalVentas(caja.getTotalVentas().add(totalVenta)); // 🔥 Adaptado al nuevo modelo

            // 2. 🔥 DESGLOSE INTELIGENTE: Repartir la plata según cómo pagaron
            if (medioPago != null) {
                if (medioPago.equalsIgnoreCase("Efectivo")) {
                    caja.setTotalEfectivo(caja.getTotalEfectivo().add(totalVenta));
                } else if (medioPago.equalsIgnoreCase("MercadoPago") || medioPago.equalsIgnoreCase("Mercado Pago")) {
                    caja.setTotalMercadoPago(caja.getTotalMercadoPago().add(totalVenta));
                } else if (medioPago.equalsIgnoreCase("Transferencia")) {
                    caja.setTotalTransferencias(caja.getTotalTransferencias().add(totalVenta));
                }
            }

            // 3. Actualizar el saldo final al vuelo
            caja.setSaldoFinal(caja.getSaldoInicial().add(caja.getTotalVentas()));

            cajaRepository.save(caja);
        } catch (Exception e) {
            System.out.println("❌ Error al actualizar totales de caja: " + e.getMessage());
        }
    }
}