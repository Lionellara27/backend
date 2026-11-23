package com.nakel.backend.service;

import com.nakel.backend.model.Articulo;
import com.nakel.backend.model.Categoria;
import com.nakel.backend.model.ConfiguracionDivisa;
import com.nakel.backend.model.HistorialAumento;
import com.nakel.backend.model.HistorialAumentoDetalle;
import com.nakel.backend.repository.ArticuloRepository;
import com.nakel.backend.repository.CategoriaRepository;
import com.nakel.backend.repository.ConfiguracionDivisaRepository;
import com.nakel.backend.repository.HistorialAumentoDetalleRepository;
import com.nakel.backend.repository.HistorialAumentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cambios masivos de precios: por porcentaje (una categoría o todas) y según el dólar.
 *
 * Cada cambio guarda el precio anterior y el nuevo de cada artículo, así se puede DESHACER
 * exactamente durante {@link #DIAS_PARA_DESHACER} días. Pasado ese plazo queda firme.
 */
@Service
public class AumentoPreciosService {

    public static final int DIAS_PARA_DESHACER = 7;
    private static final Long ID_CONFIG = 1L;

    private final ArticuloRepository articuloRepository;
    private final CategoriaRepository categoriaRepository;
    private final HistorialAumentoRepository historialRepository;
    private final HistorialAumentoDetalleRepository detalleRepository;
    private final ConfiguracionDivisaRepository divisaRepository;

    public AumentoPreciosService(ArticuloRepository articuloRepository,
                                 CategoriaRepository categoriaRepository,
                                 HistorialAumentoRepository historialRepository,
                                 HistorialAumentoDetalleRepository detalleRepository,
                                 ConfiguracionDivisaRepository divisaRepository) {
        this.articuloRepository = articuloRepository;
        this.categoriaRepository = categoriaRepository;
        this.historialRepository = historialRepository;
        this.detalleRepository = detalleRepository;
        this.divisaRepository = divisaRepository;
    }

    // =====================================================================
    //  AUMENTO POR PORCENTAJE
    // =====================================================================

    /** Sube un porcentaje los precios de una categoría (o de todas si categoriaId es null). */
    @Transactional
    public HistorialAumento aumentarPorPorcentaje(Long categoriaId, BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El porcentaje debe ser mayor a 0.");
        }
        if (porcentaje.compareTo(BigDecimal.valueOf(1000)) > 0) {
            throw new IllegalArgumentException("El aumento no puede superar el 1000%.");
        }
        BigDecimal factor = BigDecimal.ONE.add(porcentaje.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return aplicar(HistorialAumento.TIPO_PORCENTAJE, categoriaId, factor, porcentaje, null, null);
    }

    // =====================================================================
    //  ACTUALIZACIÓN SEGÚN EL DÓLAR
    // =====================================================================

    /**
     * Lleva todos los precios al dólar de hoy: si los precios estaban calculados con el dólar a 1.000
     * y ahora está a 1.500, todo sube 50%. Después de esto, el dólar "de referencia de los precios"
     * pasa a ser el de hoy.
     */
    @Transactional
    public HistorialAumento actualizarSegunDolar() {
        ConfiguracionDivisa config = divisaRepository.findById(ID_CONFIG)
                .orElseThrow(() -> new IllegalArgumentException("Primero guardá la cotización del dólar."));

        double actual = config.getCotizacionUsd();
        double base = config.getCotizacionBasePrecios() != null ? config.getCotizacionBasePrecios() : actual;
        if (base <= 0 || actual <= 0) {
            throw new IllegalArgumentException("La cotización del dólar no es válida.");
        }
        if (Math.abs(actual - base) < 0.005) {
            throw new IllegalArgumentException("Los precios ya están al día con el dólar: no hay nada que actualizar.");
        }

        BigDecimal factor = BigDecimal.valueOf(actual).divide(BigDecimal.valueOf(base), 10, RoundingMode.HALF_UP);
        BigDecimal porcentaje = factor.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        String detalle = "Dólar " + nombreTipo(config.getTipoDolar()) + ": de " + pesos(base) + " a " + pesos(actual);

        HistorialAumento registro = aplicar(HistorialAumento.TIPO_DOLAR, null, factor, porcentaje, detalle, base);
        // Para poder volver atrás: de qué tipo y de qué día era el dólar anterior
        registro.setTipoDolarAnterior(config.getTipoDolarBase());
        registro.setFechaBaseAnterior(config.getFechaBasePrecios());
        registro = historialRepository.save(registro);

        // Desde ahora los precios están calculados con el dólar de hoy
        config.setCotizacionBasePrecios(actual);
        config.setTipoDolarBase(config.getTipoDolar());
        config.setFechaBasePrecios(hoy());
        divisaRepository.save(config);
        return registro;
    }

    // =====================================================================
    //  LO COMÚN: aplicar un factor y dejarlo anotado
    // =====================================================================

    private HistorialAumento aplicar(String tipo, Long categoriaId, BigDecimal factor, BigDecimal porcentaje,
                                     String detalle, Double cotizacionAnterior) {
        String categoriaNombre = "Todas las categorías";
        if (categoriaId != null) {
            Categoria categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new IllegalArgumentException("La categoría elegida ya no existe."));
            categoriaNombre = categoria.getNombre();
        }

        List<Articulo> articulos = articuloRepository.findActivosParaAumento(categoriaId);
        if (articulos.isEmpty()) {
            throw new IllegalArgumentException("No hay artículos activos en " + categoriaNombre + ": no se cambió ningún precio.");
        }

        HistorialAumento registro = new HistorialAumento();
        registro.setFecha(LocalDateTime.now());
        registro.setTipo(tipo);
        registro.setCategoriaId(categoriaId);
        registro.setCategoriaNombre(categoriaNombre);
        registro.setPorcentaje(porcentaje);
        registro.setCantidadArticulos(articulos.size());
        registro.setEstado(HistorialAumento.ESTADO_APLICADO);
        registro.setDetalle(detalle);
        registro.setCotizacionAnterior(cotizacionAnterior);
        registro = historialRepository.save(registro);

        List<HistorialAumentoDetalle> detalles = new ArrayList<>();
        for (Articulo art : articulos) {
            BigDecimal anterior = art.getPrecio();
            BigDecimal nuevo = anterior.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            art.setPrecio(nuevo);

            HistorialAumentoDetalle d = new HistorialAumentoDetalle();
            d.setAumentoId(registro.getId());
            d.setArticuloId(art.getId());
            d.setPrecioAnterior(anterior);
            d.setPrecioNuevo(nuevo);
            detalles.add(d);
        }
        articuloRepository.saveAll(articulos);
        detalleRepository.saveAll(detalles);
        return registro;
    }

    // =====================================================================
    //  DESHACER
    // =====================================================================

    /**
     * Devuelve los precios a como estaban antes de ese cambio.
     * Solo toca los artículos cuyo precio sigue siendo el que dejó ese cambio: si después se editó a mano
     * o hubo otro aumento encima, ese artículo no se pisa.
     *
     * @return cuántos artículos se restauraron y cuántos se dejaron como estaban
     */
    @Transactional
    public Map<String, Object> deshacer(Long idAumento) {
        HistorialAumento registro = historialRepository.findById(idAumento)
                .orElseThrow(() -> new IllegalArgumentException("Ese cambio de precios no existe."));

        if (!HistorialAumento.ESTADO_APLICADO.equals(registro.getEstado())) {
            throw new IllegalArgumentException("Ese cambio ya fue deshecho.");
        }
        if (diasRestantes(registro) <= 0) {
            throw new IllegalArgumentException("Ya pasaron " + DIAS_PARA_DESHACER + " días de ese cambio: no se puede deshacer.");
        }

        List<HistorialAumentoDetalle> detalles = detalleRepository.findByAumentoId(idAumento);
        int restaurados = 0, sinTocar = 0;
        List<Articulo> aGuardar = new ArrayList<>();
        for (HistorialAumentoDetalle d : detalles) {
            Articulo art = articuloRepository.findById(d.getArticuloId()).orElse(null);
            if (art != null && art.getPrecio() != null && art.getPrecio().compareTo(d.getPrecioNuevo()) == 0) {
                art.setPrecio(d.getPrecioAnterior());
                aGuardar.add(art);
                restaurados++;
            } else {
                sinTocar++;
            }
        }

        if (restaurados == 0) {
            throw new IllegalArgumentException("No se pudo deshacer: los precios de esos artículos cambiaron después "
                    + "(por otro aumento o a mano). Si hubo un aumento más nuevo, deshacé primero ese.");
        }

        articuloRepository.saveAll(aGuardar);
        registro.setEstado(HistorialAumento.ESTADO_DESHECHO);
        registro.setFechaDeshecho(LocalDateTime.now());
        historialRepository.save(registro);

        // Si era una actualización por dólar, los precios vuelven a estar calculados con el dólar de antes
        if (HistorialAumento.TIPO_DOLAR.equals(registro.getTipo()) && registro.getCotizacionAnterior() != null) {
            divisaRepository.findById(ID_CONFIG).ifPresent(config -> {
                config.setCotizacionBasePrecios(registro.getCotizacionAnterior());
                config.setTipoDolarBase(registro.getTipoDolarAnterior());
                config.setFechaBasePrecios(registro.getFechaBaseAnterior());
                divisaRepository.save(config);
            });
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("restaurados", restaurados);
        resultado.put("sinTocar", sinTocar);
        return resultado;
    }

    // =====================================================================
    //  HISTORIAL
    // =====================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> historial(int pagina, int tamanio) {
        Page<HistorialAumento> page = historialRepository.findAllByOrderByFechaDescIdDesc(
                PageRequest.of(Math.max(0, pagina), Math.max(1, tamanio)));

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> filas = new ArrayList<>();
        for (HistorialAumento h : page.getContent()) {
            long dias = diasRestantes(h);
            boolean aplicado = HistorialAumento.ESTADO_APLICADO.equals(h.getEstado());

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", h.getId());
            fila.put("fecha", h.getFecha().format(formato));
            fila.put("tipo", h.getTipo());
            fila.put("categoria", h.getCategoriaNombre());
            fila.put("porcentaje", h.getPorcentaje());
            fila.put("cantidadArticulos", h.getCantidadArticulos());
            fila.put("detalle", h.getDetalle());
            fila.put("estado", h.getEstado());
            fila.put("fechaDeshecho", h.getFechaDeshecho() != null ? h.getFechaDeshecho().format(formato) : null);
            fila.put("sePuedeDeshacer", aplicado && dias > 0);
            fila.put("diasRestantes", aplicado ? Math.max(0, dias) : 0);
            filas.add(fila);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("content", filas);
        resultado.put("totalElements", page.getTotalElements());
        resultado.put("totalPages", page.getTotalPages());
        resultado.put("number", page.getNumber());
        resultado.put("diasParaDeshacer", DIAS_PARA_DESHACER);
        return resultado;
    }

    /** Días que quedan para poder deshacer (7 el mismo día, 0 cuando ya venció). */
    private long diasRestantes(HistorialAumento h) {
        LocalDateTime vence = h.getFecha().plusDays(DIAS_PARA_DESHACER);
        if (!LocalDateTime.now().isBefore(vence)) return 0;
        return ChronoUnit.DAYS.between(LocalDateTime.now(), vence) + 1;
    }

    /** La fecha de hoy como se muestra en pantalla: 23/11/2026 */
    public static String hoy() {
        return java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static String nombreTipo(String tipo) {
        if (tipo == null) return "";
        return switch (tipo.trim().toLowerCase()) {
            case "oficial" -> "oficial";
            case "blue" -> "blue";
            case "tarjeta" -> "tarjeta";
            case "bolsa" -> "bolsa (MEP)";
            default -> tipo;
        };
    }

    private String pesos(double valor) {
        return new DecimalFormat("$ #,##0.##", new DecimalFormatSymbols(new Locale("es", "AR"))).format(valor);
    }
}
