package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "proveedores")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String razonSocial;

    private String nombreContacto;
    private String rubro;
    private String cuit;
    private String telefono;
    private String email;

    // 🔥 SOLO QUEDA ESTE
    @Column(nullable = true)
    private BigDecimal saldo = BigDecimal.ZERO;
}