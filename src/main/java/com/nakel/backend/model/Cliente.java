package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter // Reemplazamos @Data
@Setter // Reemplazamos @Data
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String telefono;

    private String email;

    // Le quitamos el nullable = false por si entra alguien a comprar rápido y no da su CUIT/DNI
    @Column(unique = true)
    private String cuit;

    @Column(nullable = false)
    private String condicionIva;

    @Column(nullable = false)
    private BigDecimal saldoCuentaCorriente = BigDecimal.ZERO;

    // 🔥 LA CONEXIÓN: Un cliente tiene muchas ventas (El historial de sus compras)
    // Usamos JsonIgnore para que cuando el Frontend pida la lista de clientes,
    // no se traiga toda la base de datos de ventas pegada y cuelgue el sistema.
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Venta> ventas = new ArrayList<>();
}