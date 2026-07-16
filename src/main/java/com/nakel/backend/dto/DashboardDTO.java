package com.nakel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private double ventasHoy;
    private int cantidadVentasHoy;
    private double ventasSemana;
    private int cantidadVentasSemana;
    private int productosActivos;
    private int stockCritico;

}