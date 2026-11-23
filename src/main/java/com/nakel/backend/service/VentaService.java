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

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    @Transactional(readOnly = true)
    public Venta buscarPorId(Long id) {
        return ventaRepository.findById(id).orElse(null);
    }

    @Transactional
    public Venta procesarYGuardarVenta(Venta venta, String username) {

        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La venta no tiene artículos.");
        }
        if (venta.getTotal() == null) {
            throw new IllegalArgumentException("La venta no tiene total.");
        }

        // 0. EL CONTROL POLICIAL DEL CLIENTE (Blindado por ID)
        if (venta.getCliente() != null && venta.getCliente().getId() != null && venta.getCliente().getId() > 0) {
            Long idBuscado = venta.getCliente().getId();

            // Lo buscamos por su ID real de la base de datos, que es infalible
            com.nakel.backend.model.Cliente clienteReal = clienteRepository.findById(idBuscado)
                    .orElse(null); // Si no lo encuentra, no explota, lo pasa a nulo (Consumidor Final)

            venta.setCliente(clienteReal);
        } else {
            // Cae acá directo si en el mostrador es una venta anónima
            venta.setCliente(null);
        }

        // 1. Enlazamos los renglones y los pagos a la venta padre
        venta.getDetalles().forEach(detalle -> {
            detalle.setVenta(venta);
            if (detalle.getCantidadDevuelta() == null) {
                detalle.setCantidadDevuelta(0);
            }
        });
        if (venta.getPagos() != null) {
            venta.getPagos().forEach(pago -> pago.setVenta(venta));
        }
        if (venta.getEsTicketCambio() == null) {
            venta.setEsTicketCambio(false);
        }

        // 🔥 2. EL ESCUDO DE PRESUPUESTOS (Nuestra regla de oro)
        if ("Presupuesto".equalsIgnoreCase(venta.getTipoComprobante())) {
            System.out.println("🛡️ Guardando Presupuesto: No se descuenta stock ni ingresa a la caja.");
            venta.setTipoComprobante("Presupuesto");
            venta.setEsFiscal(false);
            return ventaRepository.save(venta);
        }

        // 🟢 DE ACÁ PARA ABAJO PASAN SOLO LAS VENTAS REALES 🟢

        // 3. LA LÓGICA DE NEGOCIO (AFIP vs BARRANÍ)
        boolean debeSerFiscal = false;
        if (venta.getPagos() != null) {
            for (var pago : venta.getPagos()) {
                if (pago.getMetodoPago() != null && !pago.getMetodoPago().equalsIgnoreCase("Efectivo")) {
                    debeSerFiscal = true;
                    break;
                }
            }
        }
        venta.setEsFiscal(debeSerFiscal);

        // 4. LA LÓGICA DE STOCK (ahora valida: no se puede vender lo que no hay)
        for (var detalle : venta.getDetalles()) {
            if (detalle.getArticulo() == null || detalle.getArticulo().getId() == null) {
                throw new IllegalArgumentException("Hay un renglón de la venta sin artículo.");
            }
            if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException("Hay un renglón de la venta con cantidad inválida.");
            }

            var articuloEnBd = articuloRepository.findById(detalle.getArticulo().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Artículo no encontrado con ID: " + detalle.getArticulo().getId()));

            if (!articuloEnBd.isActivo()) {
                throw new IllegalArgumentException("El artículo " + articuloEnBd.getNombre() + " fue dado de baja y no se puede vender.");
            }
            if (articuloEnBd.getStockActual() < detalle.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente de " + articuloEnBd.getNombre()
                        + ": quedan " + articuloEnBd.getStockActual() + " y se quieren vender " + detalle.getCantidad() + ".");
            }

            articuloEnBd.setStockActual(articuloEnBd.getStockActual() - detalle.getCantidad());
            articuloRepository.save(articuloEnBd);
            detalle.setArticulo(articuloEnBd);
        }

        // 5. Guardamos la Venta final en la Base de Datos
        Venta ventaGuardada = ventaRepository.save(venta);

        // 6. ¡LE MANDAMOS LA PLATA A LA CAJA DEL USUARIO!
        cajaService.acumularVentaEnCajaActual(ventaGuardada, username);

        return ventaGuardada;
    }

    // Convierte el mes elegido en un rango [desde, hasta).
    // Si el mes todavía no llegó este año, se entiende el del año pasado
    // (ej: en enero, "Diciembre" es el diciembre anterior). Se puede forzar el año con "anio".
    private LocalDateTime[] rangoDelMes(int mes, Integer anio) {
        if (mes < 1 || mes > 12) {
            return null; // sin filtro de fecha
        }
        LocalDate hoy = LocalDate.now();
        int anioReal = (anio != null && anio > 0)
                ? anio
                : (mes > hoy.getMonthValue() ? hoy.getYear() - 1 : hoy.getYear());
        LocalDateTime desde = LocalDate.of(anioReal, mes, 1).atStartOfDay();
        return new LocalDateTime[]{desde, desde.plusMonths(1)};
    }

    @Transactional(readOnly = true)
    public Page<Venta> obtenerHistorialConFiltros(String buscar, String criterio, int mes, Integer anio, Pageable pageable) {
        LocalDateTime[] rango = rangoDelMes(mes, anio);
        LocalDateTime ahora = LocalDateTime.now();
        return ventaRepository.buscarConFiltros(buscar, criterio,
                rango == null,
                rango == null ? ahora : rango[0],
                rango == null ? ahora : rango[1],
                pageable);
    }

    @Transactional(readOnly = true)
    public Double obtenerTotalGlobal(String buscar, String criterio, int mes, Integer anio) {
        LocalDateTime[] rango = rangoDelMes(mes, anio);
        LocalDateTime ahora = LocalDateTime.now();
        Double total = ventaRepository.obtenerTotalGlobal(buscar, criterio,
                rango == null,
                rango == null ? ahora : rango[0],
                rango == null ? ahora : rango[1]);
        return total != null ? total : 0.0;
    }
}
