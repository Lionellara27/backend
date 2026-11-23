package com.nakel.backend.service;

import com.nakel.backend.model.CajaDiaria;
import com.nakel.backend.model.Pago;
import com.nakel.backend.model.TipoMovimientoCaja;
import com.nakel.backend.model.Usuario;
import com.nakel.backend.model.Venta;
import com.nakel.backend.repository.CajaRepository;
import com.nakel.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MovimientoCajaService movimientoCajaService;

    /**
     * Busca el usuario que opera la caja. NUNCA devuelve null mientras exista algún usuario:
     * si el nombre que manda la pantalla no existe (por ejemplo porque se cambió el usuario
     * en Seguridad y la sesión quedó con el nombre viejo), usa "admin" y, si tampoco está,
     * el primer usuario del sistema. Antes devolvía null y eso hacía fallar todas las ventas.
     */
    private Usuario resolverUsuario(String username) {
        Optional<Usuario> usuario = (username == null || username.isBlank())
                ? Optional.empty()
                : usuarioRepository.findByNombreUsuario(username.trim());

        if (usuario.isEmpty()) {
            usuario = usuarioRepository.findByNombreUsuario("admin");
        }
        if (usuario.isEmpty()) {
            usuario = usuarioRepository.findAll().stream().findFirst();
        }
        return usuario.orElseThrow(() -> new IllegalStateException("No hay ningún usuario cargado en el sistema."));
    }

    // 🟢 1. Obtener la caja activa del día, reabrirla si se cerró temporalmente, o crear nueva si cambió el día
    public synchronized CajaDiaria obtenerOCrearCajaActual(String username) {
        LocalDate hoy = LocalDate.now();

        // Buscamos al usuario que está operando
        Usuario usuario = resolverUsuario(username);

        // Traemos todas las cajas para buscar exactamente la fecha de hoy
        List<CajaDiaria> todasLasCajas = cajaRepository.findAllByOrderByFechaAperturaDesc();

        Optional<CajaDiaria> cajaDeHoyOpt = todasLasCajas.stream()
                .filter(c -> c.getFecha() != null && c.getFecha().equals(hoy))
                .findFirst();

        // =========================================================
        // 📍 CASO A: LA CAJA DE HOY YA EXISTE EN LA BASE DE DATOS
        // =========================================================
        if (cajaDeHoyOpt.isPresent()) {
            CajaDiaria cajaDeHoy = cajaDeHoyOpt.get();

            // 1. Si la caja ya está abierta, la devolvemos
            if ("ABIERTA".equalsIgnoreCase(cajaDeHoy.getEstado())) {
                return cajaDeHoy;
            }

            // 2. Si estaba cerrada (ej. cierre del mediodía), LA REABRIMOS
            cajaDeHoy.setEstado("ABIERTA");
            cajaDeHoy.setFechaCierre(null);
            CajaDiaria cajaReabierta = cajaRepository.save(cajaDeHoy);

            movimientoCajaService.registrarMovimiento(
                    cajaReabierta, usuario, TipoMovimientoCaja.APERTURA_CAJA,
                    "Reapertura de Caja", "Reingreso al sistema en el mismo día", null, BigDecimal.ZERO
            );

            return cajaReabierta;
        }

        // =========================================================
        // 📍 CASO B: NO HAY CAJA DE HOY. ¿QUEDÓ ALGUNA DE AYER ABIERTA?
        // =========================================================
        for (CajaDiaria cajaVieja : todasLasCajas) {
            if (!"ABIERTA".equalsIgnoreCase(cajaVieja.getEstado())) continue;

            cajaVieja.setEstado("CERRADA");
            cajaVieja.setFechaCierre(LocalDateTime.now());
            cajaVieja.setObservaciones("Cierre automático por cambio de día.");

            BigDecimal saldoFinal = calcularSaldoFinal(cajaVieja);
            cajaVieja.setSaldoFinal(saldoFinal);
            cajaRepository.save(cajaVieja);

            movimientoCajaService.registrarMovimiento(
                    cajaVieja, usuario, TipoMovimientoCaja.CIERRE_CAJA,
                    "Cierre Automático", "El sistema cerró la caja del día anterior", null, saldoFinal
            );
        }

        // =========================================================
        // 📍 CASO C: CREAMOS LA CAJA NUEVA DE HOY LIMPIA
        // =========================================================
        CajaDiaria nuevaCaja = new CajaDiaria();
        nuevaCaja.setFecha(hoy);
        nuevaCaja.setFechaApertura(LocalDateTime.now());
        nuevaCaja.setEstado("ABIERTA");
        nuevaCaja.setUsuarioApertura(usuario);
        nuevaCaja.setSaldoInicial(BigDecimal.ZERO);
        nuevaCaja.setSaldoFinal(BigDecimal.ZERO);

        CajaDiaria cajaGuardada = cajaRepository.save(nuevaCaja);

        movimientoCajaService.registrarMovimiento(
                cajaGuardada, usuario, TipoMovimientoCaja.APERTURA_CAJA,
                "Apertura de Caja", "Caja del día iniciada", null, BigDecimal.ZERO
        );

        return cajaGuardada;
    }

    // Saldo Final = Saldo Inicial + Ventas - Egresos
    private BigDecimal calcularSaldoFinal(CajaDiaria caja) {
        return caja.getSaldoInicial()
                .add(caja.getTotalVentas())
                .subtract(caja.getTotalEgresos());
    }

    // 🔴 2. Cerrar la caja activa
    public synchronized CajaDiaria cerrarCajaActual(String username) throws Exception {
        CajaDiaria caja = cajaRepository.findFirstByEstadoOrderByFechaAperturaDesc("ABIERTA")
                .orElseThrow(() -> new Exception("No hay ninguna caja abierta para cerrar."));

        Usuario usuario = resolverUsuario(username);

        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado("CERRADA");
        caja.setUsuarioCierre(usuario);

        BigDecimal saldoFinalCalculado = calcularSaldoFinal(caja);
        caja.setSaldoFinal(saldoFinalCalculado);

        CajaDiaria cajaCerrada = cajaRepository.save(caja);

        movimientoCajaService.registrarMovimiento(
                cajaCerrada, usuario, TipoMovimientoCaja.CIERRE_CAJA, "Cierre de Caja", "Caja cerrada por el usuario", null, saldoFinalCalculado
        );

        return cajaCerrada;
    }

    // 🔥 3. Historial PAGINADO (solo lectura)
    @Transactional(readOnly = true)
    public Page<CajaDiaria> obtenerHistorialPaginado(Pageable pageable) {
        return cajaRepository.findAll(pageable);
    }

    // Suma un pago al rubro que corresponde según el medio de pago
    private void sumarAlRubro(CajaDiaria caja, String medio, BigDecimal monto) {
        if (medio == null || monto == null) return;

        if (medio.equalsIgnoreCase("Efectivo")) {
            caja.setTotalEfectivo(caja.getTotalEfectivo().add(monto));
        } else if (medio.equalsIgnoreCase("MercadoPago") || medio.equalsIgnoreCase("Mercado Pago")) {
            caja.setTotalMercadoPago(caja.getTotalMercadoPago().add(monto));
        } else if (medio.equalsIgnoreCase("Transferencia")) {
            caja.setTotalTransferencias(caja.getTotalTransferencias().add(monto));
        } else if (medio.equalsIgnoreCase("Tarjeta de Débito") || medio.equalsIgnoreCase("Tarjeta Debito")) {
            caja.setTotalTarjetaDebito(caja.getTotalTarjetaDebito().add(monto));
        } else if (medio.equalsIgnoreCase("Tarjeta de Crédito") || medio.equalsIgnoreCase("Tarjeta Credito")) {
            caja.setTotalTarjetaCredito(caja.getTotalTarjetaCredito().add(monto));
        }
    }

    // 💵 4. Acumular Venta en la Caja Activa (🔥 SOPORTA MULTI-PAGOS Y TARJETAS)
    // Si algo falla acá, la excepción sube y la venta entera se deshace: preferimos avisar
    // a que quede una venta cobrada que no figura en la caja.
    public void acumularVentaEnCajaActual(Venta venta, String username) {
        CajaDiaria caja = obtenerOCrearCajaActual(username);
        Usuario usuario = resolverUsuario(username);

        BigDecimal totalVenta = venta.getTotal();

        // Sumar cantidad de ventas y total vendido
        caja.setCantidadVentas(caja.getCantidadVentas() + 1);
        caja.setTotalVentas(caja.getTotalVentas().add(totalVenta));

        // 🔥 Recorremos todos los pagos individuales de la venta (Soporta Pago Mixto)
        if (venta.getPagos() != null) {
            for (Pago pago : venta.getPagos()) {
                sumarAlRubro(caja, pago.getMetodoPago(), pago.getMonto());
            }
        }

        caja.setSaldoFinal(calcularSaldoFinal(caja));
        cajaRepository.save(caja);

        String descripcionPago = "Mixto / Varios";
        if (venta.getPagos() != null && venta.getPagos().size() == 1) {
            descripcionPago = venta.getPagos().get(0).getMetodoPago();
        }

        movimientoCajaService.registrarMovimiento(
                caja, usuario, TipoMovimientoCaja.VENTA, "Venta realizada",
                "Cobro de ticket #" + venta.getId(), descripcionPago, totalVenta
        );
    }

    // 🔁 4.1 Diferencia cobrada en un CAMBIO: antes no entraba a la caja en ningún lado.
    public void registrarIngresoPorCambio(Long idVenta, String medioPago, BigDecimal monto, String username) {
        if (monto == null || monto.signum() <= 0) return;

        CajaDiaria caja = obtenerOCrearCajaActual(username);
        Usuario usuario = resolverUsuario(username);

        caja.setTotalVentas(caja.getTotalVentas().add(monto));
        sumarAlRubro(caja, medioPago, monto);
        caja.setSaldoFinal(calcularSaldoFinal(caja));
        cajaRepository.save(caja);

        movimientoCajaService.registrarMovimiento(
                caja, usuario, TipoMovimientoCaja.INGRESO, "Diferencia por cambio",
                "Cambio sobre la venta #" + idVenta, medioPago, monto
        );
    }

    // 💸 5. Registrar Retiro / Egreso de Dinero
    public void registrarEgreso(BigDecimal monto, String concepto, String descripcion, String username) {
        CajaDiaria caja = obtenerOCrearCajaActual(username);
        Usuario usuario = resolverUsuario(username);

        caja.setTotalEgresos(caja.getTotalEgresos().add(monto));
        caja.setSaldoFinal(calcularSaldoFinal(caja));

        cajaRepository.save(caja);

        movimientoCajaService.registrarMovimiento(
                caja, usuario, TipoMovimientoCaja.EGRESO, concepto, descripcion, "Efectivo", monto
        );
    }
}
