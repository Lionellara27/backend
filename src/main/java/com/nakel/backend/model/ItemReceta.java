package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "items_receta")
public class ItemReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Qué insumo estamos usando
    @ManyToOne
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    // Cuántos cm2, unidades u horas usamos
    @Column(nullable = false)
    private BigDecimal cantidadUtilizada = BigDecimal.ZERO;

    // Cuánto costó esta línea (Cantidad * Precio del insumo)
    private BigDecimal subtotal = BigDecimal.ZERO;
}