package com.nakel.backend.service;

import com.nakel.backend.model.CajaDiaria;
import com.nakel.backend.model.TipoMovimientoCaja; // 🔥 Importante
import com.nakel.backend.model.Usuario;
import com.nakel.backend.repository.CajaRepository;
import com.nakel.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    // 🟢 1. Obtener la caja activa del día, reabrirla si se cerró temporalmente, o crear nueva si cambió el día
    // 🟢 1. Obtener la caja activa del día, reabrirla si se cerró temporalmente, o crear nueva si cambió el día
    // 🔥 Le agregamos 'synchronized' para evitar que un doble-clic cree dos cajas al mismo tiempo
    public synchronized CajaDiaria obtenerOCrearCajaActual(String username) {
        LocalDate hoy = LocalDate.now();

        // Buscamos al usuario que está operando
        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseGet(() -> usuarioRepository.findByNombreUsuario("admin").orElse(null));

        // 🔥 LA MAGIA: Traemos la lista y buscamos EXACTAMENTE la fecha de hoy.
        // Esto evita que SQLite nos mienta con el ordenamiento de fechas.
        List<CajaDiaria> todasLasCajas = cajaRepository.findAllByOrderByFechaAperturaDesc();

        Optional<CajaDiaria> cajaDeHoyOpt = todasLasCajas.stream()
                .filter(c -> c.getFecha() != null && c.getFecha().equals(hoy))
                .findFirst();

        // =========================================================
        // 📍 CASO A: LA CAJA DE HOY YA EXISTE EN LA BASE DE DATOS
        // =========================================================
        if (cajaDeHoyOpt.isPresent()) {
            CajaDiaria cajaDeHoy = cajaDeHoyOpt.get();

            // 1. Si la caja ya está abierta, la devolvemos para seguir vendiendo
            if ("ABIERTA".equalsIgnoreCase(cajaDeHoy.getEstado())) {
                return cajaDeHoy;
            }

            // 2. Si estaba cerrada (ej. cierre del mediodía), LA REABRIMOS
            cajaDeHoy.setEstado("ABIERTA");
            cajaDeHoy.setFechaCierre(null); // Se quita el horario de cierre
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
        Optional<CajaDiaria> viejaAbierta = todasLasCajas.stream()
                .filter(c -> "ABIERTA".equalsIgnoreCase(c.getEstado()))
                .findFirst();

        if (viejaAbierta.isPresent()) {
            CajaDiaria cajaVieja = viejaAbierta.get();
            cajaVieja.setEstado("CERRADA");
            cajaVieja.setFechaCierre(LocalDateTime.now());
            cajaVieja.setObservaciones("Cierre automático por cambio de día.");

            BigDecimal saldoFinal = cajaVieja.getSaldoInicial()
                    .add(cajaVieja.getTotalVentas())
                    .subtract(cajaVieja.getTotalEgresos());

            cajaVieja.setSaldoFinal(saldoFinal);
            cajaRepository.save(cajaVieja);

            movimientoCajaService.registrarMovimiento(
                    cajaVieja, null, TipoMovimientoCaja.CIERRE_CAJA,
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

    // 🔴 2. Cerrar la caja activa
    public CajaDiaria cerrarCajaActual(String username) throws Exception {
        CajaDiaria caja = cajaRepository.findFirstByEstadoOrderByFechaAperturaDesc("ABIERTA")
                .orElseThrow(() -> new Exception("No hay ninguna caja abierta para cerrar."));

        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseGet(() -> usuarioRepository.findByNombreUsuario("admin").orElse(null));

        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado("CERRADA");
        caja.setUsuarioCierre(usuario);

        // Saldo Final = Saldo Inicial + Ventas - Egresos
        BigDecimal saldoFinalCalculado = caja.getSaldoInicial()
                .add(caja.getTotalVentas())
                .subtract(caja.getTotalEgresos());

        caja.setSaldoFinal(saldoFinalCalculado);

        CajaDiaria cajaCerrada = cajaRepository.save(caja);

        // 🔥 Usamos el Enum correspondiente
        movimientoCajaService.registrarMovimiento(
                cajaCerrada, usuario, TipoMovimientoCaja.CIERRE_CAJA, "Cierre de Caja", "Caja cerrada por el usuario", null, saldoFinalCalculado
        );

        return cajaCerrada;
    }

    // 📊 3. Traer todas las cajas para el Historial
    public List<CajaDiaria> obtenerHistorialCajas() {
        return cajaRepository.findAllByOrderByFechaAperturaDesc();
    }

    // 💵 4. Acumular Venta en la Caja Activa
    public void acumularVentaEnCajaActual(BigDecimal totalVenta, String medioPago, String username) {
        try {
            CajaDiaria caja = obtenerOCrearCajaActual(username);

            Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                    .orElseGet(() -> usuarioRepository.findByNombreUsuario("admin").orElse(null));

            // Sumar cantidad de ventas y total vendido
            caja.setCantidadVentas(caja.getCantidadVentas() + 1);
            caja.setTotalVentas(caja.getTotalVentas().add(totalVenta));

            // Repartir según el medio de pago
            if (medioPago != null) {
                if (medioPago.equalsIgnoreCase("Efectivo")) {
                    caja.setTotalEfectivo(caja.getTotalEfectivo().add(totalVenta));
                } else if (medioPago.equalsIgnoreCase("MercadoPago") || medioPago.equalsIgnoreCase("Mercado Pago")) {
                    caja.setTotalMercadoPago(caja.getTotalMercadoPago().add(totalVenta));
                } else if (medioPago.equalsIgnoreCase("Transferencia")) {
                    caja.setTotalTransferencias(caja.getTotalTransferencias().add(totalVenta));
                }
            }

            // Recalcular Saldo Final
            caja.setSaldoFinal(caja.getSaldoInicial().add(caja.getTotalVentas()).subtract(caja.getTotalEgresos()));

            cajaRepository.save(caja);

            // 🔥 Usamos el Enum correspondiente para Venta / Ingreso
            movimientoCajaService.registrarMovimiento(
                    caja, usuario, TipoMovimientoCaja.VENTA, "Venta realizada", "Cobro de ticket", medioPago, totalVenta
            );

        } catch (Exception e) {
            System.out.println("❌ Error al acumular venta en caja: " + e.getMessage());
        }
    }

    // 💸 5. Registrar Retiro / Egreso de Dinero (Ej: Compra de cuero)
    public void registrarEgreso(BigDecimal monto, String concepto, String descripcion, String username) {
        try {
            CajaDiaria caja = obtenerOCrearCajaActual(username);

            Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                    .orElseGet(() -> usuarioRepository.findByNombreUsuario("admin").orElse(null));

            caja.setTotalEgresos(caja.getTotalEgresos().add(monto));
            caja.setSaldoFinal(caja.getSaldoInicial().add(caja.getTotalVentas()).subtract(caja.getTotalEgresos()));

            cajaRepository.save(caja);

            // 🔥 Usamos el Enum correspondiente para Egreso
            movimientoCajaService.registrarMovimiento(
                    caja, usuario, TipoMovimientoCaja.EGRESO, concepto, descripcion, "Efectivo", monto
            );

        } catch (Exception e) {
            System.out.println("❌ Error al registrar egreso de caja: " + e.getMessage());
        }
    }
}