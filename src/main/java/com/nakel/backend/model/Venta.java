package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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

    @Column(nullable = false)
    private Boolean esFiscal;

    @Column(nullable = false)
    private Boolean esTicketCambio = false;

    // 🔥 BLINDAJE 1: Lazy para que no reviente la base de datos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // 🔥 BLINDAJE 2: new ArrayList<>() para evitar NullPointerExceptions
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    // 🔥 BLINDAJE 3: Igual acá
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pago> pagos = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.fechaHora = LocalDateTime.now();
    }
}