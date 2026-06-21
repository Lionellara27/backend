package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ¿A qué venta pertenece esta plata?
    @ManyToOne
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    // Ej: "Efectivo", "Tarjeta de Débito", "Mercado Pago"
    @Column(nullable = false)
    private String metodoPago;

    @Column(nullable = false)
    private BigDecimal monto;
}