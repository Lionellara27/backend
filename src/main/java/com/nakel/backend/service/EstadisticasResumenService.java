package com.nakel.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Números del módulo de Estadísticas para un período (hoy, 7 días, mes, mes pasado, año).
 *
 * Todo se calcula en la base con SUM / COUNT / GROUP BY: no se traen las ventas a memoria.
 * (La única excepción es la evolución, que trae solo fecha y total de cada venta del período.)
 *
 * Reglas comunes:
 *  - Los presupuestos no son ventas: se excluyen siempre.
 *  - En los rankings de productos se descuentan las unidades devueltas por cambios.
 */
@Service
public class EstadisticasResumenService {

    private static final String SIN_PRESUPUESTOS = "(v.tipoComprobante IS NULL OR v.tipoComprobante <> 'Presupuesto')";
    private static final String EN_PERIODO = "v.fechaHora >= :desde AND v.fechaHora < :hasta AND " + SIN_PRESUPUESTOS;
    // Unidades realmente vendidas de un renglón (descontando lo devuelto) y su importe
    private static final String UNIDADES = "(d.cantidad - COALESCE(d.cantidadDevuelta, 0))";
    private static final String IMPORTE = "(d.precioUnitario * " + UNIDADES + ")";
    private static final String CUIT_CONSUMIDOR_FINAL = "00000000";
    private static final int DIAS_SIN_MOVIMIENTO = 90;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Map<String, Object> resumen(String periodo, Integer anio) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime manana = hoy.plusDays(1).atStartOfDay();

        // Cada período tiene su rango [desde, hasta) y el rango anterior contra el que se compara
        String nombre;
        LocalDateTime desde, hasta, antDesde, antHasta;
        String p = periodo == null ? "MES" : periodo.trim().toUpperCase();
        switch (p) {
            case "HOY" -> {
                nombre = "Hoy";
                desde = hoy.atStartOfDay(); hasta = manana;
                antDesde = desde.minusDays(1); antHasta = desde;
            }
            case "SEMANA" -> {
                nombre = "Últimos 7 días";
                desde = hoy.minusDays(6).atStartOfDay(); hasta = manana; // hoy + los 6 anteriores = 7 días
                antDesde = desde.minusDays(7); antHasta = desde;
            }
            case "MES_PASADO" -> {
                nombre = "Mes pasado";
                desde = hoy.withDayOfMonth(1).minusMonths(1).atStartOfDay(); hasta = hoy.withDayOfMonth(1).atStartOfDay();
                antDesde = desde.minusMonths(1); antHasta = desde;
            }
            case "ANIO" -> {
                // Año elegido en la pantalla. Si es el año en curso llega hasta hoy; si es uno pasado, entero.
                int elegido = (anio != null && anio >= 2000 && anio <= hoy.getYear()) ? anio : hoy.getYear();
                nombre = "Año " + elegido;
                desde = LocalDate.of(elegido, 1, 1).atStartOfDay();
                hasta = elegido == hoy.getYear() ? manana : LocalDate.of(elegido + 1, 1, 1).atStartOfDay();
                antDesde = desde.minusYears(1); antHasta = hasta.minusYears(1);
            }
            default -> {
                p = "MES";
                nombre = "Este mes";
                desde = hoy.withDayOfMonth(1).atStartOfDay(); hasta = manana;
                antDesde = desde.minusMonths(1); antHasta = hasta.minusMonths(1); // mismos días del mes anterior
            }
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("periodo", p);
        r.put("periodoNombre", nombre);
        r.put("desde", desde.toLocalDate().toString());
        r.put("hasta", hasta.minusDays(1).toLocalDate().toString());
        r.put("anio", desde.getYear());

        // Años que se pueden elegir: desde el de la primera venta hasta el actual (el más nuevo primero)
        LocalDateTime primeraVenta = (LocalDateTime) em.createQuery(
                "SELECT MIN(v.fechaHora) FROM Venta v WHERE " + SIN_PRESUPUESTOS).getSingleResult();
        int primerAnio = primeraVenta != null ? Math.min(primeraVenta.getYear(), hoy.getYear()) : hoy.getYear();
        List<Integer> anios = new ArrayList<>();
        for (int a = hoy.getYear(); a >= primerAnio; a--) anios.add(a);
        r.put("anios", anios);

        // ---------- VENTAS ----------
        Object[] actual = totalYCantidad(desde, hasta);
        Object[] anterior = totalYCantidad(antDesde, antHasta);
        double total = num(actual[0]);
        long cantidad = ((Number) actual[1]).longValue();
        r.put("totalVentas", total);
        r.put("cantidadVentas", cantidad);
        r.put("ticketPromedio", cantidad > 0 ? total / cantidad : 0);
        r.put("totalAnterior", num(anterior[0]));
        r.put("cantidadAnterior", ((Number) anterior[1]).longValue());

        Object[] deHoy = totalYCantidad(hoy.atStartOfDay(), manana);
        r.put("ventasHoy", num(deHoy[0]));
        r.put("cantidadHoy", ((Number) deHoy[1]).longValue());

        r.put("evolucion", evolucion(p, desde, hasta));
        r.put("mayorCompra", mayorCompra(desde, hasta));
        r.put("mediosPago", filas(
                "SELECT p.metodoPago, SUM(p.monto) FROM Pago p JOIN p.venta v WHERE " + EN_PERIODO
                        + " GROUP BY p.metodoPago ORDER BY SUM(p.monto) DESC",
                desde, hasta, 0, "nombre", "total"));

        // ---------- PRODUCTOS ----------
        r.put("categorias", filas(
                "SELECT COALESCE(c.nombre, 'Sin categoría'), SUM(" + UNIDADES + "), SUM(" + IMPORTE + ") "
                        + "FROM DetalleVenta d JOIN d.venta v JOIN d.articulo a LEFT JOIN a.categoria c WHERE " + EN_PERIODO
                        + " GROUP BY c.nombre HAVING SUM(" + UNIDADES + ") > 0 ORDER BY SUM(" + IMPORTE + ") DESC",
                desde, hasta, 0, "nombre", "unidades", "total"));
        r.put("topArticulos", filas(
                "SELECT a.codigo, a.nombre, SUM(" + UNIDADES + "), SUM(" + IMPORTE + ") "
                        + "FROM DetalleVenta d JOIN d.venta v JOIN d.articulo a WHERE " + EN_PERIODO
                        + " GROUP BY a.id, a.codigo, a.nombre HAVING SUM(" + UNIDADES + ") > 0 "
                        + "ORDER BY SUM(" + UNIDADES + ") DESC, SUM(" + IMPORTE + ") DESC",
                desde, hasta, 10, "codigo", "nombre", "unidades", "total"));
        r.put("origenes", filas(
                "SELECT a.origen, SUM(" + UNIDADES + "), SUM(" + IMPORTE + ") "
                        + "FROM DetalleVenta d JOIN d.venta v JOIN d.articulo a WHERE " + EN_PERIODO
                        + " GROUP BY a.origen HAVING SUM(" + UNIDADES + ") > 0 ORDER BY SUM(" + IMPORTE + ") DESC",
                desde, hasta, 0, "nombre", "unidades", "total"));

        // ---------- CLIENTES ----------
        String identificado = "c.cuit IS NOT NULL AND c.cuit <> '" + CUIT_CONSUMIDOR_FINAL + "'";
        List<Map<String, Object>> clientes = filas(
                "SELECT c.nombre, SUM(v.total), COUNT(v) FROM Venta v JOIN v.cliente c WHERE " + EN_PERIODO
                        + " AND " + identificado + " GROUP BY c.id, c.nombre ORDER BY SUM(v.total) DESC",
                desde, hasta, 10, "nombre", "total", "compras");
        for (Map<String, Object> c : clientes) {
            long compras = ((Number) c.get("compras")).longValue();
            c.put("ticketPromedio", compras > 0 ? num(c.get("total")) / compras : 0);
        }
        r.put("topClientes", clientes);

        Object[] identificados = (Object[]) em.createQuery(
                        "SELECT COALESCE(SUM(v.total), 0), COUNT(v) FROM Venta v JOIN v.cliente c WHERE " + EN_PERIODO + " AND " + identificado)
                .setParameter("desde", desde).setParameter("hasta", hasta).getSingleResult();
        r.put("totalClientesIdentificados", num(identificados[0]));
        r.put("ventasClientesIdentificados", ((Number) identificados[1]).longValue());
        r.put("totalConsumidorFinal", Math.max(0, total - num(identificados[0])));
        r.put("ventasConsumidorFinal", Math.max(0, cantidad - ((Number) identificados[1]).longValue()));

        // ---------- INVENTARIO (foto de hoy, no depende del período) ----------
        Object[] inv = (Object[]) em.createQuery(
                "SELECT COUNT(a), COALESCE(SUM(a.stockActual), 0), COALESCE(SUM(a.precio * a.stockActual), 0) "
                        + "FROM Articulo a WHERE a.activo = true").getSingleResult();
        r.put("productosActivos", ((Number) inv[0]).longValue());
        r.put("unidadesEnStock", ((Number) inv[1]).longValue());
        r.put("valorStock", num(inv[2]));

        // Stock crítico: cada artículo contra SU PROPIO mínimo (no un número fijo para todos)
        r.put("cantidadStockCritico", ((Number) em.createQuery(
                "SELECT COUNT(a) FROM Articulo a WHERE a.activo = true AND a.stockActual <= a.stockMinimo").getSingleResult()).longValue());
        r.put("stockCritico", filasSinFechas(
                "SELECT a.codigo, a.nombre, a.stockActual, a.stockMinimo FROM Articulo a "
                        + "WHERE a.activo = true AND a.stockActual <= a.stockMinimo ORDER BY a.stockActual ASC, a.nombre ASC",
                null, 300, "codigo", "nombre", "stock", "minimo"));

        // Sin movimiento: con stock, y sin ninguna venta en los últimos 90 días
        LocalDateTime corte = hoy.minusDays(DIAS_SIN_MOVIMIENTO).atStartOfDay();
        String sinMovimiento = "FROM Articulo a WHERE a.activo = true AND a.stockActual > 0 AND a.id NOT IN ("
                + "SELECT d.articulo.id FROM DetalleVenta d JOIN d.venta v WHERE v.fechaHora >= :corte AND " + SIN_PRESUPUESTOS + ")";
        Object[] quietos = (Object[]) em.createQuery(
                        "SELECT COUNT(a), COALESCE(SUM(a.precio * a.stockActual), 0) " + sinMovimiento)
                .setParameter("corte", corte).getSingleResult();
        r.put("diasSinMovimiento", DIAS_SIN_MOVIMIENTO);
        r.put("cantidadSinMovimiento", ((Number) quietos[0]).longValue());
        r.put("valorSinMovimiento", num(quietos[1]));
        r.put("sinMovimiento", filasSinFechas(
                "SELECT a.codigo, a.nombre, a.stockActual, (a.precio * a.stockActual) " + sinMovimiento
                        + " ORDER BY (a.precio * a.stockActual) DESC",
                corte, 15, "codigo", "nombre", "stock", "valor"));

        // Vales vigentes: plata que el local "debe" en mercadería
        Object[] vales = (Object[]) em.createQuery(
                        "SELECT COUNT(x), COALESCE(SUM(x.monto), 0) FROM Vale x WHERE x.estado = 'ACTIVO' AND x.fechaVencimiento > :ahora")
                .setParameter("ahora", LocalDateTime.now()).getSingleResult();
        r.put("cantidadVales", ((Number) vales[0]).longValue());
        r.put("totalVales", num(vales[1]));

        return r;
    }

    // =====================================================================

    private Object[] totalYCantidad(LocalDateTime desde, LocalDateTime hasta) {
        return (Object[]) em.createQuery("SELECT COALESCE(SUM(v.total), 0), COUNT(v) FROM Venta v WHERE " + EN_PERIODO)
                .setParameter("desde", desde).setParameter("hasta", hasta).getSingleResult();
    }

    private Map<String, Object> mayorCompra(LocalDateTime desde, LocalDateTime hasta) {
        List<?> filas = em.createQuery(
                        "SELECT v.id, v.total, v.fechaHora, c.nombre FROM Venta v LEFT JOIN v.cliente c WHERE " + EN_PERIODO
                                + " ORDER BY v.total DESC, v.id DESC")
                .setParameter("desde", desde).setParameter("hasta", hasta).setMaxResults(1).getResultList();
        if (filas.isEmpty()) return null;
        Object[] f = (Object[]) filas.get(0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idVenta", f[0]);
        m.put("total", num(f[1]));
        m.put("fecha", f[2] != null ? ((LocalDateTime) f[2]).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        m.put("cliente", f[3] != null ? f[3] : "Consumidor Final");
        return m;
    }

    /**
     * Ventas agrupadas para el gráfico: por hora (hoy), por día (semana y mes) o por mes (año).
     * Devuelve también los tramos sin ventas, en cero, para que el gráfico no "salte" días.
     */
    private List<Map<String, Object>> evolucion(String periodo, LocalDateTime desde, LocalDateTime hasta) {
        Map<String, double[]> tramos = new LinkedHashMap<>(); // etiqueta -> [total, cantidad]
        DateTimeFormatter porDia = DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter porMes = DateTimeFormatter.ofPattern("MMM", new Locale("es", "AR"));

        if ("HOY".equals(periodo)) {
            for (int h = 8; h <= 21; h++) tramos.put(String.format("%02d h", h), new double[2]);
        } else if ("ANIO".equals(periodo)) {
            for (LocalDateTime m = desde; m.isBefore(hasta); m = m.plusMonths(1)) tramos.put(m.format(porMes), new double[2]);
        } else {
            long dias = ChronoUnit.DAYS.between(desde, hasta);
            for (int i = 0; i < dias; i++) tramos.put(desde.plusDays(i).format(porDia), new double[2]);
        }

        List<?> ventas = em.createQuery("SELECT v.fechaHora, v.total FROM Venta v WHERE " + EN_PERIODO)
                .setParameter("desde", desde).setParameter("hasta", hasta).getResultList();
        for (Object fila : ventas) {
            Object[] f = (Object[]) fila;
            if (f[0] == null) continue;
            LocalDateTime fecha = (LocalDateTime) f[0];
            String clave = "HOY".equals(periodo) ? String.format("%02d h", fecha.getHour())
                    : "ANIO".equals(periodo) ? fecha.format(porMes) : fecha.format(porDia);
            double[] acumulado = tramos.computeIfAbsent(clave, k -> new double[2]);
            acumulado[0] += num(f[1]);
            acumulado[1] += 1;
        }

        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map.Entry<String, double[]> t : tramos.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("etiqueta", t.getKey());
            m.put("total", t.getValue()[0]);
            m.put("cantidad", (long) t.getValue()[1]);
            salida.add(m);
        }
        // Las horas fuera de 8 a 21 se agregaron al final: las ordenamos
        if ("HOY".equals(periodo)) salida.sort((a, b) -> ((String) a.get("etiqueta")).compareTo((String) b.get("etiqueta")));
        return salida;
    }

    /** Ejecuta una consulta agregada del período y arma una lista de filas {columna: valor}. */
    private List<Map<String, Object>> filas(String jpql, LocalDateTime desde, LocalDateTime hasta, int maximo, String... columnas) {
        var consulta = em.createQuery(jpql).setParameter("desde", desde).setParameter("hasta", hasta);
        if (maximo > 0) consulta.setMaxResults(maximo);
        return aFilas(consulta.getResultList(), columnas);
    }

    /** Igual, para consultas de inventario (sin período; opcionalmente con fecha de corte). */
    private List<Map<String, Object>> filasSinFechas(String jpql, LocalDateTime corte, int maximo, String... columnas) {
        var consulta = em.createQuery(jpql);
        if (corte != null) consulta.setParameter("corte", corte);
        if (maximo > 0) consulta.setMaxResults(maximo);
        return aFilas(consulta.getResultList(), columnas);
    }

    private List<Map<String, Object>> aFilas(List<?> resultado, String... columnas) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Object fila : resultado) {
            Object[] f = (Object[]) fila;
            Map<String, Object> m = new LinkedHashMap<>();
            for (int i = 0; i < columnas.length; i++) {
                Object valor = f[i];
                // Los importes viajan como número simple, no como BigDecimal con 6 decimales
                m.put(columnas[i], valor instanceof BigDecimal b ? (Object) b.doubleValue() : valor);
            }
            salida.add(m);
        }
        return salida;
    }

    private double num(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : 0;
    }
}
