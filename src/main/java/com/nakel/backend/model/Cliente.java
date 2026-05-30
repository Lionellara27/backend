package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
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

    // Campos obligatorios previstos para ARCA (Fase 2)
    @Column(nullable = false)
    private String cuit;

    @Column(nullable = false)
    private String condicionIva; // Ej: "Consumidor Final", "Monotributista"

    //Acá está la logica de la "Cuenta Corriente" o Fiado
    // Si es 0.00, está al día. Si es mayor, nos debe plata.
    @Column(nullable = false)
    private BigDecimal saldoCuentaCorriente = BigDecimal.ZERO;
}