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

    // ==========================================
    // 📦 PARA INSUMOS POR UNIDAD (Cierres, Avíos)
    // ==========================================
    private Integer cantidadLote;   // Ej: Vinieron 100 remaches
    private Integer cantidadActual; // Ej: Me quedan 85 remaches

    // ==========================================
    // 📏 PARA INSUMOS POR SUPERFICIE (Telas, Cueros)
    // ==========================================
    private Integer anchoLoteCm;   // Ej: 100 cm (Medida original comprada)
    private Integer largoLoteCm;   // Ej: 100 cm (Medida original comprada)
    private Integer areaActualCm2; // Ej: Arranca en 10.000 cm², si uso 400cm² baja a 9.600 cm²

    // ==========================================
    // 🧮 MOTORES DE CÁLCULO DE COSTOS (Usan el LOTE)
    // ==========================================
    public BigDecimal getCostoPorCm2() {
        // 🔥 Blindaje: verificamos que no sean nulos Y que sean mayores a cero
        if (anchoLoteCm != null && largoLoteCm != null && anchoLoteCm > 0 && largoLoteCm > 0) {
            BigDecimal areaTotal = new BigDecimal(anchoLoteCm * largoLoteCm);
            // Divide el costo de toda la plancha por el área LOTE para saber el costo de 1cm²
            return costoTotal.divide(areaTotal, 4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getCostoPorUnidad() {
        if (cantidadLote != null && cantidadLote > 0) {
            // Divide el costo del paquete por la cantidad LOTE que trae
            return costoTotal.divide(new BigDecimal(cantidadLote), 4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // 🔥 AGREGÁ ESTO EN TU CLASE INSUMO (Backend)
    public void inicializarStock() {
        if (this.categoria != null) {
            String tipo = this.categoria.getTipoMedicion();

            if ("UNIDAD".equals(tipo)) {
                this.cantidadActual = this.cantidadLote;
            }
            else if ("SUPERFICIE".equals(tipo)) {
                if (this.anchoLoteCm != null && this.largoLoteCm != null) {
                    this.areaActualCm2 = this.anchoLoteCm * this.largoLoteCm;
                }
            }
        }
    }
}