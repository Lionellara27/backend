package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.math.RoundingMode;

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

    // En tu Insumo.java
    public BigDecimal getCostoPorCm2() {
        // 🔥 Blindaje: verificamos que no sean nulos Y que sean mayores a cero
        if (anchoCm != null && largoCm != null && anchoCm > 0 && largoCm > 0) {
            BigDecimal areaTotal = new BigDecimal(anchoCm * largoCm);
            return costoTotal.divide(areaTotal, 4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getCostoPorUnidad() {
        if (cantidad != null && cantidad > 0) {
            // Divide el costo del paquete por la cantidad de remaches que trae
            return costoTotal.divide(new BigDecimal(cantidad), 4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}