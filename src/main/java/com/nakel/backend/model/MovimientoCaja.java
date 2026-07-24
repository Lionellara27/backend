package com.nakel.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore; // 🔥 1. Importante importar esto
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "movimientos_caja")
@NoArgsConstructor
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Caja a la que pertenece el movimiento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id", nullable = false)
    @JsonIgnore // 🔥 2. ESTO CORTA EL BUCLE INFINITO EN EL JSON
    private CajaDiaria caja;

    // Usuario que realizó el movimiento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Fecha y hora exacta del movimiento
    @Column(nullable = false)
    private LocalDateTime fechaHora;

    // VENTA, EGRESO, DEVOLUCION, APERTURA, CIERRE, AJUSTE...
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimientoCaja tipo;

    // Ej: "Venta", "Compra proveedor", "Retiro", etc.
    @Column(nullable = false, length = 100)
    private String concepto;

    // Texto libre
    @Column(length = 500)
    private String descripcion;

    // EFECTIVO, MERCADOPAGO, TRANSFERENCIA
    @Column(length = 50)
    private String medioPago;

    // Siempre positivo. El tipo indica si suma o resta.
    @Column(nullable = false)
    private BigDecimal monto;
}