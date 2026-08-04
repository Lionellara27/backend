package com.nakel.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "cambios")
public class Cambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "venta_original_id", nullable = false)
    @JsonIgnore
    private Venta ventaOriginal;

    @Column(nullable = false)
    private LocalDateTime fechaCambio;

    private Double diferenciaCobrada;
    private String metodoPago;
    private String codigoValeGenerado;

    @Column(length = 1000)
    private String resumenArticulos;

    // 🔥 LA NUEVA LISTA QUE CONTIENE LOS ÍTEMS DEL CAMBIO (DEVUELTOS Y NUEVOS)
    @OneToMany(mappedBy = "cambio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCambio> items = new ArrayList<>();

    // 🔥 MÉTODOS HELPERS QUE EL SERVICIO NECESITA PARA FILTRAR
    public List<ItemCambio> getItemsDevueltos() {
        if (items == null) return new ArrayList<>();
        return items.stream().filter(i -> "DEVUELTO".equals(i.getTipo())).toList();
    }

    public List<ItemCambio> getItemsNuevos() {
        if (items == null) return new ArrayList<>();
        return items.stream().filter(i -> "NUEVO".equals(i.getTipo())).toList();
    }

    @PrePersist
    public void prePersist() {
        this.fechaCambio = LocalDateTime.now();
    }
}