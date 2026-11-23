package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un cambio masivo de precios (por porcentaje o por actualización según el dólar).
 * Queda registrado para poder verlo en el historial y deshacerlo durante unos días.
 * Los textos (tipo, estado) son String y no enum a propósito: SQLite le pone una restricción
 * CHECK a los enum y después no deja agregar valores nuevos.
 */
@Data
@Entity
@Table(name = "historial_aumentos")
public class HistorialAumento {

    public static final String TIPO_PORCENTAJE = "PORCENTAJE";
    public static final String TIPO_DOLAR = "DOLAR";
    public static final String ESTADO_APLICADO = "APLICADO";
    public static final String ESTADO_DESHECHO = "DESHECHO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private String tipo;

    // null = todas las categorías
    private Long categoriaId;

    @Column(nullable = false)
    private String categoriaNombre;

    // Puede ser negativo si el dólar bajó
    @Column(nullable = false)
    private BigDecimal porcentaje;

    @Column(nullable = false)
    private Integer cantidadArticulos;

    @Column(nullable = false)
    private String estado;

    private LocalDateTime fechaDeshecho;

    // Texto libre para mostrar en el historial (ej: "Dólar blue: de $ 1.000 a $ 1.675")
    @Column(length = 300)
    private String detalle;

    // Solo para tipo DOLAR: con qué dólar estaban calculados los precios antes, para restaurarlo al deshacer
    private Double cotizacionAnterior;
    // ...y de qué tipo y de qué día era, para restaurarlos también
    private String tipoDolarAnterior;
    private String fechaBaseAnterior;
}
