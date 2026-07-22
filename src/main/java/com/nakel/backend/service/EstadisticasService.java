package com.nakel.backend.service;

import com.nakel.backend.dto.DashboardDTO;
import com.nakel.backend.repository.ArticuloRepository;
import com.nakel.backend.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class EstadisticasService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ArticuloRepository articuloRepository;

    public DashboardDTO obtenerDatosDashboard() {
        // 1. Calculamos los rangos de tiempo
        LocalDateTime inicioHoy = LocalDateTime.now().with(LocalTime.MIN); // Hoy a las 00:00:00
        LocalDateTime finHoy = LocalDateTime.now().with(LocalTime.MAX);    // Hoy a las 23:59:59

        LocalDateTime inicioSemana = LocalDateTime.now().minusDays(7).with(LocalTime.MIN); // Hace 7 días

        // 2. Buscamos los datos de Ventas
        BigDecimal totalHoy = ventaRepository.sumarVentasEntreFechas(inicioHoy, finHoy);
        Integer cantHoy = ventaRepository.contarVentasEntreFechas(inicioHoy, finHoy);

        BigDecimal totalSemana = ventaRepository.sumarVentasEntreFechas(inicioSemana, finHoy);
        Integer cantSemana = ventaRepository.contarVentasEntreFechas(inicioSemana, finHoy);

        // 3. Buscamos los datos de Inventario
        Integer prodActivos = articuloRepository.contarProductosTotales();
        Integer stockCritico = articuloRepository.contarStockCritico();

        // 4. Armamos el DTO y lo devolvemos
        return new DashboardDTO(
                totalHoy.doubleValue(),
                cantHoy,
                totalSemana.doubleValue(),
                cantSemana,
                prodActivos,
                stockCritico
        );
    }
}