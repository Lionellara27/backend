package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "categorias_insumo")
public class CategoriaInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nombre; // Ej: "Cueros y Telas", "Herrajes", "Mano de Obra"

    // 🔥 Acá vive la regla para que el frontend sepa qué cajas de texto mostrar
    @Column(nullable = false)
    private String tipoMedicion; // Ej: "SUPERFICIE", "UNIDAD", "TIEMPO"
}