package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "categorias")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nombre; // Ej: "Billeteras", "Lámparas"

    // 🔥 Acá vive la regla del SKU para todas las billeteras
    @Column(nullable = false)
    private String prefijoSku; // Ej: "1111", "2222"

    // 📏 NUEVO: Para saber cómo calcularlo en la receta (UNIDAD, SUPERFICIE, etc.)
    @Column(nullable = true)
    private String tipoMedicion;
}