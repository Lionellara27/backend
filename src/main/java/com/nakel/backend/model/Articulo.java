package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data // Lombok: crea getters, setters y constructores automáticamente por detrás
@Entity // Le avisa a Hibernate que esta clase es una tabla
@Table(name = "articulos") // Nombre en plural para la tabla en la base de datos
public class Articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Llave primaria autoincremental

    @Column(unique = true, nullable = false)
    private String codigo; // El de la pistolita láser o generado con el SKU de la categoría

    @Column(nullable = false)
    private String nombre; // Ej: "Billetera", "Mate"

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stockActual;

    // Dato clave previsto para el Módulo Fiscal (Fase 2)
    @Column(nullable = false)
    private Double alicuotaIva;

    // 🔥 La etiqueta mágica: "REVENTA" o "PRODUCCION_PROPIA"
    @Column(nullable = false)
    private String origen;

    // 🏷️ Filtro visual: De qué está hecho (Ej: "Cuero Liso")
    @ManyToOne
    @JoinColumn(name = "material_id")
    private Material material;

    // 🏷️ Magia del SKU: A qué familia pertenece (Ej: "Billeteras" -> 1111)
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}