package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;

// Ajustes de la pestaña "Divisas" de Configuración. Hay una sola fila (id = 1).
@Data
@Entity
@Table(name = "configuracion_divisa")
public class ConfiguracionDivisa {

    @Id
    private Long id = 1L;

    // Cotización del dólar que usa el local
    @Column(nullable = false)
    private Double cotizacionUsd = 1000.0;

    @Column(nullable = false)
    private boolean modoAutomatico = false;
}
