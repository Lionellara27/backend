package com.nakel.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "configuracion_global")
public class ConfiguracionGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El parámetro que la dueña puede cambiar (Ej: 10000.00)
    @Column(nullable = false)
    private BigDecimal valorHoraTrabajo;
}