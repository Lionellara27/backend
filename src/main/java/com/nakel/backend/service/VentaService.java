package com.nakel.backend.service;

import com.nakel.backend.model.Venta;
import com.nakel.backend.repository.ClienteRepository;
import com.nakel.backend.repository.VentaRepository;
import com.nakel.backend.repository.ArticuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ArticuloRepository articuloRepository;
    private final CajaService cajaService;

    @Autowired
    public VentaService(VentaRepository ventaRepository, ClienteRepository clienteRepository, ArticuloRepository articuloRepository, CajaService cajaService) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.articuloRepository = articuloRepository;
        this.cajaService = cajaService;
    }

    // 🔥 1. ACÁ ESTÁ EL MÉTODO QUE FALTABA (Para que el correo encuentre la venta)
    public Venta buscarPorId(Long id) {
        return ventaRepository.findById(id).orElse(null);
    }

    @Transactional
    public Venta procesarYGuardarVenta(Venta venta, String username) {

        // 0. EL CONTROL POLICIAL DEL CLIENTE
        if (venta.getCliente() != null && venta.getCliente().getCuit() != null) {
            String cuitBuscado = venta.getCliente().getCuit();
            com.nakel.backend.model.Cliente clienteReal = clienteRepository.findByCuit(cuitBuscado)
                    .orElseThrow(() -> new RuntimeException("Error: No se encontró el cliente con CUIT " + cuitBuscado));
            venta.setCliente(clienteReal);
        } else {
            venta.setCliente(null);
        }

        // 1. Enlazamos los renglones y los pagos a la venta padre
        if (venta.getDetalles() != null) {
            venta.getDetalles().forEach(detalle -> detalle.setVenta(venta));
        }
        if (venta.getPagos() != null) {
            venta.getPagos().forEach(pago -> pago.setVenta(venta));
        }

        // 🔥 2. EL ESCUDO DE PRESUPUESTOS (Nuestra regla de oro)
        if ("Presupuesto".equalsIgnoreCase(venta.getTipoComprobante())) {
            System.out.println("🛡️ Guardando Presupuesto: No se descuenta stock ni ingresa a la caja.");
            venta.setEsFiscal(false);
            return ventaRepository.save(venta);
        }

        // 🟢 DE ACÁ PARA ABAJO PASAN SOLO LAS VENTAS REALES 🟢

        // 3. LA LÓGICA DE NEGOCIO (AFIP vs BARRANÍ)
        boolean debeSerFiscal = false;
        if (venta.getPagos() != null) {
            for (var pago : venta.getPagos()) {
                if (!pago.getMetodoPago().equalsIgnoreCase("Efectivo")) {
                    debeSerFiscal = true;
                    break;
                }
            }
        }
        venta.setEsFiscal(debeSerFiscal);

        // 4. LA LÓGICA DE STOCK
        if (venta.getDetalles() != null) {
            for (var detalle : venta.getDetalles()) {
                var articuloEnBd = articuloRepository.findById(detalle.getArticulo().getId())
                        .orElseThrow(() -> new RuntimeException("Artículo no encontrado con ID: " + detalle.getArticulo().getId()));

                articuloEnBd.setStockActual(articuloEnBd.getStockActual() - detalle.getCantidad());
                articuloRepository.save(articuloEnBd);
            }
        }

        // 5. Guardamos la Venta final en la Base de Datos
        Venta ventaGuardada = ventaRepository.save(venta);

        // 6. ¡LE MANDAMOS LA PLATA A LA CAJA DEL USUARIO!
        cajaService.acumularVentaEnCajaActual(ventaGuardada, username);

        return ventaGuardada;
    }

    // 🔥 ACTUALIZADO: Ahora recibe el "criterio" y se lo pasa al Repositorio
    @Transactional(readOnly = true)
    public Page<Venta> obtenerHistorialConFiltros(String buscar, String criterio, int mes, Pageable pageable) {
        String mesStr = (mes == 0) ? "00" : String.format("%02d", mes);
        return ventaRepository.buscarConFiltros(buscar, criterio, mesStr, pageable);
    }

    // 🔥 ACTUALIZADO: Ahora recibe el "criterio" para calcular la plata exacta
    @Transactional(readOnly = true)
    public Double obtenerTotalGlobal(String buscar, String criterio, int mes) {
        String mesStr = (mes == 0) ? "00" : String.format("%02d", mes);
        Double total = ventaRepository.obtenerTotalGlobal(buscar, criterio, mesStr);
        return total != null ? total : 0.0;
    }
}