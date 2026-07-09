package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "insumos")
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre; // Ej: "Cuero liso", "Cierre YKK", "Remaches"

    // 🔗 Relación con la nueva tabla de Categorías de Insumos
    @ManyToOne
    @JoinColumn(name = "categoria_insumo_id", nullable = false)
    private CategoriaInsumo categoria;

    @Column(nullable = false)
    private BigDecimal costoTotal; // Cuánto costó la plancha o el lote de herrajes

    // --- Campos dinámicos (Pueden ser null dependiendo de lo que diga su categoría) ---

    private Integer anchoCm; // Solo para SUPERFICIE

    private Integer largoCm; // Solo para SUPERFICIE

    private Integer cantidad; // Solo para UNIDAD (Ej: vinieron 100 remaches)
}