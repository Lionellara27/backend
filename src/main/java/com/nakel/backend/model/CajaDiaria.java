package com.nakel.backend.model;

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
}