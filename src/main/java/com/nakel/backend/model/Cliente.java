package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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

    @Column(unique = true)
    private String cuit;

    @Column(nullable = false)
    private String condicionIva;

    // 🔥 ACÁ ESTÁ EL CAMBIO: Borramos saldoCuentaCorriente y metemos los dos nuevos
    @Column(nullable = false)
    private BigDecimal saldoAFavor = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Venta> ventas = new ArrayList<>();
}