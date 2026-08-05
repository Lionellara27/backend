package com.nakel.backend.service;

import com.nakel.backend.dto.ArticuloInfoDTO; // Asegurate que el paquete coincida con donde creaste el DTO
import com.nakel.backend.model.Cambio;
import com.nakel.backend.model.DetalleVenta;
import com.nakel.backend.model.Venta;
import com.nakel.backend.repository.CambioRepository;
import com.nakel.backend.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CambioService {

    private final CambioRepository cambioRepository;
    private final VentaRepository ventaRepository;

    @Autowired
    public CambioService(CambioRepository cambioRepository, VentaRepository ventaRepository) {
        this.cambioRepository = cambioRepository;
        this.ventaRepository = ventaRepository;
    }

    @Transactional
    public Cambio registrarCambio(Long idVentaOriginal, Cambio nuevoCambio) {
        // 1. Buscamos el ticket original
        Venta venta = ventaRepository.findById(idVentaOriginal)
                .orElseThrow(() -> new RuntimeException("Venta original no encontrada con ID: " + idVentaOriginal));

        // 🔥 ARREGLO 1: Límite de 2 cambios máximos
        List<Cambio> historialActual = cambioRepository.findByVentaOriginalId(idVentaOriginal);
        if (historialActual.size() >= 2) {
            throw new RuntimeException("El sistema ha bloqueado esta venta: se alcanzó el límite máximo de 2 cambios permitidos.");
        }

        // 2. Le asignamos la venta al cambio para que queden vinculados
        nuevoCambio.setVentaOriginal(venta);

        // 🔥 ARREGLO 2: Solución al error de Hibernate (not-null property)
        // Y ARREGLO 3: Frenar el glitch de devoluciones infinitas (Mate Pampa)
        if (nuevoCambio.getItemsDevueltos() != null) {
            for (var itemDevuelto : nuevoCambio.getItemsDevueltos()) {

                // A) Vinculamos el item devuelto con su Cambio padre (Soluciona el error de consola)
                itemDevuelto.setCambio(nuevoCambio);

                // B) Buscamos el detalle original en la venta y le sumamos la cantidad devuelta
                for (DetalleVenta detalle : venta.getDetalles()) {
                    if (detalle.getArticulo().getId().equals(itemDevuelto.getArticulo().getId())) {
                        // Nos aseguramos de que no sea null
                        int actual = detalle.getCantidadDevuelta() != null ? detalle.getCantidadDevuelta() : 0;
                        detalle.setCantidadDevuelta(actual + itemDevuelto.getCantidad());
                        break;
                    }
                }
            }
        }

        // 🔥 ARREGLO 2 (Continuación): Vinculamos también los items nuevos al Cambio
        if (nuevoCambio.getItemsNuevos() != null) {
            for (var itemNuevo : nuevoCambio.getItemsNuevos()) {
                itemNuevo.setCambio(nuevoCambio);
            }
        }

        // 3. Guardamos el cambio (Al tener @Transactional, los cambios en 'venta' se guardan solos)
        return cambioRepository.save(nuevoCambio);
    }

    public List<Cambio> obtenerHistorialPorVenta(Long idVentaOriginal) {
        return cambioRepository.findByVentaOriginalId(idVentaOriginal);
    }

    // 🔥 EL NUEVO MÉTODO: Calcula el "Ticket Vivo" restando devoluciones y sumando nuevos artículos
    public List<ArticuloInfoDTO> obtenerArticulosActualesDeVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + ventaId));

        // Mapa para llevar la cuenta: ID de Artículo -> Estado actual en poder del cliente
        Map<Long, ArticuloInfoDTO> balanceArticulos = new HashMap<>();

        // 1. Cargamos la compra original
        if (venta.getDetalles() != null) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                Long artId = detalle.getArticulo().getId();
                balanceArticulos.put(artId, new ArticuloInfoDTO(
                        detalle.getArticulo(),
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario().doubleValue()
                ));
            }
        }

        // 2. Buscamos el historial de cambios de esta venta
        List<Cambio> historialCambios = cambioRepository.findByVentaOriginalId(ventaId);

        // 3. Aplicamos la historia (Restamos devoluciones, sumamos nuevos artículos)
        for (Cambio cambio : historialCambios) {

            // A) Restamos lo que devolvió
            if (cambio.getItemsDevueltos() != null) {
                for (var devuelto : cambio.getItemsDevueltos()) {
                    Long artId = devuelto.getArticulo().getId();
                    if (balanceArticulos.containsKey(artId)) {
                        int nuevaCantidad = balanceArticulos.get(artId).getCantidad() - devuelto.getCantidad();
                        if (nuevaCantidad <= 0) {
                            balanceArticulos.remove(artId); // Ya no le queda de este artículo
                        } else {
                            balanceArticulos.get(artId).setCantidad(nuevaCantidad);
                        }
                    }
                }
            }

            // B) Sumamos lo que se llevó nuevo
            if (cambio.getItemsNuevos() != null) {
                for (var nuevo : cambio.getItemsNuevos()) {
                    Long artId = nuevo.getArticulo().getId();
                    balanceArticulos.compute(artId, (id, infoExistente) -> {
                        if (infoExistente != null) {
                            infoExistente.setCantidad(infoExistente.getCantidad() + nuevo.getCantidad());
                            return infoExistente;
                        } else {
                            return new ArticuloInfoDTO(nuevo.getArticulo(), nuevo.getCantidad(), nuevo.getPrecioUnitario());
                        }
                    });
                }
            }
        }

        // Retornamos la lista final limpia con lo que el cliente tiene hoy en su poder
        return new ArrayList<>(balanceArticulos.values());
    }
}