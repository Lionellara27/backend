package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "cajas_diarias")
@NoArgsConstructor
public class CajaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Fecha del día de la caja
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha = LocalDate.now();

    // Fecha y hora en que se abrió la caja
    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    // Fecha y hora en que se cerró la caja
    private LocalDateTime fechaCierre;

    // Usuario que abrió la caja
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_apertura_id", nullable = false)
    private Usuario usuarioApertura;

    // Usuario que la cerró (puede ser null mientras esté abierta)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_cierre_id")
    private Usuario usuarioCierre;

    // Dinero con el que inició la jornada
    @Column(nullable = false)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    // Ventas por medio de pago
    @Column(nullable = false)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalMercadoPago = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalTransferencias = BigDecimal.ZERO;

    // 🔥 NUEVOS CAMPOS: Tarjetas agregadas para Pago Mixto y Arqueo
    @Column(name = "total_tarjeta_debito", nullable = false)
    private BigDecimal totalTarjetaDebito = BigDecimal.ZERO;

    @Column(name = "total_tarjeta_credito", nullable = false)
    private BigDecimal totalTarjetaCredito = BigDecimal.ZERO;

    // Total vendido
    @Column(nullable = false)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    // Total retirado de la caja (pagos a proveedores, gastos, etc.)
    @Column(nullable = false)
    private BigDecimal totalEgresos = BigDecimal.ZERO;

    // Saldo final de la caja
    @Column(nullable = false)
    private BigDecimal saldoFinal = BigDecimal.ZERO;

    // Cantidad de ventas realizadas
    @Column(nullable = false)
    private Integer cantidadVentas = 0;

    // ABIERTA / CERRADA
    @Column(nullable = false)
    private String estado = "ABIERTA";

    // Observaciones del día
    @Column(length = 1000)
    private String observaciones;

    @OneToMany(mappedBy = "caja", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovimientoCaja> movimientos = new ArrayList<>();
}

/*package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime; // 🔥 Cambiamos a LocalDateTime para guardar la hora exacta

@Data
@Entity
@Table(name = "cajas_diarias")
@NoArgsConstructor // Obligatorio para Hibernate
public class CajaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardamos fecha y hora exacta de apertura (quitamos el unique para permitir varios turnos en un día)
    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    // Se llena recién cuando toca el botón "Cerrar Caja"
    private LocalDateTime fechaCierre;

    @Column(nullable = false)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    // 🔥 DESGLOSE DE RECAUDACIÓN (Para que no se vuelva loca contando)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;
    private BigDecimal totalMercadoPago = BigDecimal.ZERO;
    private BigDecimal totalTransferencias = BigDecimal.ZERO;

    // La suma de todo lo que vendió
    private BigDecimal totalVentas = BigDecimal.ZERO;

    // El saldo total final (Inicial + Ventas)
    private BigDecimal saldoFinal;

    // 📊 Para las estadísticas (Ej: "Hoy hiciste 15 ventas")
    private Integer cantidadVentas = 0;

    @Column(nullable = false)
    private String estado = "ABIERTA"; // "ABIERTA" o "CERRADA"
}*/