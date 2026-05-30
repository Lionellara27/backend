package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "telares")
public class Telar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nombre; // Ej: "Cuero liso"

    @Column(nullable = false)
    private BigDecimal costo; // Costo total del paño/telar

    @Column(nullable = false)
    private Integer anchoCm; // Medida en centímetros

    @Column(nullable = false)
    private Integer largoCm; // Medida en centímetros
}
