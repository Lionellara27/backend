package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private String medioPago; // Ej: "Efectivo", "Tarjeta", "Mercado Pago"

    // El campo que separa el mostrador de la AFIP
    @Column(nullable = false)
    private Boolean esFiscal;

    // Relación: Muchas ventas pueden pertenecer a un solo Cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
}
