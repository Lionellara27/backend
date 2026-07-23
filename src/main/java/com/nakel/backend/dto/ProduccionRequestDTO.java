package com.nakel.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProduccionRequestDTO {

    // --- Datos del Artículo Nuevo ---
    private String nombre;
    private String codigo;      // Ej: 11110004
    private Long idCategoria;   // El ID de la categoría elegida (Ej: Mates)
    private Long idMaterial;    // El ID del material ganador/destacado
    private String origen;      // "Produccion Propia"
    private BigDecimal costo;
    private BigDecimal precioVenta;
    private int stock;          // Cantidad a fabricar (Ej: 1)

    // --- Datos de los Insumos Usados ---
    private List<ItemRecetaDTO> insumosUsados;

    @Data
    public static class ItemRecetaDTO {
        private Long idInsumo;
        private BigDecimal cantidadUsada; // cm2, unidades u horas usadas por CADA artículo
    }
}