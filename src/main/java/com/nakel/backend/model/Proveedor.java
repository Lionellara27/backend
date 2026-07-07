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
    private String razonSocial; // Ej: "Cueros San Martín S.A."

    private String nombreContacto; // 👈 NUEVO: Para matchear tu tabla del front

    private String rubro; // 👈 NUEVO: Ej: "Cueros", "Herrajes", etc.

    private String cuit;

    private String telefono;

    private String email;

    // 💰 La deuda nuestra con ellos (Ej: Pagos a 30 días)
    @Column(nullable = false)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;
}