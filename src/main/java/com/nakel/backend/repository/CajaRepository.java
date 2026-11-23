package com.nakel.backend.repository;

import com.nakel.backend.model.CajaDiaria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CajaRepository extends JpaRepository<CajaDiaria, Long> {
    // Busca la caja abierta activa si existe
    Optional<CajaDiaria> findFirstByEstadoOrderByFechaAperturaDesc(String estado);

    // Trae todas las cajas ordenadas por fecha reciente (para el historial)
    List<CajaDiaria> findAllByOrderByFechaAperturaDesc();

    Optional<CajaDiaria> findFirstByOrderByFechaAperturaDesc();

    // Cajas de un período (un mes o un año), paginadas y con el orden que pida la pantalla
    Page<CajaDiaria> findByFechaBetween(LocalDate desde, LocalDate hasta, Pageable pageable);

    // Total de ingresos de todas las cajas, o solo de las de un período
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.totalVentas), 0) FROM CajaDiaria c")
    java.math.BigDecimal sumarIngresosDeTodas();

    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(c.totalVentas), 0) FROM CajaDiaria c WHERE c.fecha BETWEEN :desde AND :hasta")
    java.math.BigDecimal sumarIngresosEntre(@org.springframework.data.repository.query.Param("desde") LocalDate desde,
                                            @org.springframework.data.repository.query.Param("hasta") LocalDate hasta);
}
