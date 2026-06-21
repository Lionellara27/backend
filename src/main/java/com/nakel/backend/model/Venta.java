package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private BigDecimal total;

    // El campo que separa el mostrador de la AFIP (¡Lo dejé, es excelente!)
    @Column(nullable = false)
    private Boolean esFiscal;

    // 🔥 ¡Acá se guarda si la clienta tildó "Para Regalo"!
    @Column(nullable = false)
    private Boolean esTicketCambio = false;

    // Relación: Muchas ventas pueden pertenecer a un solo Cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // 👇 LA MAGIA PARA EL PAGO MIXTO Y EL CARRITO 👇

    // Una venta tiene muchos renglones (las carteras que compró)
    @ToString.Exclude
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles;

    // Una venta tiene muchos pagos (Ej: 20k Efectivo + 40k Tarjeta)
    @ToString.Exclude
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pago> pagos;
}