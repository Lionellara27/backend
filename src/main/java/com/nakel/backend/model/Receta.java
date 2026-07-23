package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "recetas")
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A qué artículo pertenece esta receta
    @OneToOne
    @JoinColumn(name = "articulo_id", nullable = false)
    private Articulo articulo;

    // La lista de insumos que lleva adentro
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "receta_id")
    private List<ItemReceta> items = new ArrayList<>();

    private BigDecimal costoTotalProduccion = BigDecimal.ZERO;
}