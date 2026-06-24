package com.nakel.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "detalles_venta")
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private BigDecimal subtotal;

    // 🔥 BLINDAJE 1: Lazy + JsonIgnore (Para que no explote el Frontend)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    @JsonIgnore
    private Venta venta;

    // 🔥 BLINDAJE 2: Lazy (Para no sobrecargar la memoria trayendo el catálogo entero)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articulo_id", nullable = false)
    private Articulo articulo;
}