package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "vales")
public class Vale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    @Column(nullable = false)
    private LocalDateTime fechaVencimiento;

    // Estados: ACTIVO, USADO, VENCIDO
    @Column(nullable = false)
    private String estado;

    // 🔥 Agregamos el ID del cliente (null si es Consumidor Final)
    @Column(nullable = true)
    private Long idCliente;

    @PrePersist
    public void prePersist() {
        this.fechaEmision = LocalDateTime.now();
        // 🔥 A pedido del PO: 1 año exacto de validez. Pasado el año, se limpia el saldo positivo.
        this.fechaVencimiento = this.fechaEmision.plusYears(1);
        this.estado = "ACTIVO";
    }
}