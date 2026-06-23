package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

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
}