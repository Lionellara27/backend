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

    // 🔥 NUEVOS CAMPOS (Reemplazan al saldo único)
    @Column(name = "saldo_favor")
    private BigDecimal saldoFavor = BigDecimal.ZERO;

    @Column(name = "saldo_contra")
    private BigDecimal saldoContra = BigDecimal.ZERO;

    @Column(name = "comentarios", columnDefinition = "TEXT")
    private String comentarios;
}