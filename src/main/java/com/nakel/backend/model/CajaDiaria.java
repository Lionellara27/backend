package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "cajas_diarias")
public class CajaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Column(nullable = false)
    private BigDecimal saldoInicial;

    private BigDecimal saldoFinal; // Queda nulo hasta que cierre la caja

    @Column(nullable = false)
    private String estado = "ABIERTA"; // Por defecto nace abierta
}