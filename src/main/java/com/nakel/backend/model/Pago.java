package com.nakel.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 BLINDAJE 1: Lazy + JsonIgnore (¡Clave para no romper el Frontend!)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    @JsonIgnore
    private Venta venta;

    // Ej: "Efectivo", "Tarjeta de Débito", "Mercado Pago"
    @Column(nullable = false)
    private String metodoPago;

    @Column(nullable = false)
    private BigDecimal monto;
}