package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/** Precio de UN artículo antes y después de un cambio masivo. Es lo que permite deshacerlo exacto. */
@Data
@Entity
@Table(name = "historial_aumentos_detalle")
public class HistorialAumentoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long aumentoId;

    @Column(nullable = false)
    private Long articuloId;

    @Column(nullable = false)
    private BigDecimal precioAnterior;

    @Column(nullable = false)
    private BigDecimal precioNuevo;
}
