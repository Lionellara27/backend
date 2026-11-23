package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;

// Ajustes de la pestaña "Precios y Dólar" de Configuración. Hay una sola fila (id = 1).
@Data
@Entity
@Table(name = "configuracion_divisa")
public class ConfiguracionDivisa {

    @Id
    private Long id = 1L;

    // Cotización del dólar que el local tiene cargada HOY
    @Column(nullable = false)
    private Double cotizacionUsd = 1000.0;

    // Quedó de la versión anterior (la cotización ya no se busca sola: se pide con un botón)
    @Column(nullable = false)
    private boolean modoAutomatico = false;

    // Qué dólar se sigue: oficial, blue, tarjeta o bolsa. (Columna nueva: puede venir vacía en bases viejas)
    private String tipoDolar = "blue";

    // Con qué dólar están calculados los precios del catálogo. Los artículos se guardan SOLO en pesos:
    // con este único número alcanza para saber cuánto se atrasaron los precios respecto del dólar de hoy.
    // (Columna nueva: null = todavía no se definió, se toma la cotización actual)
    private Double cotizacionBasePrecios;

    // De qué tipo era ese dólar (blue, oficial...) y de qué día es el valor ("23/11/2026").
    // Sirve para decir: "tus precios están calculados con el dólar oficial a $ 1.600, del 23/11/2026".
    private String tipoDolarBase;
    private String fechaBasePrecios;
}
