package com.nakel.backend.service;

import com.nakel.backend.dto.ArticuloInfoDTO; // Asegurate que el paquete coincida con donde creaste el DTO
import com.nakel.backend.model.Articulo;
import com.nakel.backend.model.Cambio;
import com.nakel.backend.model.DetalleVenta;
import com.nakel.backend.model.ItemCambio;
import com.nakel.backend.model.Vale;
import com.nakel.backend.model.Venta;
import com.nakel.backend.repository.ArticuloRepository;
import com.nakel.backend.repository.CambioRepository;
import com.nakel.backend.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CambioService {

    private final CambioRepository cambioRepository;
    private final VentaRepository ventaRepository;
    private final ArticuloRepository articuloRepository;
    private final ValeService valeService;
    private final CajaService cajaService;

    @Autowired
    public CambioService(CambioRepository cambioRepository, VentaRepository ventaRepository,
                         ArticuloRepository articuloRepository, ValeService valeService, CajaService cajaService) {
        this.cambioRepository = cambioRepository;
        this.ventaRepository = ventaRepository;
        this.articuloRepository = articuloRepository;
        this.valeService = valeService;
        this.cajaService = cajaService;
    }

    /**
     * Registra un cambio COMPLETO en una sola transacción: historial, stock, vale y caja.
     * Antes la pantalla hacía cada paso por separado (stock por un lado, vale por otro) y,
     * si algo fallaba a mitad de camino, quedaba el stock movido o un vale emitido sin cambio.
     * Ahora, si cualquier paso falla, se deshace todo.
     */
    @Transactional
    public Cambio registrarCambio(Long idVentaOriginal, Cambio nuevoCambio, String username) {
        // 1. Buscamos el ticket original
        Venta venta = ventaRepository.findById(idVentaOriginal)
                .orElseThrow(() -> new RuntimeException("Venta original no encontrada con ID: " + idVentaOriginal));

        // 🔥 Límite de 2 cambios máximos
        List<Cambio> historialActual = cambioRepository.findByVentaOriginalId(idVentaOriginal);
        if (historialActual.size() >= 2) {
            throw new RuntimeException("El sistema ha bloqueado esta venta: se alcanzó el límite máximo de 2 cambios permitidos.");
        }

        List<ItemCambio> devueltos = nuevoCambio.getItemsDevueltos();
        List<ItemCambio> nuevos = nuevoCambio.getItemsNuevos();
        if (devueltos.isEmpty() && nuevos.isEmpty()) {
            throw new RuntimeException("El cambio está vacío: no se devuelve ni se lleva ningún artículo.");
        }

        // 2. Le asignamos la venta al cambio para que queden vinculados
        nuevoCambio.setVentaOriginal(venta);

        // 3. No se puede devolver más de lo que el cliente tiene hoy en su poder
        Map<Long, Integer> enPoder = new HashMap<>();
        for (ArticuloInfoDTO info : obtenerArticulosActualesDeVenta(idVentaOriginal)) {
            enPoder.put(info.getArticulo().getId(), info.getCantidad());
        }

        // 4. DEVUELTOS: vuelven al stock (aunque el artículo esté dado de baja)
        for (ItemCambio item : devueltos) {
            Articulo articulo = cargarArticulo(item);
            if (item.getCantidad() <= 0) {
                throw new RuntimeException("Cantidad inválida para devolver: " + articulo.getNombre());
            }
            int tiene = enPoder.getOrDefault(articulo.getId(), 0);
            if (item.getCantidad() > tiene) {
                throw new RuntimeException("No se pueden devolver " + item.getCantidad() + " de " + articulo.getNombre()
                        + ": el cliente tiene " + tiene + ".");
            }
            enPoder.put(articulo.getId(), tiene - item.getCantidad());

            item.setCambio(nuevoCambio);
            item.setArticulo(articulo);
            articulo.setStockActual(articulo.getStockActual() + item.getCantidad());
            articuloRepository.save(articulo);

            // Llevamos la cuenta de lo devuelto en el renglón original de la venta
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getArticulo() != null && detalle.getArticulo().getId().equals(articulo.getId())) {
                    int actual = detalle.getCantidadDevuelta() != null ? detalle.getCantidadDevuelta() : 0;
                    detalle.setCantidadDevuelta(actual + item.getCantidad());
                    break;
                }
            }
        }

        // 5. NUEVOS: salen del stock (validando que haya)
        for (ItemCambio item : nuevos) {
            Articulo articulo = cargarArticulo(item);
            if (item.getCantidad() <= 0) {
                throw new RuntimeException("Cantidad inválida para llevar: " + articulo.getNombre());
            }
            if (!articulo.isActivo()) {
                throw new RuntimeException("El artículo " + articulo.getNombre() + " fue dado de baja y no se puede entregar.");
            }
            if (articulo.getStockActual() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente de " + articulo.getNombre()
                        + " (quedan " + articulo.getStockActual() + ").");
            }

            item.setCambio(nuevoCambio);
            item.setArticulo(articulo);
            articulo.setStockActual(articulo.getStockActual() - item.getCantidad());
            articuloRepository.save(articulo);
        }

        // 6. La diferencia la calcula el servidor (no se confía en la cuenta de la pantalla)
        BigDecimal totalDevuelto = sumar(devueltos);
        BigDecimal totalNuevo = sumar(nuevos);
        BigDecimal diferencia = totalNuevo.subtract(totalDevuelto).setScale(2, RoundingMode.HALF_UP);
        nuevoCambio.setDiferenciaCobrada(diferencia.doubleValue());

        if (diferencia.signum() < 0) {
            // Sobra plata a favor del cliente: se emite el vale acá adentro, en la misma transacción
            Long idCliente = venta.getCliente() != null ? venta.getCliente().getId() : null;
            Vale vale = valeService.generarVale(diferencia.abs(), idCliente);
            nuevoCambio.setCodigoValeGenerado(vale.getCodigo());
            nuevoCambio.setMetodoPago(null);
        } else {
            nuevoCambio.setCodigoValeGenerado(null);
        }

        // 7. Guardamos el cambio (los renglones de la venta se guardan solos por @Transactional)
        Cambio guardado = cambioRepository.save(nuevoCambio);

        // 8. Si el cliente pagó una diferencia, esa plata entra a la caja del día
        if (diferencia.signum() > 0) {
            registrarDiferenciaEnCaja(idVentaOriginal, nuevoCambio, diferencia, username);
        }

        return guardado;
    }

    private Articulo cargarArticulo(ItemCambio item) {
        if (item.getArticulo() == null || item.getArticulo().getId() == null) {
            throw new RuntimeException("Hay un renglón del cambio sin artículo.");
        }
        return articuloRepository.findById(item.getArticulo().getId())
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado con ID: " + item.getArticulo().getId()));
    }

    private BigDecimal sumar(List<ItemCambio> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCambio item : items) {
            total = total.add(BigDecimal.valueOf(item.getPrecioUnitario()).multiply(BigDecimal.valueOf(item.getCantidad())));
        }
        return total;
    }

    // Reparte la diferencia cobrada en los rubros de la caja. Lo pagado con vale no es plata nueva.
    private void registrarDiferenciaEnCaja(Long idVenta, Cambio cambio, BigDecimal diferencia, String username) {
        List<Cambio.PagoCambio> pagos = cambio.getPagos();

        if (pagos == null || pagos.isEmpty()) {
            String medio = cambio.getMetodoPago() != null ? cambio.getMetodoPago() : "Efectivo";
            cajaService.registrarIngresoPorCambio(idVenta, medio, diferencia, username);
            return;
        }

        for (Cambio.PagoCambio pago : pagos) {
            if (pago.getMonto() == null || pago.getMonto() <= 0 || pago.getMetodoPago() == null) continue;
            if (pago.getMetodoPago().toLowerCase().contains("vale")) continue;
            cajaService.registrarIngresoPorCambio(idVenta, pago.getMetodoPago(),
                    BigDecimal.valueOf(pago.getMonto()).setScale(2, RoundingMode.HALF_UP), username);
        }
    }

    @Transactional(readOnly = true)
    public List<Cambio> obtenerHistorialPorVenta(Long idVentaOriginal) {
        return cambioRepository.findByVentaOriginalId(idVentaOriginal);
    }

    // 🔥 Calcula el "Ticket Vivo" restando devoluciones y sumando nuevos artículos
    @Transactional(readOnly = true)
    public List<ArticuloInfoDTO> obtenerArticulosActualesDeVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + ventaId));

        // Mapa para llevar la cuenta: ID de Artículo -> Estado actual en poder del cliente
        Map<Long, ArticuloInfoDTO> balanceArticulos = new HashMap<>();

        // 1. Cargamos la compra original (si un artículo figura en dos renglones, se suman)
        if (venta.getDetalles() != null) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getArticulo() == null) continue;
                Long artId = detalle.getArticulo().getId();
                ArticuloInfoDTO existente = balanceArticulos.get(artId);
                if (existente != null) {
                    existente.setCantidad(existente.getCantidad() + detalle.getCantidad());
                } else {
                    balanceArticulos.put(artId, new ArticuloInfoDTO(
                            detalle.getArticulo(),
                            detalle.getCantidad(),
                            detalle.getPrecioUnitario().doubleValue()
                    ));
                }
            }
        }

        // 2. Buscamos el historial de cambios de esta venta
        List<Cambio> historialCambios = cambioRepository.findByVentaOriginalId(ventaId);

        // 3. Aplicamos la historia (Restamos devoluciones, sumamos nuevos artículos)
        for (Cambio cambio : historialCambios) {

            // A) Restamos lo que devolvió
            for (var devuelto : cambio.getItemsDevueltos()) {
                if (devuelto.getArticulo() == null) continue;
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

            // B) Sumamos lo que se llevó nuevo
            for (var nuevo : cambio.getItemsNuevos()) {
                if (nuevo.getArticulo() == null) continue;
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

        // Retornamos la lista final limpia con lo que el cliente tiene hoy en su poder
        return new ArrayList<>(balanceArticulos.values());
    }
}
